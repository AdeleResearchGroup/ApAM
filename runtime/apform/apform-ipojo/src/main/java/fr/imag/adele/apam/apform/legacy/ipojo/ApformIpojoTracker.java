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
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.impl.ApamComponentFactory;
import fr.imag.adele.apam.apform.impl.ApamInstanceManager;
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

public class ApformIpojoTracker implements ServiceTrackerCustomizer, Apform2Apam.Platform {

    /**
     * The reference to the APAM platform
     */
	@Requires(id="apam")
    private Apam                apam;

    @Requires(optional=false, proxy=false) 
    private QueueService queueService;

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
    }
    

    @Bind(id="apam")
    private void bindToApam() {
    	Apform2Apam.setPlatform(this);
    }

    @Unbind(id="apam")
    private void unbindFromApam() {
    	Apform2Apam.setPlatform(null);
    }

    /**
     * Starting.
     */
    @Validate
    public void start() {

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
        instancesServiceTracker.close();
    }

    /**
     * Callback to handle factory binding
     */
    @Bind(id="factories",aggregate=true,optional=true)
    public void factoryBound(Factory factory) {

        if (factory instanceof ApamComponentFactory)
            return;

        if (factory instanceof IPojoFactory) {
            ApformImplementation implementation = new ApformIPojoImplementation((IPojoFactory) factory);
            Apform2Apam.newImplementation(implementation);
        }
    }

    /**
     * Callback to handle factory unbinding
     */
    @Unbind(id="factories",aggregate=true,optional=true)
    public void factoryUnbound(Factory factory) {
        if (factory instanceof ApamComponentFactory)
            return;

        if (factory instanceof IPojoFactory) {
        	((ComponentBrokerImpl)CST.componentBroker).disappearedComponent(factory.getName()) ;
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
         * Ignore instances of private factories, as no implementation is
         * available to register in APAM
         * 
         * TODO should we register iPojo private factories in APAM when their
         * instances are discovered? how to know when to unregister them?
         */
        try {
            String factoryFilter = "(factory.name=" + ipojoInstance.getFactory().getName() + ")";
            if (context.getServiceReferences(Factory.class.getName(),factoryFilter) == null)
                return false;
        } catch (InvalidSyntaxException ignored) {
        }

        /*
         * In the case of APAM instances registered in the registry (hybrid
         * components), registration in APAM has already be done by the Instance
         * Manager
         */
        if (ipojoInstance instanceof ApamInstanceManager)
            return false;

        /*
         * For legacy instances, register the corresponding declaration in APAM
         */
        ApformIpojoInstance apformInstance = new ApformIpojoInstance(ipojoInstance, reference);
        Apform2Apam.newInstance(apformInstance);

        return true;
    }

    /**
     * Callback to handle instance unbinding
     */
    public void instanceUnbound(ComponentInstance ipojoInstance) {

        /*
         * In the case of APAM instances registered in the registry (hybrid
         * components), registration in APAM has already be done by the Instance
         * Manager
         */
        if (ipojoInstance instanceof ApamInstanceManager)
            return;

        /*
         * For ipojo instances, unregister the corresponding declaration in
         * APAM
         */
        ((ComponentBrokerImpl)CST.componentBroker).disappearedComponent(ipojoInstance.getInstanceName()) ;
    }

    /**
     * Whether there is pending declarations currently being deployed in the iPOJO platform
     */
	@Override
	public boolean hasPendingDeclarations() {
		return queueService.getWaiters() > 0;
	}

	/**
	 * Waits for all pending declarations to be processed by the iPOJO platform
	 */
	@Override
	public void waitForDeclarations() {
		
		QueueWaiter waiter = new QueueWaiter(queueService);
		waiter.block();
		waiter.dispose();
	}

	private static class QueueWaiter implements QueueListener {

		private QueueService queueService;
		private String threadName;
		
		public QueueWaiter(QueueService queueService) {
			this.queueService = queueService;
			this.queueService.addQueueListener(this);
			
			this.threadName = Thread.currentThread().getName();
		}
		
		public void dispose() {
			this.queueService.removeQueueListener(this);
		}
		
		public void block() {
			synchronized (this) {
				
				try {
					while (queueService.getWaiters() > 0)
						this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}
		
		private synchronized void checkUnblock() {
			this.notifyAll();
		}
		
		@Override
		public void enlisted(JobInfo info) {
			checkUnblock();
		}

		@Override
		public void started(JobInfo info) {
			checkUnblock();
		}

		@Override
		public void executed(JobInfo info, Object result) {
			System.err.println(threadName+" JOB EXECUTED "+info.getDescription());
			checkUnblock();
		}

		@Override
		public void failed(JobInfo info, Throwable throwable) {
			checkUnblock();
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

        if (!(service instanceof Pojo))
            return;

        ComponentInstance ipojoInstance = ((Pojo) service).getComponentInstance();
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
        Instance inst = CST.componentBroker.getInst(ipojoInstance.getInstanceName());
        if (inst == null)
            return;

        /*
         * Otherwise propagate property changes to Apam
         */
        for (String key : reference.getPropertyKeys()) {
            if (!Apform2Apam.isPlatformPrivateProperty(key)) {
                String value = reference.getProperty(key).toString();
                if (value != inst.getProperty(key))
                    inst.setProperty(key, value);
            }
        }
    }

}
