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

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.declarations.InstanceDeclaration;

public interface ApformInstance extends ApformComponent {

	/**
	 * Get the associated APAM instance
	 */
	@Override
	public Instance getApamComponent();

	/**
	 * Get the development model associated with the the instance
	 */
	@Override
	public InstanceDeclaration getDeclaration();

	/**
	 * 
	 * @return the real object implementing that instance.
	 */

	public Object getServiceObject();

}
