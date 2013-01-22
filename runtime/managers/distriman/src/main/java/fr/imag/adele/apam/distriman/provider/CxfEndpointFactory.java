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
import java.util.HashMap;
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

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.distriman.Distriman;
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

	private static Logger logger = LoggerFactory.getLogger(CxfEndpointFactory.class);
	
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

	public CxfEndpointFactory() {
		apamMan = ApamManagers.getManager(CST.APAMMAN);
	}

	public void start(HttpService http) {
		// TODO distriman: Disable the fast infoset as it's not compatible (yet)
		// with OSGi
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
			myurl = new URI("http://" + LocalMachine.INSTANCE.getHost() + ":"
					+ LocalMachine.INSTANCE.getPort() + ROOT_NAME).toString(); // compute
																				// the
																				// url
		} catch (Exception e) {
			// TODO log
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
	 */
	private String createEndpoint(Instance instance, Class iface) {
		Object obj = instance.getServiceObject();
		ServerFactoryBean srvFactory;

		Class<?> clazz = iface;

		// Use the classloader of the cxf bundle in order to create the ws.
		ClassLoader loader = Thread.currentThread().getContextClassLoader();

		Thread.currentThread().setContextClassLoader(
				ServerFactoryBean.class.getClassLoader());

		try {

			srvFactory = new ServerFactoryBean();

			if (clazz != null) {
				srvFactory.setServiceClass(clazz);
			}

			srvFactory.setBus(cxfbus); // Use the OSGi Servlet as the dispatcher
			srvFactory.setServiceBean(obj);

			System.out.println("####### Server side:" + myurl);

			// srvFactory.setAddress(myurl);
			srvFactory.setAddress("/" + instance.getName());

			// HashMap props = new HashMap();
			// props.put("jaxb.additionalContextClasses", new Class[] {
			// obj.getClass(),clazz });
			// srvFactory.setProperties(props);

			Server res = srvFactory.create(); // Publish the webservice.
			
			webservices.put(instance.getName(), res);

			return res.getEndpoint().getEndpointInfo().getAddress();

		} finally {
			// reset the context classloader to the original one.
			Thread.currentThread().setContextClassLoader(loader);
		}

	}

	public void destroyEndpoint(String name) {
		if (webservices.containsKey(name)) {
			webservices.remove(name).stop();
		} else {
			// TODO distriman: log the destruction of the endpoint
		}
	}

	public EndpointRegistration resolveAndExport(RemoteDependency dependency,
			RemoteMachine client) throws ClassNotFoundException {
		Instance neo = null; // The chosen one
		EndpointRegistration registration = null;
		// Get local instance matching the RemoteDependency

		logger.info("requesting apam instance {} to resolve the dependency {}",apamMan,dependency.getIdentifier());
		
		Resolved resolved = apamMan.resolveDependency(client.getInst(),
				dependency, true);

		// No local instance matching the RemoteDependency
		if (resolved.instances.isEmpty()) {
			
			logger.info("impossile to solve dependency, the number of instances was zero");
			
			return null;
		}

		logger.info("solve dependency, {} instance(s) where found",resolved.instances.size());
		
		// Check if we already have an endpoint for the instances
		synchronized (endpoints) {

			Sets.SetView<Instance> alreadyExported = Sets.intersection(
					resolved.instances, endpoints.keySet());

			// Nope, create a new endpoint
			if (alreadyExported.isEmpty()) {
				
				neo = resolved.instances.iterator().next();
				
				logger.info("dependency {} was NOT exported before, preparing endpoint for instance {}",dependency.getIdentifier(),neo);
				
				Class ifacecazz = loadInterfaceForProxyExport(neo);
				// create the endpoint.
				String endPointURL = createEndpoint(neo, ifacecazz);

				registration = new EndpointRegistrationImpl(this,neo, client, myurl
						+ "/" + neo.getName() // myurl + "/" + neo.getName()
				, PROTOCOL_NAME, ifacecazz.getCanonicalName());//

			} else {
				
				neo = alreadyExported.iterator().next();

				Class ifacecazz = loadInterfaceForProxyExport(neo);
				
				logger.info("dependency {} was exported before, using instance {}",dependency.getIdentifier(),neo);
				
				registration = new EndpointRegistrationImpl(this,endpoints.get(neo)
						.iterator().next());
			}

			// Add the EndpointRegistration to endpoints
			endpoints.put(neo, registration);
		}

		return registration;
	}

	private Class loadInterfaceForProxyExport(Instance instance) throws ClassNotFoundException {

		Class clazz = null;

		if (instance instanceof ComponentImpl) {
			
			ResourceReference ref = instance.getSpec().getApformSpec()
					.getDeclaration().getProvidedResources().iterator().next();

			System.out.println("Interface 2 :" + ref.getName());

				clazz = Class.forName(ref.getName());
				System.out.println("Type loaded:" + clazz.getCanonicalName());


		}

		return clazz;
	}

	

}
