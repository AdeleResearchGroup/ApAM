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
package fr.imag.adele.apam.distriman.web;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.http.HttpService;

@org.apache.felix.ipojo.annotations.Component(name = "Apam::Distriman Web")
@Instantiate
public class DistrimanWeb {

	private final static String URL="/distriman";
	private final static String RESOURCE="/static";
	
	@Requires(nullable = false, optional = false)
	DistrimanServlet world;
	
	@Requires(optional = false)
	HttpService http;

	@Validate
	private void init() {
		try {
			http.registerServlet(URL, world, null, null);
			http.registerResources(RESOURCE, "/", null);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Invalidate
	private void stop() {
		http.unregister(URL);
		http.unregister(RESOURCE);
	}
}
