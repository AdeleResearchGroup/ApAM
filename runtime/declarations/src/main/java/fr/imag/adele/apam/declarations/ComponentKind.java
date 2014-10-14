package fr.imag.adele.apam.declarations;

/**
 * The different levels of component abstractions.
 * 
 * @author vega
 * 
 */
public enum ComponentKind {

	COMPONENT {
		@Override
		public boolean isAssignableTo(String className) {
			return className.equals("fr.imag.adele.apam.Component");
		}

		@Override
		public ComponentReference<?> createReference(String name) {
			return new ComponentReference<ComponentDeclaration>(name);
		}
	},

	SPECIFICATION {
		@Override
		public boolean isAssignableTo(String className) {
			return className.equals("fr.imag.adele.apam.Specification")
					|| COMPONENT.isAssignableTo(className);
		}
		
		@Override
		public SpecificationReference createReference(String name) {
			return new SpecificationReference(name);
		}
	},

	IMPLEMENTATION {
		@Override
		public boolean isAssignableTo(String className) {
			return className.equals("fr.imag.adele.apam.Implementation")
					|| COMPONENT.isAssignableTo(className);
		}
		
		@Override
		public ImplementationReference<?> createReference(String name) {
			return new ImplementationReference<ImplementationDeclaration>(name);
		}
	},

	INSTANCE {
		@Override
		public boolean isAssignableTo(String className) {
			return className.equals("fr.imag.adele.apam.Instance")
					|| COMPONENT.isAssignableTo(className);

		}
		
		@Override
		public InstanceReference createReference(String name) {
			return new InstanceReference(name);
		}
	};

	/**
	 * Determines if an component of this kind can be assigned to a java field
	 * of the specified type
	 */
	public abstract boolean isAssignableTo(String className);
	
	/**
	 * Creates a reference to a component of this kind
	 */
	public abstract ComponentReference<? extends ComponentDeclaration> createReference(String name);
}
