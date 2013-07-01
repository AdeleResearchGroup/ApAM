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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Relation;
import fr.imag.adele.apam.RelationManager;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.distriman.client.RemoteMachine;
import fr.imag.adele.apam.distriman.discovery.ApamDiscovery;
import fr.imag.adele.apam.distriman.discovery.ApamMachineFactory;
import fr.imag.adele.apam.distriman.dto.RemoteDependencyDeclaration;
import fr.imag.adele.apam.distriman.provider.CxfEndpointFactory;
import fr.imag.adele.apam.distriman.provider.EndpointRegistration;
import fr.imag.adele.apam.distriman.provider.LocalMachine;

/**
 * Core of distriman dependency manager
 * @author jnascimento
 *
 */
@org.apache.felix.ipojo.annotations.Component(name = "Apam::Distriman::core")
@Instantiate
@Provides
public class Distriman implements RelationManager {

	private static Logger logger = LoggerFactory.getLogger(Distriman.class);

	@Requires(proxy=false)
	Apam apam;
	
	@Requires(optional = false)
	private HttpService httpserver;

	@Requires(optional = false)
	private ApamMachineFactory remotes;
	
	@Requires(optional = false)
	private ApamDiscovery discovery;
	
	private CxfEndpointFactory endpointFactory;
	
	private LocalMachine providerLocal;

	private BundleContext context;
	
	public Distriman(BundleContext context) {

		System.setProperty("java.net.preferIPv6Addresses", "false");
		this.context = context;

	}

	public String getName() {
		return CST.DISTRIMAN;
	}

//	@Override
//	public void getSelectionPath(Instance client,
//			RelationDeclaration dependency, List<RelationManager> selPath) {
//		selPath.add(selPath.size(), this);
//	}

	@Override
	public int getPriority() {
		return DistrimanConstant.APAM_PRIORITY;
	}

	@Override
	public void newComposite(ManagerModel model, CompositeType composite) {
		// To change body of implemented methods use File | Settings | File
		// Templates.
	}

	/**
	 * That's the meat! Ask synchroneously to each available RemoteMachine to
	 * resolved the <code>dependency</code>, the first to solve it create the
	 * proxy.
	 * 
	 * @param client
	 *            the instance asking for the resolution (and where to create
	 *            implementation, if needed). Cannot be null.
	 * @param relation
	 *            a dependency declaration containing the type and name of the
	 *            dependency target. It can be -the specification Name (new
	 *            SpecificationReference (specName)) -an implementation name
	 *            (new ImplementationRefernece (name) -an interface name (new
	 *            InterfaceReference (interfaceName)) -a message name (new
	 *            MessageReference (dataTypeName)) - or any future resource ...
	 * @param needsInstances
	 * @return The Resolved object if a proxy has been created, null otherwise.
	 */
	
	/*
	
	@Override
	public Resolved resolveDependency(Instance client,
			Relation relation, boolean needsInstances) {
		Resolved resolved = null;

		if (!needsInstances) { // TODO distriman: should really just handle only
								// instances?
			return null;
		}

		for (Map.Entry<String, RemoteMachine> element : remotes.getMachines()
				.entrySet()) {

			RemoteMachine machine = element.getValue();
			String urlForResolution = element.getKey();

			if(machine.isLocalhost()) continue;
			
			try {

				logger.info("trying to resolve in machine key {} and url {}",
						urlForResolution, urlForResolution);

				resolved = machine.resolveRemote(client, relation);

				if (resolved != null && resolved.instances != null
						&& resolved.instances.size() > 0)
					break;

			} catch (IOException e) {
				remotes.destroyRemoteMachine(urlForResolution, element
						.getValue().getId());
			}
		}

		return resolved;
	}

*/

	@Validate
	private void init() {
		
		try {
			logger.info("Starting...");

			
			RelationManager manager; //= ApamManagers.getManager(CST.APAMMAN);
			
			while(CST.componentBroker==null || (manager = ApamManagers.getManager(CST.APAMMAN))==null){
				
				logger.info("Waiting APAMMAN to appear...");
				
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {}
				
			}

			providerLocal=new LocalMachine(Integer.parseInt(context
					.getProperty("org.osgi.service.http.port")),this);
			
			endpointFactory = new CxfEndpointFactory(manager);
			
			endpointFactory.start(httpserver);

			// Register this local machine servlet

			httpserver.registerServlet(DistrimanConstant.PROVIDER_URL,
						providerLocal.getServlet(), null, null);

			// publish this local machine over the network!
			discovery.publishLocalMachine(providerLocal);

			// Add this manager to Apam
			ApamManagers.addRelationManager(this, DistrimanConstant.APAM_PRIORITY);

			logger.info("Successfully initialized");
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
			stop();
			
		} 
		
		
		ApamManagers.addRelationManager(this,getPriority());
		
	}

	@Invalidate
	private void stop() {
		logger.info("Stopping...");

		ApamManagers.removeRelationManager(this);

		discovery.stop();

		endpointFactory.stop(httpserver);

		remotes.destroyRemoteMachines();

		httpserver.unregister(DistrimanConstant.PROVIDER_URL);

		logger.info("Successfully stopped");
	}

	public EndpointRegistration resolveDependencyLocalMachine(
			RemoteDependencyDeclaration dependency, String providerURL)
			throws ClassNotFoundException {

		logger.info(
				String.format("provider (%s) requested resolution of dependency identifier %s in the provider",providerURL,
				dependency.getIdentifier()).toString());

		logger.info("distriman available machines");

		for (Map.Entry<String, RemoteMachine> entry : remotes.getMachines()
				.entrySet()) {

			logger.info("distriman machine {} local {}", entry.getKey(),entry.getValue().isLocalhost());

		}

		// Get the composite that represent the remote machine asking to resolve
		// the RemoteDependency
		
		RemoteMachine remote = remotes.getMachines().get(providerURL);

		// No RemoteMachine corresponding to the given url is available
		if (remote == null) {
			return null;
		}
		
		logger.info("remote machine recovered key:{} ", remote.getURLServlet());

		return endpointFactory.resolveAndExport(dependency, remote);
	}

	@Override
	public void getSelectionPath(Component source, Relation relation,
			List<RelationManager> selPath) {
		selPath.add(selPath.size(), this);
	}

	@Override
	public Resolved<?> resolveRelation(Component source, Relation relation) {
		
		Resolved resolved = null;

//		for (Map.Entry<String, RemoteMachine> element : remotes.getMachines()
//				.entrySet()) {
//
//			RemoteMachine machine = element.getValue();
//			String urlForResolution = element.getKey();
//
//			if(machine.isLocalhost()) continue;
//			
//			try {
//
//				logger.info("trying to resolve in machine key {} and url {}",
//						urlForResolution, urlForResolution);
//
//				resolved = machine.resolveRemote(source.getApformComponent().getApamComponent(), relation);
//
//				if (resolved != null && resolved.instances != null
//						&& resolved.instances.size() > 0)
//					break;
//
//			} catch (IOException e) {
//				remotes.destroyRemoteMachine(urlForResolution, element
//						.getValue().getId());
//			}
//		}
//
//		System.out.println("distriman resolveRelation");
		
		return resolved;		
		
	}

	@Override
	public void notifySelection(Component client, ResolvableReference resName,
			String depName, Implementation impl, Instance inst,
			Set<Instance> insts) {
		System.out.println("distriman notifySelection");
		
	}

	@Override
	public ComponentBundle findBundle(CompositeType context,
			String bundleSymbolicName, String componentName) {
		System.out.println("distriman findBundle");
		return null;
	}


}
