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

import java.util.Collection;
import java.util.Collections;
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

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.RelationManager;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.distriman.DistrimanConstant;
import fr.imag.adele.apam.distriman.client.RemoteMachine;
import fr.imag.adele.apam.distriman.dto.RemoteDependency;
import fr.imag.adele.apam.distriman.dto.RemoteDependencyDeclaration;
import fr.imag.adele.apam.distriman.provider.impl.EndpointRegistrationImpl;
import fr.imag.adele.apam.impl.ComponentImpl;
import fr.imag.adele.apam.impl.RelToResolveImpl;

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

	private final RelationManager apamMan;
	
	private Bus cxfbus; 

	/**
	 * Multimap containing the exported Instances and their Endpoint
	 * registrations
	 */
	private final SetMultimap<Instance, EndpointRegistration> endpoints = HashMultimap
			.create();

	public SetMultimap<Instance, EndpointRegistration> getEndpoints() {
		return endpoints;
	}

	private final Map<String, Server> webservices = new HashMap<String, Server>();

	public CxfEndpointFactory(RelationManager manager) {
		apamMan = manager;
	}

	public void start(HttpService http) {
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
				
				logger.info("Server {} started!",res.getEndpoint().getEndpointInfo().getAddress());
				
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

	public EndpointRegistration resolveAndExport(RemoteDependencyDeclaration dependency,
			RemoteMachine client) throws ClassNotFoundException {
		Instance neo = null; // The chosen one
		EndpointRegistration registration = null;
		// Get local instance matching the RemoteDependency

		logger.info("requesting apam instance {} to resolve the dependency {}",
				apamMan, dependency.getIdentifier());

		RemoteDependency remote=new RemoteDependency(dependency, null);
		
		//((RelationImpl)client.getApamComponent().getRelation(dependency.getIdentifier()))
		
		RelToResolveImpl rel=new RelToResolveImpl(client.getApamComponent(), dependency);
		//rel.computeFilters(client.getApamComponent());
		
		Resolved resolved = apamMan.resolveRelation(client.getApamComponent(), rel);
		
		//resolveDependency( client.getServiceObject(),remote, true);

		// No local instance matching the RemoteDependency
		if (resolved==null) {

			logger.info("impossile to solve dependency, the number of instances was zero");

			return null;
		}

//		logger.info("solve dependency, {} instance(s) where found",
//				resolved.toInstantiate);

		// Check if we already have an endpoint for the instances
		synchronized (endpoints) {

			Sets.SetView<Component> alreadyExported = Sets.intersection(
					Collections.singleton(resolved.singletonResolved), endpoints.keySet());

			// Nope, create a new endpoint
			if (alreadyExported.isEmpty()) {
				
				neo = (Instance)resolved.singletonResolved; //instances.iterator().next();

				logger.info(
						"dependency {} was NOT exported before, preparing endpoint for instance {}",
						dependency.getIdentifier(), neo);

				Map<Class, String> localEndpoints = createEndpoint(neo);

				registration = new EndpointRegistrationImpl(this, neo,
						client, PROTOCOL_NAME);
				
				for (Map.Entry<Class, String> endpoint : localEndpoints.entrySet()) {
				
					registration.getEndpoint().put(endpoint.getKey().getCanonicalName(), client.getURLRoot() + DistrimanConstant.PROVIDER_CXF_DOMAIN + endpoint.getValue());
					
				}
				
				endpoints.put(neo, registration);
				
			} else {

				neo = (Instance)alreadyExported.iterator().next();

				logger.info(
						"dependency {} was exported before, using instance {}",
						dependency.getIdentifier(), neo);

				registration = endpoints.get(neo).iterator().next();
			}

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
