package fr.imag.adele.apam.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

        /**
         * The type of the specified java message call back method
         */
        public ResourceReference getCallbackType(String callbackName) throws NoSuchMethodException;

        /**
         * The type of the specified java field
         */
        public ResourceReference getFieldType(String fieldName) throws NoSuchFieldException;

        /**
         * The cardinality of the specified java field
         */
        public boolean isCollectionField(String fieldName) throws NoSuchFieldException;

        /**
         * The call back method specified, it return if the method has a unique Instance
         */
        public boolean checkCallback(String callbackName) throws NoSuchMethodException;

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
    private final Set<MessageProducerFieldInjection> producerInjections;

    /**
     * The map of list of call back methods associated to the same trigger
     */
    private Map<String, Set<CallbackMethod>> callbacks;

    public AtomicImplementationDeclaration(String name, SpecificationReference specification,
            Instrumentation instrumentation) {
        super(name, specification);

        assert instrumentation != null;

        this.instrumentation = instrumentation;
        this.dependencyInjections = new HashSet<DependencyInjection>();
        this.producerInjections = new HashSet<MessageProducerFieldInjection>();
        this.callbacks = new HashMap<String, Set<CallbackMethod>>();
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
     * The list of fields that must be injected in this implementation for handling message producers
     */
    public Set<MessageProducerFieldInjection> getProducerInjections() {
        return producerInjections;
    }

    @Override
    public String toString() {
        String ret = super.toString();
        if (callbacks.size() != 0) {
            ret += "\n    callback methods : ";
            for (String trigger : callbacks.keySet()) {
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
            ret += "\n    Injected message producer fields : ";
            for (MessageProducerFieldInjection injection : producerInjections) {
                ret += " " + injection.getFieldName();
            }
        }

        ret += "\n   Class Name: " + getClassName();
        return ret;
    }

    public void addCallbacks(CallbackMethod callback) {
       if(callbacks.get(callback.trigger)==null){
           callbacks.put(callback.trigger,new HashSet<CallbackMethod>());
       }       
       callbacks.get(callback.trigger).add(callback);
        
    }

    public Set<CallbackMethod> getCallback(String trigger) {
        return callbacks.get(trigger);
    }
    
}
