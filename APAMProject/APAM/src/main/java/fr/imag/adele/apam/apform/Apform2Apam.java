package fr.imag.adele.apam.apform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;

public class Apform2Apam {
	
    private static Logger logger = LoggerFactory.getLogger(Apform2Apam.class);
  
    private static List<String> platformPrivateProperties = Arrays.asList(new String[] { Constants.SERVICE_ID, Constants.OBJECTCLASS, 
    																			 "factory.name", "instance.name" });

    /**
     * Check if a property is a platform private information that must not be propagated to APAM
     */
    public static boolean isPlatformPrivateProperty(String key) {
        return platformPrivateProperties.contains(key);
    }
    
    /**
     * The components that clients are waiting for deployment to complete
     */
    private static Set<String> expectedComponents = new HashSet<String>();
    
    /**
     * The event executor. We use a pool of a threads to handle notification to APAM of underlying platform
     * events, without blocking the platform thread.
     */
    static private final Executor executor      = Executors.newCachedThreadPool();
    
    
    static private final ThreadLocal<Request> 	current = new ThreadLocal<Request>();
    static private final List<Request> 			pending = new ArrayList<Request>();
    
    /**
     * A description of a waiting request in Apam
     * 
     * @author vega
     */
    public static class Request {

    	private final String 	description;
    	private boolean			isProcessing;
    	private String 			requiredComponent;
    	
    	Request(String description) {
    		this.description	= description;
    		this.isProcessing	= false;
		}
    	
    	/**
    	 * The request description
    	 */
    	public String getDescription() {
			return description;
		}
    	
    	/**
    	 * Mark this request as started
    	 */
    	protected void started() {
    		isProcessing = true;
    		current.set(this);
    	}
    	
    	/**
    	 * Mark this request as finished
    	 */
    	protected void finished() {
    		isProcessing = false;
    		current.remove();
    	}
    	
    	/**
    	 * Whether this request is being processing
    	 */
    	public boolean isProcessing() {
    		return isProcessing;
    	}
    	
    	/**
    	 * Mark this request as pending for a component
    	 */
    	private void pending(String requiredComponent) {
    		this.requiredComponent = requiredComponent;
    		pending.add(this);
    	}

    	/**
    	 * Mark this request as resumed after the requested component is found
    	 */
    	private void resumed() {
    		this.requiredComponent = null;
    		pending.remove(this);
    	}
    	
    	/**
    	 * Whether this request is pending
    	 */
    	public boolean isPending() {
    		return pending.contains(this);
    	}
    	
    	/**
    	 * The required component
    	 */
    	public String getRequiredComponent() {
    		return requiredComponent;
    	}

    }

    /**
     * The list of pending request
     */
    public static List<Request> getPending() {
    	return Collections.unmodifiableList(pending);
    }

    /**
     * The request executing in the context of the current thread.
     * 
     */
    public static Request getCurrent() {
    	Request currentRequest = current.get();
    	if (currentRequest == null) {
    		currentRequest = new Request("Thread "+Thread.currentThread().getName());
    		currentRequest.started();
    	}
    	return currentRequest;
    }
    
    /**
     * Wait for a future component to be deployed
     */
    public static void waitForComponent(String componentName) {

        synchronized (Apform2Apam.expectedComponents) {
        	
        	Request current = getCurrent();
        	
            Apform2Apam.expectedComponents.add(componentName);
            try {
                while (Apform2Apam.expectedComponents.contains(componentName)) {
                	current.pending(componentName);
                    Apform2Apam.expectedComponents.wait();
                    current.resumed();
                }
            } catch (InterruptedException interrupted) {
                interrupted.printStackTrace();
            }
            return;
        }
    }
    
    /**
     * A request from apform to add a component to APAM, this is executed asynchronously and may block waiting
     * for another components.
     * 
     * @author vega
     * 
     */
    
    private abstract static class ComponentAppearenceRequest extends Request implements Runnable {
 
    	private final ApformComponent component;

    	protected ComponentAppearenceRequest(ApformComponent component) {
        	super("Adding component "+component.getDeclaration().getName());
    		this.component = component;
    	}
    	
    	/**
    	 * The component that needs to be reified in APAM
    	 * @return
    	 */
        public ApformComponent getComponent() {
        	return component;
        }
        
        @Override
        public void run() {
            try {
            	started();
            	Component apamReification = reify();
                if (apamReification != null) {
                    notifyDeployment(apamReification);
                }
            } catch (Exception unhandledException) {
                logger.error("Error handling Apform event :",unhandledException);
            } finally {
            	finished();
            }

        }

        
        /**
         * The processing method
         */
        protected abstract Component reify();
        
        /**
         * Notify any threads waiting for the deployment of a component
         */
        private void notifyDeployment(Component component) {
        	
            synchronized (Apform2Apam.expectedComponents) {
            	/*
            	 * If it is expected wake up all threads blocked in waitForComponent
            	 */
                if (Apform2Apam.expectedComponents.contains(component.getName())) { 
                    Apform2Apam.expectedComponents.remove(component.getName());
                    Apform2Apam.expectedComponents.notifyAll();
                }
            }
        	
        }
        

    }
    
    private static class InstanceAppearenceProcessing extends ComponentAppearenceRequest {

    	
        public InstanceAppearenceProcessing(ApformInstance instance) {
        	super(instance);
        }

        @Override
        public ApformInstance getComponent() {
        	return (ApformInstance) super.getComponent();
        }
        
        @Override
        public Component reify() {
        	
        	/*
        	 * Verify implementation is currently installed. 
        	 * If not installed wait for installation.
        	 */
        	String implementationName = getComponent().getDeclaration().getImplementation().getName();
        	CST.componentBroker.getWaitComponent(implementationName);
        	
        	/*
        	 * Add to APAM
        	 */
        	return CST.componentBroker.addInst(null,getComponent());
        }

    }

    /**
     * Task to handle implementation deployment
     * 
     * @author vega
     * 
     */
    private static class ImplementationDeploymentProcessing extends ComponentAppearenceRequest {

        public ImplementationDeploymentProcessing(ApformImplementation implementation) {
        	super(implementation);
        }

        @Override
        public ApformImplementation getComponent() {
        	return (ApformImplementation) super.getComponent();
        }
        
        @Override
        public Component reify() {
        	/*
        	 * Verify specification is currently installed. If not installed wait for
        	 * installation
        	 */
        	SpecificationReference specification = getComponent().getDeclaration().getSpecification();
        	if (specification != null) {
        		CST.componentBroker.getWaitComponent(specification.getName());
        	}
        	
        	/*
        	 * Add to APAM
        	 */
            return CST.componentBroker.addImpl(null,getComponent());
        }

    }

    /**
     * Task to handle specification deployment
     * 
     * @author vega
     * 
     */
    private static class SpecificationDeploymentProcessing extends ComponentAppearenceRequest {

        public SpecificationDeploymentProcessing(ApformSpecification specification) {
        	super(specification);
        }

        @Override
        public ApformSpecification getComponent() {
        	return (ApformSpecification) super.getComponent();
        }
        
        @Override
        public Component reify() {
            return CST.componentBroker.addSpec(getComponent());
            
        }
    }

    /**
     * A new instance, represented by object "client" just appeared in the platform.
     */
    public static void newInstance(ApformInstance client) {
        Apform2Apam.executor.execute(new InstanceAppearenceProcessing(client));
    }

    /**
     * A new implementation, represented by object "client" just appeared in the platform.
     * 
     * @param implemName : the symbolic name.
     * @param client
     */
    public static void newImplementation(ApformImplementation client) {
        Apform2Apam.executor.execute(new ImplementationDeploymentProcessing(client));
    }

    /**
     * A new specification, represented by object "client" just appeared in the platform.
     * 
     * @param specName
     * @param client
     */
    public static void newSpecification(ApformSpecification client) {
        Apform2Apam.executor.execute(new SpecificationDeploymentProcessing(client));
    }

    /**
     * The instance called "instance name" just disappeared from the platform.
     * 
     * @param instanceName
     */
    public static void vanishInstance(String instanceName) {
        Instance inst = CST.componentBroker.getInst(instanceName);
        if (inst == null) {
          // previous remove of the factory removed instances
          // logger.warn("Unable to remove instance '{}' : non-existent instance", instanceName);
            return;
        }
        ((ComponentBrokerImpl)CST.componentBroker).removeInst(inst);
        
    }

    /**
     * * The implementation called "implementation name" just disappeared from the platform.
     * 
     * @param implementationName
     */
    public static void vanishImplementation(String implementationName) {
        Implementation impl = CST.componentBroker.getImpl(implementationName);
        if (impl == null) {
        	logger.warn("Vanish implementation does not exists: " + implementationName);
            return;
        }

        ((ComponentBrokerImpl)CST.componentBroker).removeImpl(impl);
    }

    /**
     * 
     * @param specificationName
     */
    public static void vanishSpecification(String specificationName) {
        Specification spec = CST.componentBroker.getSpec(specificationName);
        if (spec == null) {
        	logger.warn("Vanish specification does not exists: " + specificationName);
            return;
        }
    	
        ((ComponentBrokerImpl)CST.componentBroker).removeSpec(spec);
    }

}
