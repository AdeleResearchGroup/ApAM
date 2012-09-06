package fr.imag.adele.apam.apform;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.impl.ImplementationBrokerImpl;
import fr.imag.adele.apam.impl.InstanceBrokerImpl;
import fr.imag.adele.apam.impl.SpecificationBrokerImpl;

public class Apform2Apam {
	
    private static Logger logger = LoggerFactory.getLogger(Apform2Apam.class);
    
    /**
     * The components that clients are waiting for deployment to complete
     */
    private static Set<String> expectedComponents = new HashSet<String>();
    
    /**
     * The event executor. We use a pool of a threads to handle notification to APAM of underlying platform
     * events, without blocking the platform thread.
     */
    static private final Executor executor      = Executors.newCachedThreadPool();
    
    /**
     * The base class of all the reification processors. This handle exception and context management
     * 
     * @author vega
     * 
     */
    private static abstract class ApamReificationProcess implements Runnable {

        @Override
        public void run() {
            try {
                notifyDeployment(reify());
            } catch (Exception unhandledException) {
                logger.error("Error handling Apform event :");
                unhandledException.printStackTrace(System.err);
            } finally {
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

    /**
     * Wait for a future component to be deployed
     */
    public static void waitForComponent(String componentName) {

        synchronized (Apform2Apam.expectedComponents) {
            Apform2Apam.expectedComponents.add(componentName);
            try {
                while (Apform2Apam.expectedComponents.contains(componentName))
                    Apform2Apam.expectedComponents.wait();
            } catch (InterruptedException interrupted) {
                interrupted.printStackTrace();
            }
            return;
        }
    }
    
    /**
     * Task to handle instance appearance
     * 
     * @author vega
     * 
     */
    private static class InstanceAppearenceProcessing extends ApamReificationProcess {

        private final ApformInstance instance;

        public InstanceAppearenceProcessing(ApformInstance instance) {
            this.instance = instance;
        }

        @Override
        public Component reify() {
            return CST.InstBroker.addInst(null,instance);
        }

    }

    /**
     * Task to handle implementation deployment
     * 
     * @author vega
     * 
     */
    private static class ImplementationDeploymentProcessing extends ApamReificationProcess {

        private final ApformImplementation implementation;

        public ImplementationDeploymentProcessing(ApformImplementation implementation) {
            this.implementation = implementation;
        }

        @Override
        public Component reify() {
            return CST.ImplBroker.addImpl(null,implementation);
        }

    }

    /**
     * Task to handle specification deployment
     * 
     * @author vega
     * 
     */
    private static class SpecificationDeploymentProcessing extends ApamReificationProcess {

        private final ApformSpecification specification;

        public SpecificationDeploymentProcessing(ApformSpecification specification) {
            this.specification = specification;
        }

        @Override
        public Component reify() {
            return CST.SpecBroker.addSpec(specification);
            
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
        Instance inst = CST.InstBroker.getInst(instanceName);
        if (inst == null) {
          // previous remove of the factory removed instances
          // logger.warn("Unable to remove instance '{}' : non-existent instance", instanceName);
            return;
        }
        ((InstanceBrokerImpl)CST.InstBroker).removeInst(inst);
        
    }

    /**
     * * The implementation called "implementation name" just disappeared from the platform.
     * 
     * @param implementationName
     */
    public static void vanishImplementation(String implementationName) {
        Implementation impl = CST.ImplBroker.getImpl(implementationName);
        if (impl == null) {
        	logger.warn("Vanish implementation does not exists: " + implementationName);
            return;
        }

        ((ImplementationBrokerImpl)CST.ImplBroker).removeImpl(impl);
    }

    /**
     * 
     * @param specificationName
     */
    public static void vanishSpecification(String specificationName) {
        Specification spec = CST.SpecBroker.getSpec(specificationName);
        if (spec == null) {
        	logger.warn("Vanish specification does not exists: " + specificationName);
            return;
        }
    	
        ((SpecificationBrokerImpl)CST.SpecBroker).removeSpec(spec);
    }

}
