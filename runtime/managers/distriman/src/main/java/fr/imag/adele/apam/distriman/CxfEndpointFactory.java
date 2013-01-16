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
package fr.imag.adele.apam.distriman;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.jws.WebService;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.service.http.HttpService;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Resolved;

/**
 * User: barjo Date: 18/12/12 Time: 14:15
 * 
 * @ThreadSafe
 */
public class CxfEndpointFactory {

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

	/**
     *
     */
	private final Map<String, Server> webservices = new HashMap<String, Server>();

	public CxfEndpointFactory() {
		// Get apamMan
		apamMan = ApamManagers.getManager(CST.APAMMAN);
	}

	protected void start(HttpService http) {
		// Disable the fast infoset as it's not compatible (yet) with OSGi
		// XXX from dosgi
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

	protected void stop(HttpService http) {
		// Unregister servlet dispatcher
		http.unregister(ROOT_NAME);
	}

	/**
	 * Create the Instance ServiceObject endpoint with CXF.
	 * 
	 * @param instance
	 */
	private String createEndpoint(Instance instance) {
		Object obj = instance.getServiceObject();
		ServerFactoryBean srvFactory;
		// TODO get the class ?
		Class<?> clazz = Object.class; // The service object spec

		// Use the classloader of the cxf bundle in order to create the ws.
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		// Thread.currentThread().setContextClassLoader(JaxWsServerFactoryBean.class.getClassLoader());
		Thread.currentThread().setContextClassLoader(
				ServerFactoryBean.class.getClassLoader());

		try {

			// Check if the interface contains the jax-ws annotation
			if (clazz != null && clazz.isAnnotationPresent(WebService.class)) {
				srvFactory = new JaxWsServerFactoryBean();
			} else {
				srvFactory = new ServerFactoryBean();
			}

			// If the interface has been specified use it in order to create the
			// endpoint.
			if (clazz != null) {
				srvFactory.setServiceClass(clazz);
			}

			srvFactory.setBus(cxfbus); // Use the OSGi Servlet as the dispatcher
			srvFactory.setServiceBean(obj);

			System.out.println("####### Server side:" + myurl);

			// srvFactory.setAddress(myurl);
			srvFactory.setAddress("/" + instance.getName());

			Server res = srvFactory.create(); // Publish the webservice.
			// Endpoint.publish(myurl, obj);

			webservices.put(instance.getName(), res);

			return res.getEndpoint().getEndpointInfo().getAddress();

		} finally {
			// reset the context classloader to the original one.
			Thread.currentThread().setContextClassLoader(loader);
		}

	}

	private void destroyEndpoint(String name) {
		if (webservices.containsKey(name)) {
			webservices.remove(name).stop();
		} else {
			// TODO log
		}
	}

	public EndpointRegistration resolveAndExport(RemoteDependency dependency,
			RemoteMachine client) {
		Instance neo = null; // The chosen one
		EndpointRegistration registration = null;
		// Get local instance matching the RemoteDependency

		System.out.println(String.format(
				"Trying to solve instance %s to server %s",
				dependency.getIdentifier(), client.getUrl()));

		Resolved resolved = apamMan.resolveDependency(client.getInst(),
				dependency, true);

		// No local instance matching the RemoteDependency
		if (resolved.instances.isEmpty()) {
			return null;
		}

		// Check if we already have an endpoint for the instances
		synchronized (endpoints) {

			Sets.SetView<Instance> alreadyExported = Sets.intersection(
					resolved.instances, endpoints.keySet());

			// Nope, create a new endpoint
			if (alreadyExported.isEmpty()) {
				System.out.println("#####" + this.getClass().getCanonicalName()
						+ " creating endpoint");

				neo = resolved.instances.iterator().next();

				// create the endpoint.
				String endPointURL=createEndpoint(neo);

				registration = new EndpointRegistrationImpl(neo, client, myurl + "/" + neo.getName() //myurl + "/" + neo.getName()
						, PROTOCOL_NAME);

			} else {
				System.out.println("#####" + this.getClass().getCanonicalName()
						+ " already endpoint already exists");

				neo = alreadyExported.iterator().next();

				registration = new EndpointRegistrationImpl(endpoints.get(neo)
						.iterator().next());
			}

			// Add the EndpointRegistration to endpoints
			endpoints.put(neo, registration);
		}

		return registration;
	}

	/**
	 * EndpointRegistration implementation.
	 */
	private class EndpointRegistrationImpl implements EndpointRegistration {
		private Instance exported;
		private RemoteMachine client;
		private String url;
		private String protocol;

		private EndpointRegistrationImpl(Instance instance,
				RemoteMachine client, String endpointUrl, String protocol) {
			if (instance == null || client == null || endpointUrl == null) {
				throw new NullPointerException(
						"Instance, RemoteMachine, endpointUrl cannot be null");
			}

			this.exported = instance;
			this.client = client;
			this.url = endpointUrl;
			this.protocol = protocol;

			client.addEndpointRegistration(this);
		}

		/**
		 * Clone
		 * 
		 * @param registration
		 *            The EndpointRegistration to be cloned.
		 */
		private EndpointRegistrationImpl(EndpointRegistration registration) {
			this(registration.getInstance(), registration.getClient(),
					registration.getEndpointUrl(), registration.getProtocol());
		}

		@Override
		public Instance getInstance() {
			return exported;
		}

		@Override
		public RemoteMachine getClient() {
			return client;
		}

		@Override
		public String getEndpointUrl() {
			return url;
		}

		@Override
		public String getProtocol() {
			return protocol;
		}

		@Override
		public void close() {
			// Has already been closed
			if (exported == null) {
				return;
			}

			synchronized (endpoints) {

				// remove this EndpointRegistration to the RemoteMachine that
				// ask for it.
				client.rmEndpointRegistration(this);

				endpoints.remove(getInstance(), getClient());

				// Last registration, destroy the endpoints.
				if (!endpoints.containsKey(getInstance()))
					destroyEndpoint(getInstance().getName());

				exported = null;
				client = null;
				url = null;
				protocol = null;

				// todo if last destroy endpoint.
			}
		}

	}

}
