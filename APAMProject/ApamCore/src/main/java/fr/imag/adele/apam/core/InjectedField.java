package fr.imag.adele.apam.core;

/**
 * This class declares a field in a java implementation that must be injected with a resource by the runtime
 * 
 * @author vega
 * 
 */
public abstract class InjectedField {

    /**
     * Allows to know what is the type of resource dependency, in order to synthesize.
     * 
     */
    public enum InjectedType {
        INTERFACE, PUSHPRODUCER, PULLCONSUMER
    }

    protected final InjectedType                  injectedType;
    /**
     * The implementation declaring this field injection
     */
    private final AtomicImplementationDeclaration implementation;

    /**
     * The type of the resource that must be injected
     */
    private final ResourceReference 	resource;

    /**
     * The name of the field that must be injected
     */
    private final String fieldName;

    /**
     * The dependency that must be resolved to get the provider of the required resource. Not for pull.
     */
    private final DependencyDeclaration           dependency;


    protected InjectedField(AtomicImplementationDeclaration implementation, String fieldName,
            ResourceReference resource, DependencyDeclaration dependency, InjectedType injectedType) {

        assert implementation != null;
        assert fieldName != null;
        assert resource != null;

        this.implementation = implementation;
        this.fieldName		= fieldName;
        this.resource		= resource;
        this.dependency = dependency;
        this.injectedType = injectedType;
    }

    /**
     * The implementation declaring this field injection
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
     * The name of the resource that will be injected in the field
     */
    public ResourceReference getResource() {
        return resource;
    }

    /**
     * The dependency that needs to be resolved to inject this field
     */
    public DependencyDeclaration getDependency() {
        assert injectedType != InjectedType.PUSHPRODUCER;
        return dependency;
    }

    /**
     * The declaration of a field that must be injected when a dependency is resolved
     * 
     */
    public static class RequiredInterface extends InjectedField {

        protected RequiredInterface(AtomicImplementationDeclaration implementation, String fieldName, DependencyDeclaration dependency, InterfaceReference resource) {
            super(implementation, fieldName, resource, dependency, InjectedType.INTERFACE);

            assert dependency != null;
            assert implementation.getDependencies().contains(dependency);
            assert (dependency.getResource().isSpecificationReference()) || dependency.getResource().equals(resource);
        }

        /**
         * The dependency that needs to be resolved to inject this field
         */
        @Override
        public DependencyDeclaration getDependency() {
            return super.getDependency();
        }

        @Override
        public final InterfaceReference getResource() {
            return (InterfaceReference)super.getResource();
        }

    }

    /**
     * The declaration of a field that must be injected to allow a consumer to pull its messages
     * 
     */
    public static class PullConsumer extends InjectedField {

        //        /**
        //         * The dependency that must be resolved to get the provider of the required resource
        //         */
        //        private final DependencyDeclaration dependency;

        protected PullConsumer(AtomicImplementationDeclaration implementation, String fieldName, DependencyDeclaration dependency, MessageReference resource) {
            super(implementation, fieldName, resource, dependency, InjectedType.PULLCONSUMER);

            assert dependency != null;
            assert implementation.getDependencies().contains(dependency);
            assert (dependency.getResource() instanceof SpecificationReference) || dependency.getResource().equals(resource);
        }

        /**
         * The dependency that needs to be resolved to inject this field
         */
        @Override
        public DependencyDeclaration getDependency() {
            return super.getDependency();
        }

        @Override
        public final MessageReference getResource() {
            return (MessageReference)super.getResource();
        }
    }

    /**
     * The declaration of a field that must be injected to allow a producer to push its messages
     * 
     */
    public static class PushProducer extends InjectedField {


        public PushProducer(AtomicImplementationDeclaration implementation, String fieldName, MessageReference resource) {

            super(implementation, fieldName, resource, null, InjectedType.PUSHPRODUCER);

            assert implementation.getProvidedResources().contains(resource);
        }

        @Override
        public final MessageReference getResource() {
            return (MessageReference)super.getResource();
        }

    }	
}
