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
import fr.imag.adele.apam.declarations.Reporter;

/**
 * This class represents a tool being able to encode an APAM component declaration in one of the 
 * different formats that can be used to store it.
 * 
 * Repository implementers should provide appropriate encoder implementations for the format that
 * they handle internally. 
 * 
 * The encoder is parameterized by the class of the internal object used to represent the declaration  
 * 
 * @author vega
 * 
 */
public interface Encoder<I> {


	/**
	 * encode the declaration in the format internally handled by this encoder
	 */
	public I encode(ComponentDeclaration component, Reporter reporter);
}
