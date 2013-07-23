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
 * the requiring end of a dependency.
 * 
 * @author vega
 * 
 */
public abstract class RequirerInstrumentation extends Instrumentation {

	/**
	 * The relation used to satisfy the requirement.
	 */
	protected RelationDeclaration relation;

	protected RequirerInstrumentation(AtomicImplementationDeclaration implementation) {
		super(implementation);
		this.implementation.getRequirerInstrumentation().add(this);
	}

	/**
	 * Sets the relation that will be used to satisfy the requirement.
	 */
	public void setRelation(RelationDeclaration relation) {

		assert relation.getComponent() == implementation.getReference();

		// bidirectional reference to relation
		this.relation = relation;
		this.relation.getInstrumentations().add(this);
	}

	/**
	 * Get the relation that will be used to satisfy the requirement.
	 */
	public RelationDeclaration getRelation() {
		return relation;
	}

	/**
	 * An unique identifier for this injection, within the scope of the
	 * declaring implementation and relation
	 */
	public abstract String getName();

	/**
	 * The type of the java resource that needs to be provided by the
	 * target component at runtime to perform this instrumentation
	 */
	public abstract ResourceReference getRequiredResource();

	/**
	 * Whether this instrumentation can handle multi-valued relations
	 */
	public abstract boolean acceptMultipleProviders();

	/**
	 * An injected field declaration
	 */
	public static abstract class InjectedField extends RequirerInstrumentation {
		
		protected final ResourceReference field;
		
		
		private final Lazy<ResourceReference> fieldType = new Lazy<ResourceReference>() {
			protected ResourceReference evaluate(CodeReflection reflection) {
				try {
					return reflection.getFieldType(field.getName());
				} catch (NoSuchFieldException e) {
					return null;
				}
			};

		};
		
		private final Lazy<Boolean> fieldMultiplicity = new Lazy<Boolean>() {
			protected Boolean evaluate(CodeReflection reflection) {
				try {
					return reflection.isCollectionField(field.getName());
				} catch (NoSuchFieldException e) {
					return false;
				}
			};

		};
		
		protected InjectedField(AtomicImplementationDeclaration implementation, ResourceReference field) {
			super(implementation);
			this.field = field;
		}
		
		@Override
		public String getName() {
			return field.getName();
		}
		
		@Override
		public boolean isValidInstrumentation() {
			return fieldType.get() != null;
		}
		
		@Override
		public ResourceReference getRequiredResource() {
			ResourceReference target = fieldType.get();
			return target != null ? target :  new UndefinedReference(field);
		}

		@Override
		public boolean acceptMultipleProviders() {
			return fieldMultiplicity.get();
		}

		@Override
		public String toString() {
			return "field " + getName();
		}
		
	}

	/**
	 * An field injected with a service reference (wire or directly the Apam component)
	 */
	public static class RequiredServiceField extends InjectedField {

		public RequiredServiceField(AtomicImplementationDeclaration implementation, String fieldName) {
			super(implementation, new InterfaceReference(fieldName));
		}

		public boolean isWire() {
			String type = getRequiredResource().getJavaType();

			boolean isApamComponent = "fr.imag.adele.apam.Component".equals(type)
					|| "fr.imag.adele.apam.Specification".equals(type)
					|| "fr.imag.adele.apam.Implementation".equals(type)
					|| "fr.imag.adele.apam.Instance".equals(type);

			return !isApamComponent;
		}

	}

	/**
	 * An field injected with a message queue that allow consumer to pull messages
	 */
	public static class MessageQueueField extends InjectedField {

		public MessageQueueField(AtomicImplementationDeclaration implementation, String fieldName) {
			super(implementation, new MessageReference(fieldName));
		}

		@Override
		public boolean acceptMultipleProviders() {
			return true;
		}
	}


	/**
	 * A callback to push messages to consumer
	 */
	public static class MessageConsumerCallback extends RequirerInstrumentation {

		private final String methodName;

		private final Lazy<MessageReference> argumentType = new Lazy<MessageReference>() {
			protected MessageReference evaluate(CodeReflection reflection) {
				try {
					return new MessageReference(reflection.getMethodArgumentType(methodName,true));
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
		public String getName() {
			return methodName;
		}

		@Override
		public boolean isValidInstrumentation() {
			return argumentType.get() != null;
		}
		
		@Override
		public ResourceReference getRequiredResource() {
			MessageReference target = argumentType.get();
			return target != null ? target :  new MessageReference(methodName);
		}

		@Override
		public boolean acceptMultipleProviders() {
			return true;
		}

		@Override
		public String toString() {
			return "method " + getName();
		}

	}

}
