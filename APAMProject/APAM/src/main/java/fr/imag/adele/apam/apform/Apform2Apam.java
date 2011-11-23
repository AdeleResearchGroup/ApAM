package fr.imag.adele.apam.apform;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.apamImpl.CompositeImpl;
import fr.imag.adele.apam.apamImpl.CompositeTypeImpl;
import fr.imag.adele.apam.apamImpl.ImplementationBrokerImpl;
import fr.imag.adele.apam.apamImpl.SpecificationImpl;

public class Apform2Apam {
//    static Set<String>  expectedDeployedImpls = new HashSet<String>();

    static final CompositeType    rootType      = CompositeTypeImpl.getRootCompositeType();
    static final Composite        rootInst      = CompositeImpl.getRootAllComposites();
    static Set<Implementation>    unusedImplems = CompositeTypeImpl.getRootCompositeType().getImpls();

    /**
     * The event executor. We use a pool of a single thread to process events in their arrival
     * order and never block the underlying platform thread that is invoking Apform.
     */
    static private final Executor executor      = Executors.newCachedThreadPool();

    /*new ThreadPoolExecutor(1, 1,
              0L, TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<Runnable>());*/

    /**
     * Execution context of the currently processed event
     */

    private static class ProcessingContext {
    }

    /**
     * The execution context of the currently processed event.
     * Null if this thread is not the event processing thread.
     */
    public static final ProcessingContext getContext() {
        return Apform2Apam.context.get();
    }

    private static final ThreadLocal<ProcessingContext> context = new ThreadLocal<ProcessingContext>();

    /**
     * The base class of all the event processors. This handle exception and context management
     * 
     * @author vega
     * 
     */
    private static abstract class ApformEventProcessing implements Runnable {

        @Override
        public void run() {
            try {
                Apform2Apam.context.set(new ProcessingContext());
                process();
            } catch (Exception unhandledException) {
                System.err.println("Error handling Apform event :");
                unhandledException.printStackTrace(System.err);
            } finally {
                Apform2Apam.context.set(null);
            }

        }

        /**
         * The processing method
         */
        protected abstract void process();

    }

    /**
     * Wait for a future implementation to be registered
     */
    public static void waitForImplementation(String implementationName) {

        /*
         * If the calling thread is not the event processing thread just block it waiting for the
         * event to arrive. It is the normal case.
         */
        // if (Apform2Apam.getContext() == null) {
        synchronized (Apform2Apam.expectedImpls) {
            Apform2Apam.expectedImpls.add(implementationName);
            try {
                while (Apform2Apam.expectedImpls.contains(implementationName))
                    Apform2Apam.expectedImpls.wait();
            } catch (InterruptedException interrupted) {
                interrupted.printStackTrace();
            }
            return;
        }
        // }

//        /*
//         * Otherwise we have a problem : the current event processing needs to wait for a future event,
//         * this reflects some implicit event correlation that can not be handled by our simple policy
//         * of processing events sequentially in arrival order.
//         * 
//         * As a workaround we try to see if the expected event is already queued and process it synchronously
//         * without blocking.
//         * 
//         * If the event is not already queued the situation can not be handled by our current implementation.
//         */
//        /*
//        ImplementationRegistrationProcessing expectedEvent = null;
//        for (Runnable pendingEvent : Apform2Apam.executor.getQueue()) {
//
//            if (!(pendingEvent instanceof ImplementationRegistrationProcessing))
//                continue;
//
//            /*
//             * Check if it is the expected implementation
//             */
//            ImplementationRegistrationProcessing pendingRegistration = (ImplementationRegistrationProcessing) pendingEvent;
//            if (pendingRegistration.implementationName.equals(implementationName)) {
//                expectedEvent = pendingRegistration;
//                break;
//            }
//        }
//*/
//        /*
//         * We could not find the expected event, just abort as we can not process this event
//         */
//        if (expectedEvent == null)
//            throw new UnsupportedOperationException("Deadlock while processing apform event");
//
//        /*
//         * remove event from the pending queue and process it synchronously
//         */
//        Apform2Apam.executor.getQueue().remove(expectedEvent);
//        expectedEvent.run();
//        */
    }

    public static Set<String> expectedImpls = new HashSet<String>();

    /**
     * Task to handle instance registration
     * 
     * @author vega
     * 
     */
    private static class InstanceRegistrationProcessing extends ApformEventProcessing {

        private final String         instanceName;
        private final ApformInstance instance;

        public InstanceRegistrationProcessing(String instanceName, ApformInstance instance) {
            this.instanceName = instanceName;
            this.instance = instance;
        }

        @Override
        public void process() {
//            Instance inst = CST.InstBroker.getInst(instanceName);
//            if (inst != null) {
//                System.err.println("Instance already existing: " + instanceName);
//                return;
//            }
            if (CST.ImplBroker.getImpl(instance.getImplemName()) == null)
                Apform2Apam.waitForImplementation(instance.getImplemName());
            CST.InstBroker.addInst(Apform2Apam.rootInst, instance, instance.getProperties());
        }

    }

    /**
     * Task to handle implementation registration
     * 
     * @author vega
     * 
     */
    private static class ImplementationRegistrationProcessing extends ApformEventProcessing {

        private final String               implementationName;
        private final ApformImplementation implementation;

        public ImplementationRegistrationProcessing(String implementationName, ApformImplementation implementation) {
            this.implementationName = implementationName;
            this.implementation = implementation;
        }

        @Override
        public void process() {

            Implementation impl = Apform.getUnusedImplem(implementationName);
            if (impl != null) {
                System.err.println("Implementation already existing: " + implementationName);
                return;
            }

            impl = ((ImplementationBrokerImpl) CST.ImplBroker).addImpl(Apform2Apam.rootType, implementation,
                    implementation.getProperties());

            // wake up any threads waiting for this implementation
            synchronized (Apform2Apam.expectedImpls) {
                if (Apform2Apam.expectedImpls.contains(implementationName)) { // it is expected
                    Apform2Apam.expectedImpls.remove(implementationName);
                    Apform2Apam.expectedImpls.notifyAll(); // wake up the thread waiting in waitForImplementation
                }
            }

        }

    }

    /**
     * Task to handle specification registration
     * 
     * @author vega
     * 
     */
    private static class SpecificationRegistrationProcessing extends ApformEventProcessing {

        private final String              specificationName;
        private final ApformSpecification specification;

        public SpecificationRegistrationProcessing(String specificationName, ApformSpecification specification) {
            this.specificationName = specificationName;
            this.specification = specification;
        }

        @Override
        public void process() {

            Specification spec = CST.SpecBroker.getSpec(specificationName);
            if (spec != null) {
                System.err.println("Specification already existing: merging with " + specificationName);
                ((SpecificationImpl) spec).setSamSpec(specification);
                return;
            }

            spec = CST.SpecBroker.addSpec(specificationName, specification, specification.getProperties());
        }
    }

//    synchronized (ApformImpl.expectedImpls) {
//  if (ApformImpl.expectedImpls.contains(instanceName)) { // it is expected
//      ApformImpl.expectedImpls.remove(instanceName);
//      ApformImpl.expectedImpls.notifyAll(); // wake up the thread waiting in getImplementation
//  }
//}

//synchronized (Apform.expectedMngImpls) {
//  if (Apform.expectedMngImpls.containsKey(instanceName)) {
//      if (Apform.expectedMngImpls.keySet().contains(instanceName)) {
//          for (DynamicManager manager : Apform.expectedMngImpls.get(instanceName)) {
//              manager.appeared(inst);
//          }
//          Apform.expectedMngImpls.remove(instanceName);
//      }
//  }
//}
//
//synchronized (Apform.expectedMngInterfaces) {
//  if (inst.getImpl().getSpec() != null) {
//      for (String interf : inst.getImpl().getSpec().getInterfaces()) {
//          if (Apform.expectedMngInterfaces.get(interf) != null) {
//              for (DynamicManager manager : Apform.expectedMngInterfaces.get(interf)) {
//                  manager.appeared(inst);
//              }
//          }
//          Apform.expectedMngInterfaces.remove(interf);
//      }
//  }
//}
//}

    /**
     * A new instance, represented by object "client" just appeared in the platform.
     */
    public static void newInstance(String instanceName, ApformInstance client) {
        Apform2Apam.executor.execute(new InstanceRegistrationProcessing(instanceName, client));
    }

    /**
     * A new implementation, represented by object "client" just appeared in the platform.
     * 
     * @param implemName : the symbolic name.
     * @param client
     */
    public static void newImplementation(String implemName, ApformImplementation client) {
        Apform2Apam.executor.execute(new ImplementationRegistrationProcessing(implemName, client));
    }

    /**
     * A new specification, represented by object "client" just appeared in the platform.
     * 
     * @param specName
     * @param client
     */
    public static void newSpecification(String specName, ApformSpecification client) {
        Apform2Apam.executor.execute(new SpecificationRegistrationProcessing(specName, client));
    }

    /**
     * The instance called "instance name" just disappeared from the platform.
     * 
     * @param instanceName
     */
    public static void vanishInstance(String instanceName) {
        // TODO Auto-generated method stub

    }

    /**
     * * The implementation called "implementation name" just disappeared from the platform.
     * 
     * @param implementationName
     */
    public static void vanishImplementation(String implementationName) {
        // TODO Auto-generated method stub

    }

    /**
     * 
     * @param specificationName
     */
    public static void vanishSpecification(String specificationName) {
        // TODO Auto-generated method stub

    }

}
