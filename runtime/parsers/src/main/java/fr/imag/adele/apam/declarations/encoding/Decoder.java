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
package fr.imag.adele.apam.declarations.encoding;

import fr.imag.adele.apam.declarations.ComponentDeclaration;

/**
 * This class represents a tool being able to decode one of the different formats that can be used
 * to represent APAM component declarations.
 * 
 * Repository implementers should provide appropriate decoder implementations for the format that
 * they handle internally. 
 * 
 * The decoder is parameterized by the class of the internal object used to encode the declaration  
 * 
 * @author vega
 * 
 */
public interface Decoder<I> {

	/**
	 * An string value that will be used to represent mandatory attributes not
	 * specified
	 */
	public final static String UNDEFINED = new String("<undefined value>");

	/**
	 * parses the encoded representation of the declaration
	 */
	public ComponentDeclaration decode(I encodedDeclaration, Reporter reporter);
}
