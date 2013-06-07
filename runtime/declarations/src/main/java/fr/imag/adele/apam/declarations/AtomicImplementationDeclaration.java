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
package fr.imag.adele.apam.declarations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.declarations.CallbackMethod.CallbackTrigger;

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
        String getClassName();


        /**
         * The type of the specified java field
         */
        ResourceReference getFieldType(String fieldName) throws NoSuchFieldException;     
        
        /**
         * The cardinality of the specified java field
         */
        boolean isCollectionField(String fieldName) throws NoSuchFieldException;

        /**
         * The call back method specified, it return true if the method has a unique argument which is Instance
         */
        Set<?> getCallbacks(String callbackName, boolean mandatoryInstanceArgument) throws NoSuchMethodException;
        
        /**
         * The type of return method specified in java call back method
         */
        ResourceReference getCallbackReturnType(String methodName, String type) throws NoSuchMethodException;
        
        /**
         * The type of argument method specified in java call back method
         */
        ResourceReference getCallbackArgType(String methodName, String type) throws NoSuchMethodException;

        /**
         * The cardinality of the specified java return
         */
        boolean isCollectionReturn(String methodName, String type) throws NoSuchMethodException;

        /**
         * The cardinality of the specified java argument
         */
        boolean isCollectionArgument(String methodName, String type) throws NoSuchMethodException;

    }

    /**
     * A reference to the instrumentation data associated with this implementation
     */
    private final Instrumentation                    instrumentation;

    /**
     * The list of injected fields declared for dependencies of this implementation
     */
    private final Set<RelationInjection>           relationInjections;

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
        this.relationInjections = new HashSet<RelationInjection>();
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
    public Set<RelationInjection> getRelationInjections() {
        return relationInjections;
    }

    /**
     * The list of method that must be intercepted in this implementation for handling message producers
     */
    public Set<MessageProducerMethodInterception> getProducerInjections() {
        return producerInjections;
    }

    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer();
        if (callbacks.size() != 0) {
            ret = ret.append("\n    callback methods : ");
            for (CallbackTrigger trigger : callbacks.keySet()) {
            	ret = ret.append( " " + trigger + " : " + callbacks.get(trigger)) ;
            }
        }
        if (relationInjections.size() != 0) {
        	ret = ret.append("\n    Injected fields/methods : ");
            for (RelationInjection injection : relationInjections) {
            	ret = ret.append(" " + injection.getName());
            }
        }
        if (producerInjections.size() != 0) {
        	ret = ret.append("\n    Intercepted message producer methods : ");
            for (MessageProducerMethodInterception injection : producerInjections) {
            	ret = ret.append(" " + injection.getMethoddName());
            }
        }

        ret = ret.append("\n   Class Name: " + getClassName());
        return ret.toString();
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
