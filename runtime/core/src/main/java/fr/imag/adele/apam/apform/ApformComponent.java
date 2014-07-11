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

import org.osgi.framework.Bundle;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;

/**
 * This class represents the interface between the logical state in APAM and the
 * underlying executing component.
 * 
 * @author vega
 * 
 */
public interface ApformComponent {

	/**
	 * Get the associated APAM component
	 */
	public Component getApamComponent();

	/**
	 * Get the bundle in which is located this component.
	 */
	public abstract Bundle getBundle();

	/**
	 * Get the development model associated with the the component
	 */
	public ComponentDeclaration getDeclaration();

	/**
	 * Remove a link, the relation is no longer valid (disappear target or other
	 * reason)
	 * 
	 */
	public boolean remLink(Component destInst, String depName);

	/**
	 * Associates the APAM corresponding APAM component.
	 * 
	 * This method is called when the APAM component is completely registered in
	 * the state model, an can be safely used.
	 * 
	 * When the APAM component is destroyed, this method will be called again
	 * with a null a value.
	 */
	public void setApamComponent(Component apamComponent) throws InvalidConfiguration;

	/**
	 * Notifies the underlying platform of a change in the outgoing links of the
	 * corresponding APAM component. This is usually the result of a relation
	 * resolution.
	 * 
	 * The Apform component can veto the creation of the link by returning false
	 * to ths call.
	 * 
	 */
	public boolean setLink(Component destInst, String depName);

	/**
	 * Check if link is valid (and therefore can be created) The Apform
	 * component can veto the creation of the link by returning false to ths
	 * call.
	 * 
	 */
	public boolean checkLink(Component destInst, String depName);

	/**
	 * Notifies the underlying platform of a change in a component property,
	 * performed through the APAM API
	 */
	public void setProperty(String attr, String value);
}
