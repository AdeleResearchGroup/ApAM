package fr.imag.adele.apam.core;

/**
 * This class declares a method in a java implementation that must be called back with a resource by the runtime
 * 
 * @author vega
 * 
 */
public class CallbackInjection {

	/**
	 * The atomic implementation declaring this injection
	 */
	protected final AtomicImplementationDeclaration implementation;
	
	/**
	 * The name of the method that must be called back
	 */
	protected final String methodName;

    public CallbackInjection(AtomicImplementationDeclaration implementation, String methodName) {

        assert implementation != null;
        assert methodName != null;
        
        this.implementation = implementation;
        this.methodName		= methodName;
    }

	/**
	 * The component declaring this injection
	 */
	public AtomicImplementationDeclaration getImplementation() {
	    return implementation;
	}

	/**
	 * The name of the method that will be called back
	 */
	public String getMethodName() {
	    return methodName;
	}

	/**
	 * The type of the resource that will be injected in the method
	 */
	public ResourceReference getResource() {
	    try {
			return implementation.getInstrumentation().getCallbackType(methodName);
		} catch (NoSuchMethodException e) {
			return ResourceReference.UNDEFINED;
		}
	}

	@Override
	public String toString() {
	    return "Method name: " + methodName + ". Type: " + getResource().getJavaType();
	}

}