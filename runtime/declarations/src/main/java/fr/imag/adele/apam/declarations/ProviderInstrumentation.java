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

import fr.imag.adele.apam.declarations.instrumentation.Instrumentation;
import fr.imag.adele.apam.declarations.instrumentation.InstrumentedClass;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.resources.MessageReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;

/**
 * The declaration of a code instrumentation (injection, interception, invocation, ...) that must be performed
 * at runtime to implement the actual execution semantics of the the providing end of a dependency.
 * 
 * @author vega
 * 
 */
public abstract class ProviderInstrumentation extends Instrumentation {

	/**
	 * This class declares instrumentation for message providers that is based
	 * on intercepting the return value of a method invocation
	 * 
	 */
	public static class MessageProviderMethodInterception extends ProviderInstrumentation {

		/**
		 * The name of the method that must be intercepted
		 */
		private final String methodName;

		/**
		 * The signature of the method that must be intercepted
		 */
		private final String methodSignature;

		/**
		 * The return type of the specified method
		 */
		private final Lazy<MessageReference> methodReturnType = new Lazy<MessageReference>() {

			@Override
			protected MessageReference evaluate(InstrumentedClass instrumentedClass) {
				try {
					return new MessageReference(instrumentedClass.getMethodReturnType(methodName, methodSignature, false));
				} catch (NoSuchMethodException e) {
					return null;
				}
			}
		};

		public MessageProviderMethodInterception(AtomicImplementationDeclaration implementation, String methodName, String methodSignature) {
			super(implementation.getReference(), implementation.getImplementationClass());

			assert methodName != null;

			this.methodName = methodName;
			this.methodSignature = methodSignature;
		}

		/**
		 * The name of the method to intercept
		 */
		public String getMethodName() {
			return methodName;
		}

		/**
		 * The signature of the parameters of the method to intercept
		 */
		public String getMethodSignature() {
			return methodSignature;
		}

		@Override
		public String getName() {
			return methodName;
		}

		/**
		 * The type of the java resource that needs to be provided at runtime by
		 * the component to perform this instrumentation
		 */
		@Override
		public ResourceReference getProvidedResource() {
			MessageReference target = methodReturnType.get();
			return target != null ? target : new UnknownReference(new MessageReference(methodName));
		}

		@Override
		public boolean isValidInstrumentation() {
			return methodReturnType.get() != null;
		}

		@Override
		public String toString() {
			return "method " + methodName + ": " + getProvidedResource().getJavaType();
		}

	}

	protected ProviderInstrumentation(ComponentReference<AtomicImplementationDeclaration> implementation, InstrumentedClass instrumentedClass) {
		super(implementation,instrumentedClass);
	}

	/**
	 * The type of the java resource that needs to be provided at runtime by the
	 * component to perform this instrumentation
	 */
	public abstract ResourceReference getProvidedResource();
}