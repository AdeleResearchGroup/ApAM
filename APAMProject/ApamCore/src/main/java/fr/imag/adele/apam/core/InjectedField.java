package fr.imag.adele.apam.core;

/**
 * This class declares a field in a java implementation that must me injected with a resource by the runtime 
 * 
 * @author vega
 *
 */
public abstract class InjectedField {
	
	/**
	 * The implementation declaring this field injection
	 */
	private final JavaImplementationDeclaration implementation;
	
	/**
	 * The type of the resource that must be injected
	 */
	private final ResourceReference 	resource;
	
	/**
	 * The name of the filed that must be injected
	 */
	private final String fieldName;
	

	protected InjectedField(JavaImplementationDeclaration implementation, String fieldName, ResourceReference resource) {
		
		assert implementation != null;
		assert fieldName != null;
		assert resource != null;
		
		this.implementation = implementation;
		this.fieldName		= fieldName;
		this.resource		= resource;
		
	}
	
	/**
	 * The implementation declaring this field injection
	 */
	public JavaImplementationDeclaration getImplementation() {
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
	 * The declaration of a field that must be injected when a dependency is resolved
	 * 
	 */
	public static class RequiredInterface extends InjectedField {

		/**
		 * The dependency that must be resolved to get the provider of the required resource
		 */
		private final DependencyDeclaration dependency;

		protected RequiredInterface(JavaImplementationDeclaration implementation, String fieldName, DependencyDeclaration dependency, InterfaceReference resource) {
			super(implementation,fieldName,resource);

			assert dependency != null;
			assert implementation.getDependencies().contains(dependency);
			assert dependency.getResource() instanceof SpecificationReference || dependency.getResource().equals(resource);

			this.dependency = dependency;
		}

		/**
		 * The dependency that needs to be resolved to inject this field
		 */
		public DependencyDeclaration getDependency() {
			return dependency;
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

		/**
		 * The dependency that must be resolved to get the provider of the required resource
		 */
		private final DependencyDeclaration dependency;

		protected PullConsumer(JavaImplementationDeclaration implementation, String fieldName, DependencyDeclaration dependency, MessageReference resource) {
			super(implementation,fieldName,resource);

			assert dependency != null;
			assert implementation.getDependencies().contains(dependency);
			assert dependency.getResource() instanceof SpecificationReference || dependency.getResource().equals(resource);

			this.dependency = dependency;
		}

		/**
		 * The dependency that needs to be resolved to inject this field
		 */
		public DependencyDeclaration getDependency() {
			return dependency;
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


		public PushProducer(JavaImplementationDeclaration implementation, String fieldName, MessageReference resource) {
			
			super(implementation,fieldName,resource);

			assert implementation.getProvidedResources().contains(resource);
		}

		@Override
		public final MessageReference getResource() {
			return (MessageReference)super.getResource();
		}

	}	
}
