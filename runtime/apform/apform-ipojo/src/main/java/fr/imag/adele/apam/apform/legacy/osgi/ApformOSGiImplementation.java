package fr.imag.adele.apam.apform.legacy.osgi;

import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InterfaceReference;

/**
 * This class allow integrating legacy iPojo components in the APAM runtime

 *
 */
public class ApformOSGiImplementation implements ApformImplementation {

	/**
	 * A legacy implementation declaration
	 */
	private static class Declaration extends ImplementationDeclaration {

		protected Declaration(String name) {
			super(name, null);
			
			setInstantiable(false);
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
	 * The prototype instance used to create this implementtaion
	 */
	private final ApformOSGiInstance prototype;
	
	/**
	 * The declaration of this implementation
	 */
	private final ImplementationDeclaration declaration;
	
	public ApformOSGiImplementation(ApformOSGiInstance prototype) {
		this.prototype		= prototype;
		this.declaration	= new Declaration(prototype.getDeclaration().getImplementation().getName());

		for (String providedIntereface : (String[]) prototype.getServiceReference().getProperty(Constants.OBJECTCLASS)) {
			declaration.getProvidedResources().add(new InterfaceReference(providedIntereface));
		}
	}
	
	@Override
	public Bundle getBundle() {
		return prototype.getBundle();
	}
	
	/**
	 * Create a new legacy instance
	 */
	@Override
	public ApformInstance createInstance(Map<String, String> initialProperties) {
		throw new UnsupportedOperationException();
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
	public void setProperty(String attr, String value) {
	}

}
