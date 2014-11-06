/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.apform.legacy.ipojo;

import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.IPojoFactory;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.osgi.framework.Bundle;

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.references.components.ImplementationReference;
import fr.imag.adele.apam.declarations.references.resources.InterfaceReference;
import fr.imag.adele.apam.impl.BaseApformComponent;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;

/**
 * This class allow integrating legacy iPojo components in the APAM runtime

 *
 */
public class ApformIPojoImplementation extends BaseApformComponent<Implementation,ImplementationDeclaration> implements ApformImplementation {

	/**
	 * A legacy implementation declaration
	 */
	private static class Declaration extends ImplementationDeclaration {

		protected Declaration(String name) {
			super(name, null);
		}

		/**
		 * Clone declaration
		 */
		protected Declaration(Declaration original) {
			super(original);
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
	
	public ApformIPojoImplementation(IPojoFactory factory) {

		super(new Declaration(factory.getName()));		

		/*
		 * Add the list of provided interfaces
		 */
		for (String providedIntereface : factory.getComponentDescription().getprovidedServiceSpecification()) {
			declaration.getProvidedResources().add(new InterfaceReference(providedIntereface));
		}
		
		/*
		 * Add the list of factory properties
		 */
		for(PropertyDescription  property : factory.getComponentDescription().getProperties()) {
			if (property.isImmutable()) {
				declaration.getPropertyDefinitions().add( new PropertyDefinition(declaration.getReference(), property.getName(),"string", null));
				declaration.getProperties().put(property.getName(), property.getValue());
			}
		}
		
		this.factory 		= factory;
	
	}
	
	@Override
	public Bundle getBundle() {
		return factory.getBundleContext().getBundle();
	}
	
	/**
	 * Create a new legacy instance
	 */
	@Override
	public ApformInstance createInstance(Map<String, String> initialProperties) {
		
		try {
			Properties configuration = new Properties();
			if (initialProperties != null)
				configuration.putAll(initialProperties);
			
			ComponentInstance ipojoInstance = factory.createComponentInstance(configuration);
			return new ApformIpojoInstance(ipojoInstance);
			
		} catch (Exception cause) {
			throw new IllegalArgumentException(cause);
		}
	}

	@Override
	public ApformInstance addDiscoveredInstance(Map<String, Object> configuration) throws InvalidConfiguration, UnsupportedOperationException {
		throw new UnsupportedOperationException("iPOJO instances are automatically discovered by APAM");
	}
	
	@Override
	public void setProperty(String attr, String value) {
		// TODO see if we can reconfigure factory publication
	}

}
