package fr.imag.adele.apam.core;

/**
 * This class declares a method in a java implementation that must be called by
 * the runtime.
 * 
 * The method has a single parameter of type "resource"
 * 
 * @author vega
 * 
 */

public class MethodCallback {

    /**
     * The implementation declaring this field injection
     */
    private final AtomicImplementationDeclaration implementation;

    /**
     * The type of the resource that must be injected
     */
    private final ResourceReference resource;

    /**
     * The name of the method that must be called
     */
    private final String methodName;

    protected MethodCallback(AtomicImplementationDeclaration implementation,
            String methodName, ResourceReference resource) {

        assert implementation != null;
        assert methodName != null;
        assert resource != null;

        this.implementation = implementation;
        this.methodName = methodName;
        this.resource = resource;

    }

    /**
     * The implementation declaring this method callback
     */
    public AtomicImplementationDeclaration getImplementation() {
        return implementation;
    }

    /**
     * The name of the method to call
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * The name of the resource that will be passed in the callback
     */
    public ResourceReference getResource() {
        return resource;
    }

    /**
     * The declaration of a method that must be called to push messages to a consumer 
     * 
     */
    public static class PushConsumer extends MethodCallback {

        /**
         * The dependency that must be resolved to get the producer of the required message
         */
        private final DependencyDeclaration dependency;

        protected PushConsumer(AtomicImplementationDeclaration implementation, String methodName, DependencyDeclaration dependency, MessageReference resource) {
            super(implementation,methodName,resource);

            assert dependency != null;
            assert implementation.getDependencies().contains(dependency);
            assert (dependency.getResource() instanceof SpecificationReference) || dependency.getResource().equals(resource);


            this.dependency = dependency;
        }

        /**
         * The dependency that must be resolved to get the producer of the required message
         */
        public DependencyDeclaration getDependency() {
            return dependency;
        }

        @Override
        public final MessageReference getResource() {
            return (MessageReference)super.getResource();
        }
    }


}
