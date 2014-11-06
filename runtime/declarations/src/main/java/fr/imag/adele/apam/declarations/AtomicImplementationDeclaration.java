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

import fr.imag.adele.apam.declarations.references.components.ImplementationReference;
import fr.imag.adele.apam.declarations.references.components.Versioned;

/**
 * This class represents the declaration of a java implementation of a service
 * provider
 * 
 * @author vega
 * 
 */
public class AtomicImplementationDeclaration extends ImplementationDeclaration {

	/**
	 * An interface giving access to reflection data associated with this
	 * implementation
	 */
	public interface CodeReflection {

		/**
		 * The name of the associated java class
		 */
		String getClassName();

		/**
		 * Whether the specified java field is one of the supported collections
		 */
		boolean isCollectionField(String fieldName) throws NoSuchFieldException;

		/**
		 * Whether the specified java field is one of the supported message queues
		 */
		boolean isMessageQueueField(String fieldName) throws NoSuchFieldException;

		/**
		 * The type of the specified java field, for collections is the type of the element type
		 */
		String getFieldType(String fieldName) throws NoSuchFieldException;
	
		/**
		 * The type of the specified java field 
		 */
		String getDeclaredFieldType(String fieldName) throws NoSuchFieldException;
		
		/**
		 * A special type to signal an unknown field type
		 */
		public static final String UNKNOWN_TYPE = new String("<UNKNOWN_TYPE>");
		
		/**
		 * The number of parameters of the specified java method
		 */
		int getMethodParameterNumber(String methodName, boolean includeInherited) throws NoSuchMethodException;

		/**
		 * The type of of the specified single-parameter java method
		 */
		String getMethodParameterType(String methodName, boolean includeInherited) throws NoSuchMethodException;

		/**
		 * The list of parameter types
		 */
		String[] getMethodParameterTypes(String methodName,	boolean includeInherited) throws NoSuchMethodException;

		/**
		 * The type of return of the specified java method
		 */
		String getMethodReturnType(String methodName, String signature,	boolean includeInherited) throws NoSuchMethodException;

	}

	/**
	 * The events associated to the runtime life-cycle of the component
	 */
	public enum Event {
		INIT, REMOVE
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
	 * A reference to the reflection data associated with this implementation
	 */
	private final CodeReflection reflection;


	/**
	 * The list of code instrumentation to perform for handling resource
	 * providing
	 */
	private final Set<ProviderInstrumentation> providerInstrumentations;

	/**
	 * The map of list of call back methods associated to the same trigger
	 */
	private Map<Event, Set<CallbackDeclaration>> callbacks;


	public AtomicImplementationDeclaration(String name, Versioned<SpecificationDeclaration> specification, CodeReflection reflection) {
		super(name, specification);

		assert reflection != null;

		this.reflection 				= reflection;
		this.providerInstrumentations	= new HashSet<ProviderInstrumentation>();
		this.callbacks 					= new HashMap<Event, Set<CallbackDeclaration>>();
	}

	
	/**
	 * Clone this declaration
	 */
	protected AtomicImplementationDeclaration(AtomicImplementationDeclaration original) {
		super(original);
		
		this.reflection 				= original.reflection;
		this.providerInstrumentations	= new HashSet<ProviderInstrumentation>(original.providerInstrumentations);
		
		/*
		 * make a deep copy of the callback map
		 */
		this.callbacks 					= new HashMap<Event, Set<CallbackDeclaration>>();
		for (Map.Entry<Event,Set<CallbackDeclaration>> callbackEntry : original.callbacks.entrySet()) {
			this.callbacks.put(callbackEntry.getKey(), new HashSet<CallbackDeclaration>(callbackEntry.getValue()));
		}
	}

	/**
	 * Generates the reference to this implementation
	 */
	@Override
	protected ImplementationReference<AtomicImplementationDeclaration> generateReference() {
		return new Reference(getName());
	}

	@Override
	@SuppressWarnings("unchecked")
	public ImplementationReference<AtomicImplementationDeclaration> getReference() {
		return (ImplementationReference<AtomicImplementationDeclaration>) super.getReference();
	}

	public void addCallback(Event trigger, CallbackDeclaration callback) {

		if (callbacks.get(trigger) == null) {
			callbacks.put(trigger, new HashSet<CallbackDeclaration>());
		}

		callbacks.get(trigger).add(callback);

	}

	public Set<CallbackDeclaration> getCallback(Event trigger) {
		return callbacks.get(trigger);
	}

	/**
	 * The name of the class implementing the service provider
	 */
	public String getClassName() {
		return reflection.getClassName();
	}

	/**
	 * The list of code instrumentation to perform in this implementation for
	 * providing resources
	 */
	public Set<ProviderInstrumentation> getProviderInstrumentation() {
		return providerInstrumentations;
	}

	/**
	 * The reflection data associated with this implementation
	 */
	public CodeReflection getReflection() {
		return reflection;
	}


	@Override
	public String toString() {
		StringBuffer ret = new StringBuffer(super.toString());
		if (callbacks.size() != 0) {
			ret = ret.append("\n    callback methods : ");
			for (Event trigger : callbacks.keySet()) {
				ret = ret
						.append(" " + trigger + " : " + callbacks.get(trigger));
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

}
