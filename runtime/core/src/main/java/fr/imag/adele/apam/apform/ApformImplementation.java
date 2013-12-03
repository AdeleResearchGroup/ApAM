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
package fr.imag.adele.apam.apform;

import java.util.Map;

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.impl.ComponentImpl;

public interface ApformImplementation extends ApformComponent {

    /**
     * Creates a new instance of this implementation and registers the
     * associated component in APAM.
     * 
     * This method can be used by external services (like device discovery
     * protocols) to create instances in APAM that are not the result of a
     * resolution.
     * 
     * Notice that the configuration can contain values that are not necessarily
     * APAM properties, but specific to a given ApformImplementation
     */
    public ApformInstance addDiscoveredInstance(
	    Map<String, Object> configuration)
	    throws ComponentImpl.InvalidConfiguration,
	    UnsupportedOperationException;

    /**
     * Creates an instance of this implementation, and initialize its properties
     * with the set of provided properties.
     * 
     * This method is called by APAM when a new instance is created by the
     * resolver or directly by API
     */
    public ApformInstance createInstance(Map<String, String> initialproperties)
	    throws ComponentImpl.InvalidConfiguration;

    /**
     * Get the associated APAM implementation
     */
    @Override
    public Implementation getApamComponent();

    /**
     * Get the development model associated with the the implementation
     */
    @Override
    public ImplementationDeclaration getDeclaration();
}
