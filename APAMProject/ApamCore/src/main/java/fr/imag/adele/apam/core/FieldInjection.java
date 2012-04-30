package fr.imag.adele.apam.core;

/**
 * This class declares a field in a java implementation that must be injected with a resource by the runtime
 * 
 * @author vega
 * 
 */
public class FieldInjection {

	/**
	 * The atomic implementation declaring this injection
	 */
	protected final AtomicImplementationDeclaration implementation;
	
	/**
	 * The name of the field that must be injected
	 */
	protected final String fieldName;

    public FieldInjection(AtomicImplementationDeclaration implementation, String fieldName) {

        assert implementation != null;
        assert fieldName != null;
        
        this.implementation = implementation;
        this.fieldName		= fieldName;
    }

	/**
	 * The component declaring this injection
	 */
	public AtomicImplementationDeclaration getImplementation() {
	    return implementation;
	}

	/**
	 * The name of the field to inject
	 */
	public String getFieldName() {
	    return fieldName;
	}

	/**
	 * The type of the resource that will be injected in the field
	 */
	public ResourceReference getResource() {
	    try {
			return implementation.getInstrumentation().getFieldType(fieldName);
		} catch (NoSuchFieldException e) {
			return ResourceReference.UNDEFINED;
		}
	}

	/**
	 * whether this field is a collection or not
	 */
	public boolean isCollection() {
		try {
			return implementation.getInstrumentation().isCollectionField(fieldName);
		} catch (NoSuchFieldException e) {
			return false;
		}
	}

	@Override
	public String toString() {
	    return "Field name: " + fieldName + ". Type: " + getResource().getJavaType() +(isCollection()?"[]":"");
	}

}