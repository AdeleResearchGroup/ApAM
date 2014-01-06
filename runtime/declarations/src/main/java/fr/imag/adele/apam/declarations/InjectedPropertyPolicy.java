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
package fr.imag.adele.apam.declarations;

/**
 * Specify How a property can be changed, by java or by ApAM API
 * 
 * @author thibaud
 * 
 */
public enum InjectedPropertyPolicy {

	/**
	 * Property can only be set or modified by the java class
	 */
	INTERNAL,

	/**
	 * Property can only be set by ApAM API
	 */
	EXTERNAL,

	/**
	 * Both is default value, changes are bidirectionnal from java OR from ApAM
	 * (the last one who spoke who's right). A value defined in ApAM descriptor
	 * will erase the one defined by the constructor.
	 */
	BOTH;

	public static InjectedPropertyPolicy getPolicy(String id) {
		for (InjectedPropertyPolicy p : values()) {
			if (p.toString().toLowerCase().equals(id)) {
				return p;
			}
		}

		return null;
	}

}
