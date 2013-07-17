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
package fr.imag.adele.apam.apform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.declarations.ComponentReference;


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
    public static abstract class Request {

    	private final String 			description;
    	private boolean					isProcessing;
    	private String 					requiredComponent;
    	private List<StackTraceElement>	stack;
    	
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
    	protected void pending(String requiredComponent) {
    		this.requiredComponent	= requiredComponent;
    		this.stack				= getCurrentStack();			
    		pending.add(this);
    	}

    	/**
    	 * Mark this request as resumed after the requested component is found
    	 */
    	protected void resumed() {
    		this.requiredComponent 	= null;
    		this.stack				= null;
    		
    		pending.remove(this);
    	}
    	
    	/**
    	 * Whether this request is pending
    	 */
    	public boolean isPending() {
    		return pending.contains(this);
    	}
    	
    	/**
    	 * The stack of pending requests
    	 */
    	public List<StackTraceElement> getStack() {
    		return stack;
    	}
    	
    	/**
    	 * The required component
    	 */
    	public String getRequiredComponent() {
    		return requiredComponent;
    	}

    	@Override
    	public String toString() {
    		return description;
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
    		currentRequest = new WaitRequest();
    		currentRequest.started();
    	}
    	return currentRequest;
    }
    
    /**
     * The stack of the request executing in the context of the
     * current thread.
     * 
     */
    public static List<StackTraceElement> getCurrentStack() {
    	
    	List<StackTraceElement> 	stack 	= new ArrayList<StackTraceElement>(Arrays.asList(new Throwable().getStackTrace()));
    	
    	/*
    	 * Remove ourselves from the top of the stack, to increase the readability of the stack trace
    	 */
    	Iterator<StackTraceElement> frames	= stack.iterator();
    	while (frames.hasNext()) {
    		if (frames.next().getClassName().startsWith(Apform2Apam.class.getName()))
    			frames.remove();
    		
    		break;
    	}
    	return stack;
    }


    /**
     * Wait for a future component to be deployed
     */
    public static void waitForComponent(String componentName) {
           waitForComponent(componentName,0);
    }

    /**
     * Wait for a future component to be deployed
     */
    public static void waitForComponent(String componentName, long timeout) {

        synchronized (Apform2Apam.expectedComponents) {
        	
        	/*
        	 * Last verification before blocking. The expected component could have
        	 * been added to APAM between the moment we checked and this method
        	 * call.
        	 * 
        	 * NOTE notice that the check is inside the synchronized block to avoid
        	 * race conditions with appearing components.
        	 * 
        	 * TODO perhaps this code should be refactored into the broker, so that
        	 * validation and blocking can be done atomically
        	 */
        	if (CST.componentBroker.getComponent(componentName) != null)
        		return;
        	
        	Request current = getCurrent();
            Apform2Apam.expectedComponents.add(componentName);
           	current.pending(componentName);
           	
           	/*
           	 * long startWaiting = System.currentTimeMillis();
           	 */
            
           	try {
                 while (Apform2Apam.expectedComponents.contains(componentName)) {
                    Apform2Apam.expectedComponents.wait(timeout);

                    /*
                     * NOTE current implementation actually waits forever, even if it
                     * wakes up at the timeout expiration. However if we change this, most
                     * of the time this cause errors because findByName return a null
                     * component, and this is not systematically tested.
                     * 
                     * TODO Either remove timeout or check component after calling
                     * finfByName.
                     * 
                    long elapsed = System.currentTimeMillis() - startWaiting;
                    if (elapsed > timeout)
                    	return;
                    */
                 }
            } catch (InterruptedException interrupted) {
                interrupted.printStackTrace();
            }
            finally {
            	current.resumed();
            }
            return;
        }
    }
  
    /**
     * A request to wait for a component outside the context of an apform event. These are temporary requests
     * that are finished as soon as they are satisfied.
     * 
     * @author vega
     * 
     */
    private static class WaitRequest extends Request {

		public WaitRequest() {
			super("Thread "+Thread.currentThread().getName());
		}
	
		/**
		 * Automatically finish the request when resumed
		 */
		@Override
		protected void resumed() {
			super.resumed();
			finished();
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
        
        /**
         * The  method that reifies the apform component in APAM
         */
        protected abstract Component reify();

        /**
         * The required components that need to be already refified
         * in APAM as a requisites to start reifying this component
         */
        protected abstract List<ComponentReference<?>> getRequirements();
        
        /*
         * (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            try {
            	started();
            	
            	/*
            	 * Wait for required components
            	 */
            	for (ComponentReference<?> requirement : getRequirements()) {
            		CST.componentBroker.getWaitComponent(requirement.getName());
				}
            	
            	/*
            	 * perform reification and notify after completion
            	 */
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
        protected List<ComponentReference<?>> getRequirements() {
        	return Collections.<ComponentReference<?>>singletonList(getComponent().getDeclaration().getImplementation());
        }
        
        @Override
        public Component reify() {
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
        protected List<ComponentReference<?>> getRequirements() {
        	ComponentReference<?> specification = getComponent().getDeclaration().getSpecification();
           	if (specification != null)
           		return Collections.<ComponentReference<?>>singletonList(specification);

           	return Collections.emptyList();
        }
        
        @Override
        public Component reify() {
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
        protected List<ComponentReference<?>> getRequirements() {
        	return Collections.emptyList();
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

//    /**
//     * The instance called "instance name" just disappeared from the platform.
//     * 
//     * @param instanceName
//     */
//    public static void vanishInstance(String instanceName) {
//        Instance inst = CST.componentBroker.getInst(instanceName);
//        if (inst == null) {
//          // previous remove of the factory removed instances
//          // logger.warn("Unable to remove instance '{}' : non-existent instance", instanceName);
//            return;
//        }
//        ((ComponentBrokerImpl)CST.componentBroker).removeInst(inst);
//        
//    }
//
//    /**
//     * * The implementation called "implementation name" just disappeared from the platform.
//     * 
//     * @param implementationName
//     */
//    public static void vanishImplementation(String implementationName) {
//        Implementation impl = CST.componentBroker.getImpl(implementationName);
//        if (impl == null) {
//        	logger.warn("Vanish implementation does not exists: " + implementationName);
//            return;
//        }
//
//        ((ComponentBrokerImpl)CST.componentBroker).removeImpl(impl);
//    }
//
//    /**
//     * 
//     * @param specificationName
//     */
//    public static void vanishSpecification(String specificationName) {
//        Specification spec = CST.componentBroker.getSpec(specificationName);
//        if (spec == null) {
//        	logger.warn("Vanish specification does not exists: " + specificationName);
//            return;
//        }
//    	
//        ((ComponentBrokerImpl)CST.componentBroker).removeSpec(spec);
//    }

}
