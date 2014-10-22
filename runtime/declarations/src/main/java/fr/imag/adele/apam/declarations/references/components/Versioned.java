package fr.imag.adele.apam.declarations.references.components;

import fr.imag.adele.apam.declarations.ComponentDeclaration;


/**
 * This class is used to represent a range of versions of the referenced component
 */
public class Versioned<C extends ComponentDeclaration> {
	
	private final ComponentReference<C>	component;
	private final String 				range;
	
	/**
	 * Get a reference to any version of the specified component
	 */
	public static final <C extends ComponentDeclaration, R extends ComponentReference<C>> Versioned<C> any(R component) {
		return new Versioned<C>(component,null);
	}

	/**
	 * Get a reference to some version of the specified component in the specified range
	 */
	public static final  <C extends ComponentDeclaration, R extends ComponentReference<C>> Versioned<C> range(R component,String range) {
		return new Versioned<C>(component,range);
	}

	private Versioned(ComponentReference<C> component, String range) {
		this.component	= component;
		this.range 		= range;
	}
	
	/**
	 * The referenced component
	 */
	public ComponentReference<C> getComponent() {
		return component;
	}

	/**
	 * The name of the referenced component
	 */
	public String getName() {
		return component.getName();
	}

	/**
	 * The range of versions
	 */
	public String getRange() {
		return range;
	}
	
	@Override
	public int hashCode() {
		return component.hashCode()+ (range != null ? range.hashCode() : 0);
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof Versioned)) {
			return false;
		}

		Versioned<?> that = (Versioned<?>) object;
		
		return this.component.equals(that.component) &&
			   this.range != null ? that.range != null && this.range.equals(that.range) : that.range == null;
	}
} 
