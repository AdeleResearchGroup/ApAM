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
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;

public class ApamAtomicComponentFactory extends ApamImplementationFactory {

	
    /**
     * Build a new factory with the specified metadata
     */
    public ApamAtomicComponentFactory(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);
    }

    @Override
    protected final boolean hasInstrumentedCode() {
        return true;
    }

	@Override
	protected ApformComponent createApform() {
		return this.new Apform();
	}

	private class Apform extends ApamImplementationFactory.Apform<Implementation, AtomicImplementationDeclaration> {
	}
}