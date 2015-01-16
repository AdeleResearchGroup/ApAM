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
package fr.imag.adele.apam.util;

import java.net.URL;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.impl.APAMImpl;

public class ApamInstall {

	private static Logger logger = LoggerFactory.getLogger(ApamInstall.class);

	private static boolean deployBundle(URL url, String compoName) {
		Bundle bundle = null;
		try {
			bundle = APAMImpl.context.installBundle(url.toString());
			if (!ApamInstall.getAllComponentNames(bundle).contains(compoName)) {
				logger.error("Bundle " + url.toString() + " does not contain " + compoName + " but contains " + ApamInstall.getAllComponentNames(bundle));
				return false;
			}
			bundle.start();
			return true;
		} catch (BundleException e) {
			logger.error(e.getMessage());
			return false;
		}
	}

	public static Set<String> getAllComponentNames(Bundle bundle) {
		Set<String> componentNames = new HashSet<String>();

		Dictionary<String, String> headers = bundle.getHeaders();
		String iPOJO_components = headers.get("iPOJO-Components");

		ApamInstall.getcompositeNames(iPOJO_components, "fr.imag.adele.apam:specification", componentNames);
		ApamInstall.getcompositeNames(iPOJO_components, "fr.imag.adele.apam:composite", componentNames);
		ApamInstall.getcompositeNames(iPOJO_components, "fr.imag.adele.apam:implementation", componentNames);
		ApamInstall.getcompositeNames(iPOJO_components, "ipojo:component", componentNames);
		ApamInstall.getcompositeNames(iPOJO_components, "component", componentNames);

		return componentNames;
	}

	private static void getcompositeNames(String metadata, String balise, Set<String> names) {
		if ((metadata == null) || metadata.isEmpty()) {
			return;
		}
		int compoIndex = metadata.indexOf(balise);
		if (compoIndex == -1) {
			return;
		}
		String components = metadata.substring(compoIndex + 5);
		String name = ApamInstall.getNamecomponent(components);
		if (name != null) {
			names.add(name);
		}
		ApamInstall.getcompositeNames(components, balise, names);
	}

	private static String getNamecomponent(String string) {
		String[] tokens = string.split("\\s");
		String name;
		for (String token : tokens) {
			if (token.startsWith("$name=")) {
				name = token.substring(7, token.length() - 1);
				return name;
			}
		}
		return null;
	}

	public static Component installFromURL(URL url, String compoName) {
		if (!ApamInstall.deployBundle(url, compoName)) {
			return null;
		}
		return CST.componentBroker.getWaitComponent(compoName);
	}

	public static Implementation intallImplemFromURL(URL url, String compoName) {
		Component impl = installFromURL(url, compoName);
		if (impl == null) {
			return null;
		}
		if (!(impl instanceof Implementation)) {
			logger.error("component " + compoName + " is found but is not an Implementation.");
			return null;
		}
		return (Implementation) impl;
	}

	public static Specification intallSpecFromURL(URL url, String compoName) {
		Specification spec = (Specification) installFromURL(url, compoName);
		if (spec == null) {
			return null;
		}
		if (!(spec instanceof Specification)) {
			logger.error("component " + compoName + " is found but is not a Specification.");
		}
		return spec;

	}

}
