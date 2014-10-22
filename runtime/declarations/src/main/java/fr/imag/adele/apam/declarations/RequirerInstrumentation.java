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
public abstract class RequirerInstrumentation extends Instrumentation {

	/**
	 * An injected field declaration
	 */
	public static abstract class InjectedField extends RequirerInstrumentation {

		protected final String field;

		private final Lazy<String> fieldType = new Lazy<String>() {
			@Override
			protected String evaluate(CodeReflection reflection) {
				try {
					return reflection.getFieldType(field);
				} catch (NoSuchFieldException e) {
					return null;
				}
			};

		};

		private final Lazy<Boolean> fieldMultiplicity = new Lazy<Boolean>() {
			@Override
			protected Boolean evaluate(CodeReflection reflection) {
				try {
					return reflection.isCollectionField(field);
				} catch (NoSuchFieldException e) {
					return false;
				}
			};

		};

		protected InjectedField(AtomicImplementationDeclaration implementation,	String field) {
			super(implementation);
			this.field = field;
		}

		@Override
		public boolean acceptMultipleProviders() {
			return fieldMultiplicity.get();
		}

		@Override
		public String getName() {
			return field;
		}

		@Override
		public ResourceReference getRequiredResource() {
			String target = fieldType.get();
			return target != null && !target.equals(CodeReflection.UNKNOWN_TYPE)? generateReference(target) : new UnknownReference(generateReference(this.toString()));
		}

		/**
		 * Generates a new reference of the appropriate class for the specified, required resource 
		 */
		protected abstract ResourceReference generateReference(String type);
		
		@Override
		public boolean isValidInstrumentation() {
			return fieldType.get() != null;
		}

		@Override
		public String toString() {
			return "field " + getName();
		}

	}

	/**
	 * A callback to push messages to consumer
	 */
	public static class MessageConsumerCallback extends RequirerInstrumentation {

		private final String methodName;

		private final Lazy<String> argumentType = new Lazy<String>() {
			@Override
			protected String evaluate(CodeReflection reflection) {
				try {
					return reflection.getMethodParameterType(methodName, true);
				} catch (NoSuchMethodException e) {
					return null;
				}
			};

		};

		public MessageConsumerCallback(AtomicImplementationDeclaration implementation, String methodName) {
			super(implementation);
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
			return target != null && !target.equals(CodeReflection.UNKNOWN_TYPE)? new MessageReference(target) : new UnknownReference(new MessageReference(this.toString()));
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
	public static class MessageQueueField extends InjectedField {

		public MessageQueueField(AtomicImplementationDeclaration implementation, String fieldName) {
			super(implementation,fieldName);
		}
		
		@Override
		protected ResourceReference generateReference(String type) {
			return  new MessageReference(type);
		}

		@Override
		public boolean acceptMultipleProviders() {
			return true;
		}
	}

	/**
	 * An field injected with a service reference (wire or directly the Apam
	 * component)
	 */
	public static class RequiredServiceField extends InjectedField {

		public RequiredServiceField(AtomicImplementationDeclaration implementation, String fieldName) {
			super(implementation,fieldName);
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
	 * The relation used to satisfy the requirement.
	 */
	protected RelationDeclaration relation;

	protected RequirerInstrumentation(AtomicImplementationDeclaration implementation) {
		super(implementation);
		this.implementation.getRequirerInstrumentation().add(this);
	}

	/**
	 * Whether this instrumentation can handle multi-valued relations
	 */
	public abstract boolean acceptMultipleProviders();

	/**
	 * An unique identifier for this injection, within the scope of the
	 * declaring implementation and relation
	 */
	public abstract String getName();

	/**
	 * Get the relation that will be used to satisfy the requirement.
	 */
	public RelationDeclaration getRelation() {
		return relation;
	}

	/**
	 * The type of the java resource that needs to be provided by the target
	 * component at runtime to perform this instrumentation
	 */
	public abstract ResourceReference getRequiredResource();

	/**
	 * Sets the relation that will be used to satisfy the requirement.
	 */
	public void setRelation(RelationDeclaration relation) {

		assert relation.getComponent() == implementation.getReference();

		// bidirectional reference to relation
		this.relation = relation;
		this.relation.getInstrumentations().add(this);
	}

}
