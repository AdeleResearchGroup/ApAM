package fr.imag.adele.apam.declarations;

import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.ImplementationReference;
import fr.imag.adele.apam.declarations.references.components.InstanceReference;
import fr.imag.adele.apam.declarations.references.components.SpecificationReference;

/**
 * The different levels of component abstractions.
 * 
 * NOTE Notice that kinds are specified in order of increasing abstraction, to be able to 
 * use the natural order of enumerations (that is based on declaration order) to compare 
 * abstraction levels
 * 
 * @author vega
 * 
 */
public enum ComponentKind implements Comparable<ComponentKind> {

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

	COMPONENT {
		@Override
		public boolean isAssignableTo(String className) {
			return className.equals("fr.imag.adele.apam.Component");
		}

		@Override
		public ComponentReference<?> createReference(String name) {
			return new ComponentReference<ComponentDeclaration>(name);
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
	
	/**
	 * Whether this kind is more abstract than the specified one
	 */
	public boolean isMoreAbstractThan(ComponentKind that) {
		return this.compareTo(that) > 0;
	}
}
