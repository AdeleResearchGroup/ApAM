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

import fr.imag.adele.apam.declarations.instrumentation.InjectedField;
import fr.imag.adele.apam.declarations.instrumentation.Instrumentation;
import fr.imag.adele.apam.declarations.instrumentation.InstrumentedClass;
import fr.imag.adele.apam.declarations.references.resources.InterfaceReference;
import fr.imag.adele.apam.declarations.references.resources.MessageReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;

/**
 * The declaration of a code instrumentation (injection, interception, invocation, ...) that must be performed at runtime
 * to implement the actual execution semantics of the the requiring end of a dependency.
 * 
 * @author vega
 * 
 */
public interface RequirerInstrumentation  {

	/**
	 * A callback to push messages to consumer
	 */
	public static class MessageConsumerCallback extends Instrumentation implements RequirerInstrumentation {

		private final String methodName;

		private final Lazy<String> argumentType = new Lazy<String>() {
			@Override
			protected String evaluate(InstrumentedClass instrumentedClass) {
				try {
					return instrumentedClass.getMethodParameterType(methodName, true);
				} catch (NoSuchMethodException e) {
					return null;
				}
			};

		};

		public MessageConsumerCallback(AtomicImplementationDeclaration implementation, String methodName) {
			super(implementation.getReference(), implementation.getImplementationClass());
			this.methodName = methodName;
		}

		@Override
		public boolean acceptMultipleProviders() {
			return true;
		}

		@Override
		public String getName() {
			return methodName;
		}

		@Override
		public ResourceReference getRequiredResource() {
			String target = argumentType.get();
			return target != null && !target.equals(InstrumentedClass.UNKNOWN_TYPE)? new MessageReference(target) : new UnknownReference(new MessageReference(this.toString()));
		}

		@Override
		public boolean isValidInstrumentation() {
			return argumentType.get() != null;
		}

		@Override
		public String toString() {
			return "method " + getName();
		}

	}

	/**
	 * An field injected with a message queue that allow consumer to pull
	 * messages
	 */
	public static class MessageQueueField extends InjectedField implements RequirerInstrumentation {

		public MessageQueueField(AtomicImplementationDeclaration implementation, String fieldName) {
			super(implementation,fieldName);
		}
		
		@Override
		public boolean acceptMultipleProviders() {
			return true;
		}

		@Override
		public ResourceReference getRequiredResource() {
			return getType();
		}
		
		@Override
		protected ResourceReference generateReference(String type) {
			return  new MessageReference(type);
		}

	}

	/**
	 * An field injected with a service reference (wire or directly the Apam
	 * component)
	 */
	public static class RequiredServiceField extends InjectedField  implements RequirerInstrumentation {

		public RequiredServiceField(AtomicImplementationDeclaration implementation,String fieldName) {
			super(implementation,fieldName);
		}

		@Override
		public boolean acceptMultipleProviders() {
			return isCollection();
		}

		@Override
		public ResourceReference getRequiredResource() {
			return getType();
		}

		@Override
		protected ResourceReference generateReference(String type) {
			return new InterfaceReference(type);
		}

		public boolean isWire() {
			String type = getRequiredResource().getJavaType();

			boolean isApamComponent = 	"fr.imag.adele.apam.Component".equals(type)	||
										"fr.imag.adele.apam.Specification".equals(type)	||
										"fr.imag.adele.apam.Implementation".equals(type)||
										"fr.imag.adele.apam.Instance".equals(type);

			return !isApamComponent;
		}

	}

	/**
	 * An unique identifier for this injection, within the scope of the declaring implementation
	 */
	public abstract String getName();
	
	/**
	 * Whether the instrumentation declaration is valid in the instrumented code
	 */
	public abstract boolean isValidInstrumentation();
	
	/**
	 * Whether this instrumentation can handle multi-valued relations
	 */
	public boolean acceptMultipleProviders();


	/**
	 * The type of the java resource that needs to be provided by the target
	 * component at runtime to perform this instrumentation
	 */
	public ResourceReference getRequiredResource();

}
