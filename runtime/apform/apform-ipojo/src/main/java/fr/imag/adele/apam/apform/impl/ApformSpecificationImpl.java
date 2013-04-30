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
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;

public class ApformSpecificationImpl extends ApformComponentImpl implements ApformSpecification {

    /**
     * Build a new factory with the specified metadata
     *
     * @param context
     * @param metadata
     * @throws ConfigurationException
     */
    public ApformSpecificationImpl(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);

    }

    @Override
    public SpecificationDeclaration getDeclaration() {
        return (SpecificationDeclaration) super.getDeclaration();
    }

    @Override
    public boolean hasInstrumentedCode() {
        return false;
    }

    /**
     * Gets the class name.
     *
     * @return the class name.
     * @see org.apache.felix.ipojo.IPojoFactory#getClassName()
     */
    @Override
    public String getClassName() {
        return this.getDeclaration().getName();
    }

    @Override
    public boolean isInstantiable() {
        return false;
    }

    @Override
    public ApformInstanceImpl createApamInstance(IPojoContext context, HandlerManager[] handlers) {
        throw new UnsupportedOperationException("APAM specification is not instantiable");
    }


    /**
     * Register this implementation with APAM
     */
    @Override
    protected void bindToApam(Apam apam) {
        Apform2Apam.newSpecification(this);
    }

    /**
     * Unregister this implementation from APAM
     *
     * @param apam
     */
    @Override
    protected void unbindFromApam(Apam apam) {
        ComponentBrokerImpl.disappearedComponent(getName()) ;

    }

    @Override
    public void setProperty(String attr, String value) {
        // TODO Auto-generated method stub

    }
    
	@Override
	public boolean setLink(Component destInst, String depName) {
		//TODO to implement
		throw new UnsupportedOperationException() ;
	}

	@Override
	public boolean remLink(Component destInst, String depName) {
		//TODO to implement
		throw new UnsupportedOperationException() ;
	}



}