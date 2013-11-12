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
    },

    SPECIFICATION {
	@Override
	public boolean isAssignableTo(String className) {
	    return className.equals("fr.imag.adele.apam.Specification")
		    || COMPONENT.isAssignableTo(className);
	}
    },

    IMPLEMENTATION {
	@Override
	public boolean isAssignableTo(String className) {
	    return className.equals("fr.imag.adele.apam.Implementation")
		    || COMPONENT.isAssignableTo(className);
	}
    },

    INSTANCE {
	@Override
	public boolean isAssignableTo(String className) {
	    return className.equals("fr.imag.adele.apam.Instance")
		    || COMPONENT.isAssignableTo(className);

	}
    };

    /**
     * Determines if an component of this kind can be assigned to a java field
     * of the specified type
     */
    public abstract boolean isAssignableTo(String className);

}
