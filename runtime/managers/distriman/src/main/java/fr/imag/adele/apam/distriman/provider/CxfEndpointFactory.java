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
package fr.imag.adele.apam.distriman.provider;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.distriman.client.RemoteMachine;
import fr.imag.adele.apam.distriman.dto.RemoteDependency;
import fr.imag.adele.apam.distriman.provider.impl.EndpointRegistrationImpl;
import fr.imag.adele.apam.impl.ComponentImpl;

/**
 * User: barjo Date: 18/12/12 Time: 14:15
 * 
 * @ThreadSafe
 */
public class CxfEndpointFactory {

	private static Logger logger = LoggerFactory
			.getLogger(CxfEndpointFactory.class);

	public static final String PROTOCOL_NAME = "cxf";
	public static final String ROOT_NAME = "/ws";

	private Bus cxfbus; // Cxf dispatcher, set in start!

	private String myurl;
	private final DependencyManager apamMan;

	/**
	 * Multimap containing the exported Instances and their Endpoint
	 * registrations
	 */
	private final SetMultimap<Instance, EndpointRegistration> endpoints = HashMultimap
			.create();

	public SetMultimap<Instance, EndpointRegistration> getEndpoints() {
		return endpoints;
	}

	/**
     *
     */
	private final Map<String, Server> webservices = new HashMap<String, Server>();

	public CxfEndpointFactory(DependencyManager manager) {
		apamMan = manager;
	}

	public void start(HttpService http, LocalMachine machine) {
		// TODO distriman: Disable the fast infoset as it's not compatible (yet)
		System.setProperty("org.apache.cxf.nofastinfoset", "true");

		// Register the CXF Servlet
		ClassLoader loader = Thread.currentThread().getContextClassLoader();

		// switch to the cxg minimal bundle class loader
		Thread.currentThread().setContextClassLoader(
				CXFNonSpringServlet.class.getClassLoader());

		try {
			CXFNonSpringServlet servlet = new CXFNonSpringServlet();

			// Register a CXF Servlet dispatcher
			http.registerServlet(ROOT_NAME, servlet, null, null);

			// get the bus
			cxfbus = servlet.getBus();

		} catch (Exception e) {
			// TODO log
			throw new RuntimeException(e);
		} finally {
			Thread.currentThread().setContextClassLoader(loader);
		}

		// compute the PROP_CXF_URL property
		try {
			myurl = new URI("http://" + machine.getHost() + ":"
					+ machine.getPort() + ROOT_NAME).toString();

			logger.info("instantiating endpoint factory for the url {}", myurl);

		} catch (Exception e) {
			// TODO distriman
			// "Cannot create the URL of the JAX-WS server, this will lead to incomplete EndpointDescription.",e);
		}
	}

	public void stop(HttpService http) {
		// Unregister servlet dispatcher
		http.unregister(ROOT_NAME);
	}

	/**
	 * Create the Instance ServiceObject endpoint with CXF.
	 * 
	 * @param instance
	 * @throws ClassNotFoundException
	 */
	private Map<Class, String> createEndpoint(Instance instance)
			throws ClassNotFoundException {
		Object obj = instance.getServiceObject();
		ServerFactoryBean srvFactory;

		Map<Class, String> result = new HashMap<Class, String>();

		Collection<Class> classes = loadInterfaceForProxyExport(instance);

		for (Class iface : classes) {

			// ClassLoader loader =
			// Thread.currentThread().getContextClassLoader();
			// Thread.currentThread().setContextClassLoader(
			// ServerFactoryBean.class.getClassLoader());

			try {

				srvFactory = new ServerFactoryBean();

				srvFactory.setServiceClass(iface);

				srvFactory.setBus(cxfbus); // Use the OSGi Servlet as the
											// dispatcher
				srvFactory.setServiceBean(obj);

				srvFactory.setAddress("/" + instance.getName() + "/"
						+ iface.getCanonicalName().replaceAll("\\.", "/"));

				// HashMap props = new HashMap();
				// try {
				// props.put("jaxb.additionalContextClasses", new Class[] {
				// Class.forName("fr.imag.adele.apam.pax.test.iface.P2SpecKeeper")
				// });
				// srvFactory.setProperties(props);
				// } catch (ClassNotFoundException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }

				Server res = srvFactory.create();
				
				while(!res.isStarted())
					try {
						logger.info("Server {} not started, waiting..",srvFactory.getAddress());
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
				logger.info("Server {} started!",srvFactory.getAddress());
				
				webservices.put(srvFactory.getAddress(), res);

				result.put(iface, res.getEndpoint().getEndpointInfo()
						.getAddress());

			} finally {
				// reset the context classloader to the original one.
				// Thread.currentThread().setContextClassLoader(loader);
			}

		}

		return result;

	}

	public void destroyEndpoints() {
		
		logger.info("destroying endpoints..");
		
		for(Map.Entry<String, Server> element:webservices.entrySet()){
			String wsUrl=element.getKey();
			Server entrypoint=element.getValue();
			entrypoint.stop();
			logger.info("endpoint {} destroyed.",wsUrl);
			webservices.remove(element.getKey());
		}
		
	}

	public EndpointRegistration resolveAndExport(RemoteDependency dependency,
			RemoteMachine client) throws ClassNotFoundException {
		Instance neo = null; // The chosen one
		EndpointRegistration registration = null;
		// Get local instance matching the RemoteDependency

		logger.info("requesting apam instance {} to resolve the dependency {}",
				apamMan, dependency.getIdentifier());

		Resolved resolved = apamMan.resolveDependency(client.getInst(),
				dependency, true);

		// No local instance matching the RemoteDependency
		if (resolved==null||resolved.instances==null||resolved.instances.isEmpty()) {

			logger.info("impossile to solve dependency, the number of instances was zero");

			return null;
		}

		logger.info("solve dependency, {} instance(s) where found",
				resolved.instances.size());

		// Check if we already have an endpoint for the instances
		synchronized (endpoints) {

			Sets.SetView<Instance> alreadyExported = Sets.intersection(
					resolved.instances, endpoints.keySet());

			// Nope, create a new endpoint
			if (alreadyExported.isEmpty()) {

				neo = resolved.instances.iterator().next();

				logger.info(
						"dependency {} was NOT exported before, preparing endpoint for instance {}",
						dependency.getIdentifier(), neo);

				Map<Class, String> endpoints = createEndpoint(neo);

				String fullURL = "";
				
				for (Map.Entry<Class, String> endpoint : endpoints.entrySet()) {

					logger.info("cxf endpoint created in the address {}",
							fullURL);
					
					fullURL+= myurl + endpoint.getValue()+"!"+endpoint.getKey().getCanonicalName()+",";

				}
				
				registration = new EndpointRegistrationImpl(this, neo,
						client, fullURL, PROTOCOL_NAME,fullURL);

			} else {

				neo = alreadyExported.iterator().next();

				logger.info(
						"dependency {} was exported before, using instance {}",
						dependency.getIdentifier(), neo);

				registration = new EndpointRegistrationImpl(this, endpoints
						.get(neo).iterator().next());
			}

			// Add the EndpointRegistration to endpoints
			endpoints.put(neo, registration);
		}

		return registration;
	}

	private Collection<Class> loadInterfaceForProxyExport(Instance instance)
			throws ClassNotFoundException {

		Collection<Class> classes = new HashSet<Class>();

		logger.info("gigging interfaces used in the apam instance {}",instance.getName());
		
		if (instance instanceof ComponentImpl) {

			logger.info("getting reference for the interfaces of the instance..");
			
			for (ResourceReference ref : instance.getSpec().getApformSpec()
					.getDeclaration().getProvidedResources()) {
				logger.info("adding {} as interfaces for this instance",ref.getName());
				classes.add(Class.forName(ref.getName()));
			}
			
			logger.info("done.");

		}

		return classes;
	}

}
