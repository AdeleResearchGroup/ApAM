package fr.imag.adele.apam.apform.legacy.osgi;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.SpecificationReference;

/**
 * This class allow integrating legacy iPojo components in the APAM runtime

 *
 */
public class ApformOSGiImplementation implements ApformImplementation {

	/**
	 * A legacy implementation declaration
	 */
	public final static class Declaration extends ImplementationDeclaration {

		protected Declaration(String name, SpecificationReference specification) {
			super(name, specification);
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

		protected Reference(String name) {
			super(name);
		}
		
	}
	
	/**
	 * The prototype instance used to create this implementation
	 */
	private final ApformOSGiInstance prototype;
	
	/**
	 * The declaration of this implementation
	 */
	private final ImplementationDeclaration declaration;
	
	/**
	 * Creates a new implementation reference from aprototype instance
	 */
	public ApformOSGiImplementation(ApformOSGiInstance prototype) {
		
		this.prototype		= prototype;
		
		/*
		 * find a matching specification
		 */
		
		SpecificationReference  perfectMatch		= null;
		Set<SpecificationReference> partialMatch	= new HashSet<SpecificationReference>();
		
		for (Specification candidate : CST.componentBroker.getSpecs()) {
			Set<InterfaceReference> candidateInterfaces = candidate.getDeclaration().getProvidedResources(InterfaceReference.class);
			
			if (prototype.getProvidedResources().equals(candidateInterfaces))
				perfectMatch = candidate.getApformSpec().getDeclaration().getReference();
			else if (prototype.getProvidedResources().containsAll(candidateInterfaces))
				partialMatch.add(candidate.getApformSpec().getDeclaration().getReference());
		}
		
		SpecificationReference specification = perfectMatch != null ? perfectMatch : partialMatch.size() == 1 ? partialMatch.iterator().next() : null; 

		/*
		 * create declaration dynamically
		 */
		this.declaration	= new Declaration(prototype.getDeclaration().getImplementation().getName(),specification);
   		declaration.getProvidedResources().addAll(prototype.getProvidedResources());
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
