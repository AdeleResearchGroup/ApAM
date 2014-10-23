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
package fr.imag.adele.apam.declarations.encoding.ipojo;

import org.apache.felix.ipojo.metadata.Element;

import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.Reporter;
import fr.imag.adele.apam.declarations.encoding.Decoder;

/**
 * Parse an APAM declaration from its iPojo metadata representation.
 * 
 * Notice that this parser tries to build a representation of the metadata
 * declarations even in the presence of errors. It is up to the error handler to
 * abort parsing if necessary by throwing unrecoverable parsing exceptions. It
 * will add place holders for missing information that can be verified after
 * parsing by another tool.
 * 
 * @author vega
 * 
 */
public class MetadataParser implements Decoder<Element> {

	/**
	 * A service to access introspection information for primitive components
	 */
	public interface IntrospectionService {

		/**
		 * Get reflection information for the implementation class
		 */
		public Class<?> getInstrumentedClass(String classname) throws ClassNotFoundException;
	}

	private final IntrospectionService introspection;

	public MetadataParser() {
		this(null);
	}
	
	public MetadataParser(IntrospectionService introspection) {
		this.introspection = introspection;
	}
	
	@Override
	public ComponentDeclaration decode(Element encodedDeclaration, Reporter errorHandler) {
		return new ComponentParser(introspection).decode(encodedDeclaration,errorHandler);
	}

}
