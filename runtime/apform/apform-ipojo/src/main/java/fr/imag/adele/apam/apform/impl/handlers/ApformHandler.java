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
package fr.imag.adele.apam.apform.impl.handlers;

import org.apache.felix.ipojo.PrimitiveHandler;

import fr.imag.adele.apam.apform.impl.ApformComponentImpl;
import fr.imag.adele.apam.apform.impl.ApformInstanceImpl;

/**
 * The base class for all iPojo handlers manipulating APAM components
 * 
 * @author vega
 * 
 */
public abstract class ApformHandler extends PrimitiveHandler {

	
	@Override
	public ApformComponentImpl getFactory() {
		return (ApformComponentImpl)super.getFactory();
	}
	
	@Override
	public ApformInstanceImpl getInstanceManager() {
		return (ApformInstanceImpl) super.getInstanceManager();
	}

}
