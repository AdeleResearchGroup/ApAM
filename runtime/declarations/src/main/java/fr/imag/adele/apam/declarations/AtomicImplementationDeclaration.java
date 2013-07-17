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

/**
 * This class represents the declaration of a java implementation of a service provider
 * 
 * @author vega
 * 
 */
public class AtomicImplementationDeclaration extends ImplementationDeclaration {

    /**
     * An interface giving access to reflection data associated with this implementation
     */
    public interface CodeReflection {

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
         * The type of return of the specified java method 
         */
        String getMethodReturnType(String methodName, String signature, boolean includeInherited) throws NoSuchMethodException;
        
        /**
         * The number of parameters of the specified java methos
         */
        int getMethodArgumentNumber(String methodName, boolean includeInherited) throws NoSuchMethodException;
        
        /**
         * The type of the first parameter of the specified java method
         */
        String getMethodArgumentType(String methodName, boolean includeInherited) throws NoSuchMethodException;
        
        /**
         * Return the list of argument types
         * @param methodName
         * @param includeInherited
         * @return
         * @throws NoSuchMethodException
         */
        
        public String[] getMethodArgumentTypes(String methodName, boolean includeInherited) throws NoSuchMethodException;
        

    }

    /**
     * A reference to the reflection data associated with this implementation
     */
    private final CodeReflection                    reflection;

    /**
     * The list of code instrumentation to perform for handling dependencies
     */
    private final Set<RequirerInstrumentation>		requirerInstrumentations;

    /**
     * The list of code instrumentation to perform for handling resource providing
     */
    private final Set<ProviderInstrumentation> 		providerInstrumentations;

	/**
	 * The events associated to the runtime life-cycle of the component
	 */
	public enum Event {
		INIT,
		REMOVE
	}

    /**
     * The map of list of call back methods associated to the same trigger
     */
    private Map<Event, Set<CallbackDeclaration>> callbacks;

    public AtomicImplementationDeclaration(String name, SpecificationReference specification, CodeReflection reflection) {
        super(name, specification);

        assert reflection != null;

        this.reflection 				= reflection;
        this.requirerInstrumentations	= new HashSet<RequirerInstrumentation>();
        this.providerInstrumentations 	= new HashSet<ProviderInstrumentation>();
        
        this.callbacks 					= new HashMap<Event, Set<CallbackDeclaration>>();
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
     * The reflection data associated with this implementation
     */
    public CodeReflection getReflection() {
        return reflection;
    }

    /**
     * The name of the class implementing the service provider
     */
    public String getClassName() {
        return reflection.getClassName();
    }

    /**
     * The list of reflection to perform in this implementation for handling dependencies
     */
    public Set<RequirerInstrumentation> getRequirerInstrumentation() {
        return requirerInstrumentations;
    }

    /**
     * The list of code instrumentation to perform in this implementation for providing resources
     */
    public Set<ProviderInstrumentation> getProviderInstrumentation() {
        return providerInstrumentations;
    }

    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer();
        if (callbacks.size() != 0) {
            ret = ret.append("\n    callback methods : ");
            for (Event trigger : callbacks.keySet()) {
            	ret = ret.append( " " + trigger + " : " + callbacks.get(trigger)) ;
            }
        }
        if (requirerInstrumentations.size() != 0) {
        	ret = ret.append("\n    Injected fields/methods : ");
            for (RequirerInstrumentation injection : requirerInstrumentations) {
            	ret = ret.append(" " + injection.getName());
            }
        }
        if (providerInstrumentations.size() != 0) {
        	ret = ret.append("\n    Intercepted message producer methods : ");
            for (ProviderInstrumentation injection : providerInstrumentations) {
            	ret = ret.append(" " + injection.getName());
            }
        }

        ret = ret.append("\n   Class Name: " + getClassName());
        return ret.toString();
    }

    public void addCallback(Event trigger, CallbackDeclaration callback) {
       
    	if(callbacks.get(trigger)==null){ 
           callbacks.put(trigger,new HashSet<CallbackDeclaration>());
       }       
       
       callbacks.get(trigger).add(callback);
        
    }

    public Set<CallbackDeclaration> getCallback(Event trigger) {
        return callbacks.get(trigger);
    }

    
}
