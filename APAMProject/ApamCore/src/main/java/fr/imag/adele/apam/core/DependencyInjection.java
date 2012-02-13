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
     * The dependency that must be resolved to get the provider of the required resource.
     */
    private final DependencyDeclaration           dependency;

    /**
     * The name of the field that must be injected
     */
    private final String fieldName;

    /**
     * The type of the resource that must be injected
     */
    private final ResourceReference 	resource;
    
    public DependencyInjection(AtomicImplementationDeclaration implementation, DependencyDeclaration dependency, String fieldName, ResourceReference resource) {

    	assert implementation != null;
    	assert dependency != null;
        assert fieldName != null;
        assert resource != null;

        // bidirectional reference to declaration
        this.implementation = implementation;
        this.implementation.getDependencyInjections().add(this);
        
        this.fieldName		= fieldName;
        this.resource		= resource;
        
        // bidirectional reference to dependency
        assert dependency.getComponent() == implementation;
        this.dependency 	= dependency;
        this.dependency.getInjections().add(this);
        
    }

    /**
     * The component declaring this injection
     */
    public AtomicImplementationDeclaration getImplementation() {
    	return implementation;
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
     * The name of the resource that will be injected in the field
     */
    public ResourceReference getResource() {
        return resource;
    }

 }
