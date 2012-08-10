package fr.imag.adele.apam.apformipojo.legacy;

import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.IPojoFactory;

import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.InterfaceReference;

/**
 * This class allow integrating legacy iPojo components in the APAM runtime

 *
 */
public class ApformIPojoLegacyImplementation implements ApformImplementation {

	/**
	 * A legacy implementation declaration
	 */
	private static class Declaration extends ImplementationDeclaration {

		protected Declaration(String name) {
			super(name, null);
		}

		@Override
		protected ImplementationReference<?> generateReference() {
			return new Reference(getName());
		}
		
	}
	/**
	 * A reference to a legacy declaration
	 *
	 */
	public static class Reference extends ImplementationReference<Declaration> {

		public Reference(String name) {
			super(name);
		}
		
	}
	/**
	 * The associated iPojo factory
	 */
	private final IPojoFactory factory;
	
	/**
	 * The declaration of this implementation
	 */
	private final ImplementationDeclaration declaration;
	
	public ApformIPojoLegacyImplementation(IPojoFactory factory) {
		this.factory 		= factory;
		this.declaration	= new Declaration(factory.getName());
		for (String providedIntereface : factory.getComponentDescription().getprovidedServiceSpecification()) {
			declaration.getProvidedResources().add(new InterfaceReference(providedIntereface));
		}
	}
	
	/**
	 * Create a new legacy instance
	 */
	@Override
	public ApformInstance createInstance(Map<String, Object> initialproperties) {
		
		try {
			Properties configuration = new Properties();
			if (initialproperties != null)
				configuration.putAll(initialproperties);
			
			ComponentInstance ipojoInstance = factory.createComponentInstance(configuration);
			return new ApformIpojoLegacyInstance(ipojoInstance);
			
		} catch (Exception cause) {
			throw new IllegalArgumentException(cause);
		}
	}

	@Override
	public ApformSpecification getSpecification() {
		return null;
	}


	@Override
	public ImplementationDeclaration getDeclaration() {
		return declaration;
	}

	@Override
	public void setProperty(String attr, Object value) {
		// TODO Auto-generated method stub
		
	}

}
