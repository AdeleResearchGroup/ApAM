/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.apform.legacy.ipojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.IPojoFactory;
import org.apache.felix.ipojo.Pojo;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.ipojo.extender.queue.JobInfo;
import org.apache.felix.ipojo.extender.queue.QueueListener;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.DynamicManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.Apform2Apam.PendingThread;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.impl.ApamComponentFactory;
import fr.imag.adele.apam.apform.impl.ApamInstanceManager;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.references.components.Versioned;
import fr.imag.adele.apam.declarations.references.resources.InterfaceReference;
import fr.imag.adele.apam.declarations.references.resources.MessageReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;

/**
 * This class tracks iPojo legacy implementations and instances and register
 * them in APAM
 * 
 * @author vega
 * 
 */
@org.apache.felix.ipojo.annotations.Component(name = "ApformIpojoTracker" , immediate=true)
@Instantiate(name = "ApformIpojoTracker-Instance")

public class ApformIpojoTracker implements DynamicManager, Apform2Apam.Platform, ServiceTrackerCustomizer,  QueueListener {

    /**
     * The reference to the APAM platform
     */
	@Requires(id="apam",proxy=false)
    private Apam                apam;

	/**
	 * The reference to the iPOJO queue service instances
	 */
    @Requires(id="iPOJO-queueServices", optional=false, proxy=false)
    private List<QueueService> queueServices;

    /**
     * The instances service tracker.
     */
    private ServiceTracker      instancesServiceTracker;

    /**
     * The bundle context associated with this tracker
     */
    private final BundleContext context;


    public ApformIpojoTracker(BundleContext context) {
        this.context = context;
        this.queueServices = new ArrayList<QueueService>();
    }
    

	@Override
	public String getName() {
		return "ApformIpojoTracker";
	}
    
    @Bind(id="apam")
    private void bindToApam() {
    	Apform2Apam.setPlatform(this);
    }

    @Unbind(id="apam")
    private void unbindFromApam() {
    	Apform2Apam.setPlatform(null);
    }

    @Bind(id="iPOJO-queueServices")
    private void bindToQueue(QueueService queueService) {
		queueService.addQueueListener(this);
    }

    @Unbind(id="iPOJO-queueServices")
    private void unbindToQueue(QueueService queueService) {
		queueService.removeQueueListener(this);
    }

    /**
     * Starting.
     */
    @Validate
    public void start() {
		ApamManagers.addDynamicManager(this);

        try {
            Filter filter = context.createFilter("(instance.name=*)");
            instancesServiceTracker = new ServiceTracker(context, filter, this);
            instancesServiceTracker.open();


        } catch (InvalidSyntaxException e) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Stopping.
     */
    @Invalidate
    public void stop() {
		ApamManagers.removeDynamicManager(this);
		instancesServiceTracker.close();
   }

	@Override
	public void addedComponent(Component component) {
		
		/*
		 * If a new specification is defined we need to try match existing factories in the registry  
		 */
		if (component instanceof Specification) {

			Specification specification = (Specification) component;
			if (!isMatchable(specification))
				return;


			/*
			 * Get the registered factories and bind all factories matching the specification
			 */
			ServiceReference[] factoryReferences = searchFactories(null);
			for (ServiceReference factoryReference : factoryReferences != null ? factoryReferences : new ServiceReference[0]) {
				
				Factory factory = (Factory) context.getService(factoryReference);
				
				if (factory != null && isEligible(factory) && matchingScore((IPojoFactory)factory, specification) > 0) {
					factoryBound((IPojoFactory)factory,specification);
				}
				
				context.ungetService(factoryReference);
			}
			
		}
		
		/*
		 * If an iPOJO implementation is defined, we create all delayed tracked instances
		 */
		if ((component instanceof Implementation) && (component.getProperty("ipojo.factory") != null)) {
			
			ServiceReference[] trackedReferences = instancesServiceTracker.getServiceReferences();
			for (ServiceReference serviceReference : trackedReferences != null ? trackedReferences : new ServiceReference[0]) {

				Object factoryName = serviceReference.getProperty("factory.name");
				if ( factoryName != null && component.getName().equals(factoryName)) {

					Pojo pojo = (Pojo) instancesServiceTracker.getService(serviceReference);
					if (pojo != null) {
				        instanceBound(serviceReference,pojo.getComponentInstance());
					}
				}
			}
		}
		
	}
    
	/**
	 * Determines if the specification is a possible candidate to be matched to an iPOJO factory
	 */
	private static boolean isMatchable(Specification specification) {
		
		if (!specification.getRelations().isEmpty())
			return false;
		
		if (! specification.getDeclaration().getProvidedResources(MessageReference.class).isEmpty())
			return false;

		if (specification.getDeclaration().getProvidedResources(InterfaceReference.class).isEmpty())
			return false;
		
		return true;
	}
    
	
	/**
	 * Determines if the iPOJO factory is eligible to become an APAM implementation
	 */
	private static boolean isEligible(Factory factory) {
		return (factory instanceof IPojoFactory) && !(factory instanceof ApamComponentFactory);
	}

	/**
	 * Determines if the specified factory matches the specification.
	 * 
	 * Return a number that represents how well the factory matches the interface. 
	 * 
	 * The returned number is zero if the factory doesn't match the specification.
	 */
	private static int matchingScore(IPojoFactory factory, Specification specification) {

		if (!isMatchable(specification)) {
			return 0;
		}
		
    	Set<ResourceReference> providedResources = new HashSet<ResourceReference>();
    	
		for (String providedResource : factory.getComponentDescription().getprovidedServiceSpecification()) {
			providedResources.add(new InterfaceReference(providedResource));
		}

		Set<ResourceReference> matchedResources = new HashSet<ResourceReference>(specification.getProvidedResources());
		matchedResources.retainAll(providedResources);

		return matchedResources.size();
	}
	
    /**
     * Select the specification that best matches the iPOJO factory
     */
    private Specification getBestMatchingSpecification(IPojoFactory factory) {
    	
    	Set<ResourceReference> providedResources = new HashSet<ResourceReference>();
    	
		for (String providedResource : factory.getComponentDescription().getprovidedServiceSpecification()) {
			providedResources.add(new InterfaceReference(providedResource));
		}

		Specification best 	= null;
		int bestScore		= 0;
		
		for (Specification specification : CST.componentBroker.getSpecs()) {
			
			int score = matchingScore(factory, specification);
			if (score > bestScore) {
				best 		=  specification; 
				bestScore 	=  score; 
			} 
		}
		
		return best; 
    }
	
    /**
     * Callback to handle factory binding
     */
    @Bind(id="factories", aggregate=true, optional=true, proxy=false)
    public void factoryBound(Factory factory) {
    	if (isEligible(factory)) {
    		factoryBound((IPojoFactory)factory,getBestMatchingSpecification((IPojoFactory)factory));
        }
    }

    /**
     * Creates an APAM implementation corresponding to the given factory and specification
     */
    public void factoryBound(IPojoFactory factory, Specification specification) {
        if (specification != null &&  CST.componentBroker.getImpl(factory.getName()) == null) {
        	Versioned<SpecificationDeclaration> specificationReference = specification != null ? Versioned.any(specification.getApformSpec().getDeclaration().getReference()) : null;
        	ApformImplementation implementation = new ApformIPojoImplementation((IPojoFactory) factory, specificationReference);
            Apform2Apam.newImplementation(implementation);
        }
    }
    
    /**
     * Callback to handle factory unbinding
     */
    @Unbind(id="factories", aggregate=true, optional=true, proxy=false)
    public void factoryUnbound(Factory factory) {
        if (isEligible(factory) && CST.componentBroker.getImpl(factory.getName()) != null) {
        	((ComponentBrokerImpl)CST.componentBroker).disappearedComponent(factory.getName()) ;
        }
    }


    /**
     * Search OSGI for all registered iPOJO factories, optionally a filter for the name can
     * be specified
     */
    private ServiceReference[] searchFactories(String name) {
        try {
            String nameFilter = name != null ? "(factory.name=" + name + ")" : null;
            return context.getServiceReferences(Factory.class.getName(),nameFilter);
        } catch (InvalidSyntaxException ignored) {
        	return null;
        }
    }
    
    /**
     * Callback to handle instance binding
     */
    public boolean instanceBound(ServiceReference reference, ComponentInstance ipojoInstance) {
        /*
         * ignore handler instances
         */
        if (ipojoInstance.getFactory() instanceof HandlerFactory)
            return false;

       /*
        * In the case of APAM instances registered in the registry (hybrid components), registration
        * in APAM has already be done by the Instance Manager
        */
       if (ipojoInstance instanceof ApamInstanceManager)
           return false;

       /*
        * ignore already bound instances 
        */
       if (CST.componentBroker.getInst(ipojoInstance.getInstanceName()) != null)
    	   return false;
       
       /*
        * If the implementation is not yet in APAM, just delay registration, instances will be automatically
        * created later (see #addedComponent)
        */
       Implementation implementation = CST.componentBroker.getImpl(ipojoInstance.getFactory().getName());
       if (implementation == null)
    	   return true;
       
        /*
         * Register the corresponding declaration in APAM
         */
        ApformIpojoInstance apformInstance = new ApformIpojoInstance(ipojoInstance, reference);
        Apform2Apam.newInstance(apformInstance);

        return true;
    }
    
    /**
     * Callback to handle instance unbinding
     */
    public void instanceUnbound(ComponentInstance ipojoInstance) {
    	if (CST.componentBroker.getInst(ipojoInstance.getInstanceName()) != null) {
            ((ComponentBrokerImpl)CST.componentBroker).disappearedComponent(ipojoInstance.getInstanceName()) ;
    	}
    }

    @Override
    public Object addingService(ServiceReference reference) {

        /*
         * Ignore events while APAM is not available
         */
        if (apam == null)
            return null;

        /*
         * ignore services that are not iPojo
         */
        Object service = context.getService(reference);
        if ((service instanceof Pojo) && instanceBound(reference,((Pojo) service).getComponentInstance()))
            return service;

        /*
         * If service is not a recognized iPojo instance, don't track it
         */
        context.ungetService(reference);
        return null;
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {

        /*
         * This should never happen, but there seems to be some bug in the service tracker
         */
        if (!(service instanceof Pojo)) {
            return;
        }

        ComponentInstance ipojoInstance = ((Pojo) service).getComponentInstance();
        
        /*
         * Ignore hybrid APAM+iPOJO instances
         */
        if (service instanceof ApamInstanceManager) {
        	return;
        }        
        
        /*
         * unbound the instance
         */
        instanceUnbound(ipojoInstance);
        context.ungetService(reference);
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {

        if (!(service instanceof Pojo))
            return;

        ComponentInstance ipojoInstance = ((Pojo) service).getComponentInstance();

        /*
         * If the service is not reified in APAM, just ignore event
         */
        Instance instance = CST.componentBroker.getInst(ipojoInstance.getInstanceName());
        if (instance == null)
            return;

        /*
         * Otherwise propagate property changes to Apam, we only propagate properties that are defined 
         * in APAM and that are not inherited from the implementtaion
         */
        for (String key : reference.getPropertyKeys()) {
            if (instance.getImpl().getPropertyDefinition(key) != null && instance.getImpl().getProperty(key) == null) {
                String value = reference.getProperty(key).toString();
                if (value != instance.getProperty(key))
                    instance.setProperty(key, value);
            }
        }
    }
    
    /*
     *  Implementation of the ApformToApam platform API
     */
	private final static boolean DEBUG = false;


    /**
     * Whether there is pending declarations currently being deployed in the iPOJO platform
     */
	@Override
	public boolean hasPendingDeclarations() {
		return  getPendingJobs() > 0;
	}

	/**
	 * Determines if iPOJO is currently processing bundle declarations
	 */
	private int getPendingJobs() {
		int pending = 0;
		for (QueueService queueService : queueServices) {
			pending += queueService.getWaiters()+queueService.getCurrents(); 
		}
		
		return  pending;
	}
	
	/**
	 * Waits for all pending declarations to be processed by the iPOJO platform
	 */
	@Override
	public void waitForDeclarations() {
		synchronized (this) {
			try {
				
				WaitingThread thread = new WaitingThread();
				
				while (hasPendingDeclarations()) {
					if (DEBUG)
						System.err.println(Thread.currentThread()+" waiting for "+getPendingJobs()+" iPOJO jobs");
					
					thread.pending(getPendingJobs()+" iPOJO jobs");
					this.wait();
					thread.resumed();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * The list of threads waiting for a iPOJO completion if processing
	 */
	static private final List<WaitingThread> pending = new ArrayList<WaitingThread>();

	/**
	 * The list of threads waiting for a component in APAM
	 */
	@Override
	public List<? extends PendingThread> getPending() {
		return Collections.unmodifiableList(pending);
	}

	/**
	 * A description of a thread that needs to wait for iPOJO processing declarations 
	 * 
	 * @author vega
	 */
	private static class WaitingThread extends PendingThread {

		private String condition;
		
		public WaitingThread() {
			super("Thread " + Thread.currentThread().getName());
		}

		/**
		 * The condition this thread is waiting for
		 */
		public String getCondition() {
			return condition;
		}


		/**
		 * Mark this thread as pending for a codition
		 */
		protected void pending(String condition) {
			this.condition = condition;
			this.stack = getCurrentStack();
			pending.add(this);
		}

		/**
		 * Mark this request as resumed after the condition is satisfied
		 */
		protected void resumed() {
			this.condition = null;
			this.stack = null;

			pending.remove(this);
		}

		@Override
		public String toString() {
			return description;
		}
	}

	/**
	 * The stack of the thread currently executing inside this class, and that is going to wait for a platform condition
	 * 
	 */
	private static List<StackTraceElement> getCurrentStack() {

		List<StackTraceElement> stack = new ArrayList<StackTraceElement>(Arrays.asList(new Throwable().getStackTrace()));

		/*
		 * Remove ourselves from the top of the stack, to increase the readability of the stack trace
		 */
		Iterator<StackTraceElement> frames = stack.iterator();
		while (frames.hasNext()) {
			if (frames.next().getClassName().startsWith(ApformIpojoTracker.class.getName())) {
				frames.remove();
				continue;
			}

			break;
		}
		return stack;
	}

	
	
	private void jobStausChanged(String state, JobInfo info) {

		if (DEBUG)
			System.err.println(Thread.currentThread()+" job change event :"+state+" "+info.getDescription());

		synchronized (this) {
			this.notifyAll();
		}
	}
	
	@Override
	public void enlisted(JobInfo info) {
		jobStausChanged("enlisted",info);
	}

	@Override
	public void started(JobInfo info) {
		jobStausChanged("started",info);
	}

	@Override
	public void executed(JobInfo info, Object result) {
		jobStausChanged("executed",info);
	}

	@Override
	public void failed(JobInfo info, Throwable throwable) {
		jobStausChanged("failed",info);
	}
		
	@Override
	public void removedComponent(Component lostComponent) {
	}

	@Override
	public void addedLink(Link wire) {
	}

	@Override
	public void removedLink(Link wire) {
	}

}
