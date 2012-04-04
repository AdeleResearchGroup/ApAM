package fr.imag.adele.apam.core;

/**
 * This class declares a field in a java implementation that must be injected with a resource by the runtime
 * 
 * @author vega
 * 
 */
public class DependencyInjection {

    /**
     * The atomic implementation declaring this injection
     */
    private final AtomicImplementationDeclaration implementation;

    /**
     * The name of the field that must be injected
     */
    private final String 				fieldName;



    public DependencyInjection(AtomicImplementationDeclaration implementation, String fieldName) {

        assert implementation != null;
        assert fieldName != null;
        
        // bidirectional reference to declaration
        this.implementation = implementation;
        this.implementation.getDependencyInjections().add(this);

        this.fieldName			= fieldName;
    }

    /**
     * The component declaring this injection
     */
    public AtomicImplementationDeclaration getImplementation() {
        return implementation;
    }

    /**
     * The dependency that must be resolved to get the injected resource.
     */
    private DependencyDeclaration		dependency;

    /**
     * Sets the dependency that will be injected in this field
     */
    public void setDependency(DependencyDeclaration dependency) {

    	assert dependency.getComponent() == implementation;
        
        // bidirectional reference to dependency
        this.dependency = dependency;
        this.dependency.getInjections().add(this);
    }
    
    
    /**
     * The dependency that must be resolved to inject this field
     */
    public DependencyDeclaration getDependency() {
        return dependency;
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
        return implementation.getInstrumentation().getType(fieldName);
    }
    
    /**
     * whether this field is a collection or not
     */
    public boolean isCollection() {
    	return implementation.getInstrumentation().isCollection(fieldName);
    }

    @Override
    public String toString() {
        return "Field name: " + fieldName + ". Type: " + getResource().getJavaType() +(isCollection()?"[]":"");
    }

}
