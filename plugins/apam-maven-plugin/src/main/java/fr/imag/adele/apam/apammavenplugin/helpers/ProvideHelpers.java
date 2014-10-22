/**
 * Copyright 2011-2013 Universite Joseph Fourier, LIG, ADELE team
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
 *
 * ProvideHelpers.java - 7 nov. 2013
 */
package fr.imag.adele.apam.apammavenplugin.helpers;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.apam.apammavenplugin.CheckObr;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;
import fr.imag.adele.apam.util.Util;

/**
 * @author thibaud
 * 
 */
public final class ProvideHelpers {

	public static void checkResources(ComponentDeclaration implementation, ComponentDeclaration specification, Class<? extends ResourceReference> kind, CheckObr validator) {
		Set<? extends ResourceReference> expected	= specification.getProvidedResources(kind);
		Set<? extends ResourceReference> declared	= implementation.getProvidedResources(kind);
	
		Set<? extends ResourceReference> unknown	= unknowns(implementation.getProvidedResources(UnknownReference.class), kind);
		
		if (! declared.containsAll(expected)) {
			if (! unknown.isEmpty()) {
				validator.warning("Unable to verify type of provided resources at compilation time, this may cause errors at the runtime!"
						+ "\n make sure that " + Util.list(unknown,true)	+ " are of the following types "+ Util.list(expected,true));
			} else {
				validator.error("Implementation " + implementation.getName() + " must provide " + Util.list(expected,true));
			}
		}
	}

	public static <R extends ResourceReference> Set<R> unknowns(Set<UnknownReference> unknowns, Class<R> kind) {
		
		Set<R> result = new HashSet<R>();
		for (UnknownReference unknown : unknowns) {
			
			R cast = unknown.as(kind);
			if (cast != null) {
				result.add(cast);
			}
		}

		return result;
	}
}