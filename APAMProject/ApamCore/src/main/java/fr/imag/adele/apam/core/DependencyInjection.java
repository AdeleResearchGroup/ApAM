package fr.imag.adele.apam.core;

/**
 * This class declares a field or method in a java implementation that must be injected with a resource by the runtime
 * when resolving dependencies.
 * 
 * @author vega
 * 
 */
public abstract class DependencyInjection  {

	/**
	 * The implementation associated to this injection
	 */
	protected final AtomicImplementationDeclaration implementation;

    /**
     * The dependency that must be resolved to get the injected resource.
     */
	protected DependencyDeclaration		dependency;

    protected DependencyInjection(AtomicImplementationDeclaration implementation) {

    	// bidirectional reference to declaration
    	this.implementation = implementation;
        this.implementation.getDependencyInjections().add(this);
    }

    /**
     * Sets the dependency that will be injected
     */
    public void setDependency(DependencyDeclaration dependency) {

    	assert dependency.getComponent() == implementation;
        
        // bidirectional reference to dependency
        this.dependency = dependency;
        this.dependency.getInjections().add(this);
    }
    
    
    /**
     * The dependency that must be resolved
     */
    public DependencyDeclaration getDependency() {
        return dependency;
    }
    
    /**
     * The type of the resource that will be injected
     */
    public abstract ResourceReference getResource();
    
    /**
     * An unique identifier for this injection, within the scope of the declaring implementation
     * and dependency
     */
    public abstract String getName();
    
    /**
     * Whether this injection accepts collection dependencies
     */
    public abstract boolean isCollection();
    
    /**
     * Whether the injection definition is valid in the instrumented code
     */
    public abstract boolean isValidInstrumentation();
    
    /**
     * An injected field declaration
     */
    public static class Field extends DependencyInjection {
    	
    	private final String fieldName;
    	
    	public Field(AtomicImplementationDeclaration implementation, String fieldName) {
    		super(implementation);
    		this.fieldName = fieldName;
    	}

    	/**
    	 * The name of the field to inject
    	 */
    	@Override
    	public String getName() {
    	    return fieldName;
    	}

    	@Override
        public boolean isValidInstrumentation() {
    	    try {
    			implementation.getInstrumentation().getFieldType(fieldName);
    			return true;
    		} catch (NoSuchFieldException e) {
    			return false;
    		}
        }
    	
    	/**
    	 * The type of the resource that will be injected in the field
    	 */
    	@Override
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
    	@Override
    	public boolean isCollection() {
    		try {
    			return implementation.getInstrumentation().isCollectionField(fieldName);
    		} catch (NoSuchFieldException e) {
    			return false;
    		}
    	}
    	
    	@Override
    	public String toString() {
    		return "field "+getName();
    	}
		
    }

    /**
     * An message callback declaration
     */
    
    public static class Callback extends DependencyInjection {
    	
    	private final String methodName;
    	
    	public Callback(AtomicImplementationDeclaration implementation, String methodName) {
    		super(implementation);
    		this.methodName = methodName;
    	}

    	/**
    	 * The name of the field to inject
    	 */
    	public String getName() {
    	    return methodName;
    	}

        public boolean isValidInstrumentation() {
    	    try {
    	    	implementation.getInstrumentation().getCallbackType(methodName);
    			return true;
    		} catch (NoSuchMethodException e) {
    			return false;
    		}
        }
     	
    	/**
    	 * The type of the resource that will be injected in the field
    	 */
    	public ResourceReference getResource() {
    	    try {
    			return implementation.getInstrumentation().getCallbackType(methodName);
    		} catch (NoSuchMethodException e) {
    			return ResourceReference.UNDEFINED;
    		}
    	}

    	/**
    	 * whether this field is a collection or not
    	 */
    	public boolean isCollection() {
    		return false;
    	}
    	
    	@Override
    	public String toString() {
    		return "method "+getName();
    	}
    	
		
    }

}
