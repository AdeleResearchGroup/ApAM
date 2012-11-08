package fr.imag.adele.apam.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.core.CallbackMethod.CallbackTrigger;

/**
 * This class represents the declaration of a java implementation of a service provider
 * 
 * @author vega
 * 
 */
public class AtomicImplementationDeclaration extends ImplementationDeclaration {

    /**
     * An interface giving access to instrumentation data associated with this implementation
     */
    public interface Instrumentation {

        /**
         * The name of the associated java class
         */
        public String getClassName();

//        /**
//         * The type of the specified java message call back method
//         */
//        public ResourceReference getCallbackType(String callbackName) throws NoSuchMethodException;

        /**
         * The type of the specified java field
         */
        public ResourceReference getFieldType(String fieldName) throws NoSuchFieldException;     
        
        /**
         * The cardinality of the specified java field
         */
        public boolean isCollectionField(String fieldName) throws NoSuchFieldException;

        /**
         * The call back method specified, it return true if the method has a unique argument which is Instance
         */
        public Set<?> getCallbacks(String callbackName, boolean mandatoryInstanceArgument) throws NoSuchMethodException;
        
        /**
         * The type of return method specified in java call back method
         */
        public ResourceReference getCallbackReturnType(String methodName, String type) throws NoSuchMethodException;
        
        /**
         * The type of argument method specified in java call back method
         */
        public ResourceReference getCallbackArgType(String methodName, String type) throws NoSuchMethodException;

        /**
         * The cardinality of the specified java return
         */
        public boolean isCollectionReturn(String methodName, String type) throws NoSuchMethodException;

        /**
         * The cardinality of the specified java argument
         */
        public boolean isCollectionArgument(String methodName, String type) throws NoSuchMethodException;

    }

    /**
     * A reference to the instrumentation data associated with this implementation
     */
    private final Instrumentation                    instrumentation;

    /**
     * The list of injected fields declared for dependencies of this implementation
     */
    private final Set<DependencyInjection>           dependencyInjections;

    /**
     * The list of injected fields declared for message producers of this implementation
     */
    private final Set<MessageProducerMethodInterception> producerInjections;

    /**
     * The map of list of call back methods associated to the same trigger
     */
    private Map<CallbackTrigger, Set<CallbackMethod>> callbacks;

    public AtomicImplementationDeclaration(String name, SpecificationReference specification,
            Instrumentation instrumentation) {
        super(name, specification);

        assert instrumentation != null;

        this.instrumentation = instrumentation;
        this.dependencyInjections = new HashSet<DependencyInjection>();
        this.producerInjections = new HashSet<MessageProducerMethodInterception>();
        this.callbacks = new HashMap<CallbackTrigger, Set<CallbackMethod>>();
    }

    /**
     * A reference to an atomic implementation
     */
    private static class Reference extends ImplementationReference<AtomicImplementationDeclaration> {

        public Reference(String name) {
            super(name);
        }

    }

    /**
     * Generates the reference to this implementation
     */
    @Override
    protected ImplementationReference<AtomicImplementationDeclaration> generateReference() {
        return new Reference(getName());
    }

    /**
     * The instrumentation data associated with this implementation
     */
    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    /**
     * The name of the class implementing the service provider
     */
    public String getClassName() {
        return instrumentation.getClassName();
    }

    /**
     * The list of fields that must be injected in this implementation for handling dependencies
     */
    public Set<DependencyInjection> getDependencyInjections() {
        return dependencyInjections;
    }

    /**
     * The list of method that must be intercepted in this implementation for handling message producers
     */
    public Set<MessageProducerMethodInterception> getProducerInjections() {
        return producerInjections;
    }

    @Override
    public String toString() {
        String ret = super.toString();
        if (callbacks.size() != 0) {
            ret += "\n    callback methods : ";
            for (CallbackTrigger trigger : callbacks.keySet()) {
                ret += " " + trigger + " : " + callbacks.get(trigger) ;
            }
        }
        if (dependencyInjections.size() != 0) {
            ret += "\n    Injected fields/methods : ";
            for (DependencyInjection injection : dependencyInjections) {
                ret += " " + injection.getName();
            }
        }
        if (producerInjections.size() != 0) {
            ret += "\n    Intercepted message producer methods : ";
            for (MessageProducerMethodInterception injection : producerInjections) {
                ret += " " + injection.getMethoddName();
            }
        }

        ret += "\n   Class Name: " + getClassName();
        return ret;
    }

    public void addCallback(CallbackMethod callback) {
       if(callbacks.get(callback.trigger)==null){ // Initialize list of call back
           callbacks.put(callback.trigger,new HashSet<CallbackMethod>());
       }       
       callbacks.get(callback.trigger).add(callback);
        
    }

    public Set<CallbackMethod> getCallback(CallbackTrigger trigger) {
        return callbacks.get(trigger);
    }

    
}
