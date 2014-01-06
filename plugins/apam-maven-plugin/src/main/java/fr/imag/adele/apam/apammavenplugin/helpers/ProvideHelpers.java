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

import java.util.Set;

import fr.imag.adele.apam.apammavenplugin.CheckObr;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.declarations.UndefinedReference;
import fr.imag.adele.apam.util.Util;

/**
 * @author thibaud
 * 
 */
public final class ProvideHelpers {

	/**
	 * @param impl
	 * @param messages
	 * @param messagesUndefined
	 * @param specMessages
	 */
	public static void checkMessages(ComponentDeclaration impl,
			Set<MessageReference> messages,
			Set<UndefinedReference> messagesUndefined,
			Set<MessageReference> specMessages) {
		if (!messages.containsAll(specMessages)) {
			if (!messagesUndefined.isEmpty()) {
				CheckObr.warning("Unable to verify message type at compilation time, this may cause errors at the runtime!"
						+ "\n make sure that "
						+ Util.toStringUndefinedResource(messagesUndefined)
						+ " are of the following message types "
						+ Util.toStringSetReference(specMessages));
			} else {
				CheckObr.error("Implementation " + impl.getName()
						+ " must produce messages "
						+ Util.toStringSetReference(specMessages));
			}
		}
	}

	/**
	 * @param impl
	 * @param interfaces
	 * @param interfacesUndefined
	 * @param specInterfaces
	 */
	public static void checkInterfaces(ComponentDeclaration impl,
			Set<InterfaceReference> interfaces,
			Set<UndefinedReference> interfacesUndefined,
			Set<InterfaceReference> specInterfaces) {
		if (!interfaces.containsAll(specInterfaces)) {
			if (!(interfacesUndefined.isEmpty())) {
				CheckObr.warning("Unable to verify intefaces type at compilation time, this may cause errors at the runtime!"
						+ "\n make sure that "
						+ Util.toStringUndefinedResource(interfacesUndefined)
						+ " are of the following interface types "
						+ Util.toStringSetReference(specInterfaces));
			} else {
				CheckObr.error("Implementation " + impl.getName()
						+ " must implement interfaces "
						+ Util.toStringSetReference(specInterfaces));
			}
		}
	}

}
