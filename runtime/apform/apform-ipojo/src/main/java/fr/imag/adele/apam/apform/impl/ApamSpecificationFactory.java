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
package fr.imag.adele.apam.apform.impl;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;

/**
 * This class represents an APAM specification at the iPOJO level.
 * 
 * It is actually implemented as an abstract factory that can not be 
 * instantiated.
 * 
 * 
 * @author vega
 *
 */
public class ApamSpecificationFactory extends ApamComponentFactory {

    /**
     * Build a new factory with the specified metadata
     *
     * @param context
     * @param metadata
     * @throws ConfigurationException
     */
    public ApamSpecificationFactory(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);

    }

	@Override
	protected ApformComponent createApform() {
		return this.new Apform();
	}
	
    public ApformSpecification getApform() {
    	return (ApformSpecification) this.apform;
    }
    
    @Override
    public boolean isInstantiable() {
        return false;
    }

    @Override
    public boolean hasInstrumentedCode() {
        return false;
    }

    @Override
    public String getClassName() {
        return this.declaration.getName();
    }


    @Override
    public ApamInstanceManager createApamInstance(IPojoContext context, HandlerManager[] handlers) {
        throw new UnsupportedOperationException("APAM specification is not instantiable");
    }

    @Override
    protected void bindToApam(Apam apam) {
        Apform2Apam.newSpecification(getApform());
    }

	/**
	 * This class represents the base functionality of Apform mediation object between APAM and a specification factory
	 */
	private class Apform extends ApamComponentFactory.Apform<Specification,SpecificationDeclaration> implements ApformSpecification {
		
		public Apform() {
			super();
		}
	}

}