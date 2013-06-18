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

import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration.CodeReflection;

/**
 * The declaration of a code instrumentation (injection, interception, invocation, ...)
 * that must be performed at runtime to implement the actual execution semantics of the
 * the providing end of a dependency.
 * 
 * @author vega
 * 
 */
public abstract class ProviderInstrumentation extends Instrumentation {

	protected ProviderInstrumentation(AtomicImplementationDeclaration implementation) {

		super(implementation);

		assert implementation != null;
	}

	/**
	 * An unique identifier for this injection, within the scope of the
	 * declaring implementation
	 */
	public abstract String getName();
	
	/**
	 * The type of the java resource that needs to be provided at runtime by the
	 * component to perform this instrumentation
	 */
	public abstract ResourceReference getProvidedResource();

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
			protected MessageReference evaluate(CodeReflection reflection) {
				try {
					return new MessageReference(reflection.getMethodReturnType(methodName,methodSignature));
				} catch (NoSuchMethodException e) {
					return null;
				}
			}
		};

		public MessageProviderMethodInterception(AtomicImplementationDeclaration implementation, String methodName, String methodSignature) {
			super(implementation);

			assert methodName != null;

			this.methodName = methodName;
			this.methodSignature = methodSignature;
		}
		
		@Override
		public String getName() {
			return methodName;
		}

		@Override
		public boolean isValidInstrumentation() {
			return methodReturnType.get() != null;
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

		/**
		 * The type of the java resource that needs to be provided at runtime by
		 * the component to perform this instrumentation
		 */
		public ResourceReference getProvidedResource() {
			MessageReference target = methodReturnType.get();
			return target != null ? target : new UndefinedReference(methodName,MessageReference.class);
		}

		
		@Override
		public String toString() {
			return "method " + methodName + ": "+ getProvidedResource().getJavaType();
		}


	}
}