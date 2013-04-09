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

import java.io.IOException;
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

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Dependency;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.distriman.client.RemoteMachine;
import fr.imag.adele.apam.distriman.discovery.MachineDiscovery;
import fr.imag.adele.apam.distriman.discovery.RemoteMachineFactory;
import fr.imag.adele.apam.distriman.dto.RemoteDependencyDeclaration;
import fr.imag.adele.apam.distriman.provider.CxfEndpointFactory;
import fr.imag.adele.apam.distriman.provider.EndpointRegistration;
import fr.imag.adele.apam.distriman.provider.LocalMachine;

@org.apache.felix.ipojo.annotations.Component(name = "Apam::Distriman")
@Instantiate
@Provides
public class Distriman implements DependencyManager {

	private static Logger logger = LoggerFactory.getLogger(Distriman.class);

	private static final int APAM_PRIORITY = 40;

	@Requires(optional = false)
	private HttpService httpserver;

	private CxfEndpointFactory endpointFactory;

	private RemoteMachineFactory remotes;

	private final LocalMachine providerLocal = LocalMachine.INSTANCE;

	/**
	 * MachineDiscovery allows for machine discovery
	 */
	private MachineDiscovery discovery;

	private BundleContext context;

	public Distriman(BundleContext context) {

		try {
			this.context = context;
			remotes = new RemoteMachineFactory(context);
			discovery = new MachineDiscovery(remotes);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}

	}

	public String getName() {
		return CST.DISTRIMAN;
	}

	@Override
	public void getSelectionPath(Instance client,
			DependencyDeclaration dependency, List<DependencyManager> selPath) {
		selPath.add(selPath.size(), this);
	}

	@Override
	public int getPriority() {
		return APAM_PRIORITY;
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
	 * @param dependency
	 *            a dependency declaration containing the type and name of the
	 *            dependency target. It can be -the specification Name (new
	 *            SpecificationReference (specName)) -an implementation name
	 *            (new ImplementationRefernece (name) -an interface name (new
	 *            InterfaceReference (interfaceName)) -a message name (new
	 *            MessageReference (dataTypeName)) - or any future resource ...
	 * @param needsInstances
	 * @return The Resolved object if a proxy has been created, null otherwise.
	 */
	@Override
	public Resolved resolveDependency(Instance client,
			Dependency dependency, boolean needsInstances) {
		Resolved resolved = null;

		if (!needsInstances) { // TODO distriman: should really just handle only
								// instances?
			return null;
		}

		for (Map.Entry<String, RemoteMachine> element : remotes.getMachines()
				.entrySet()) {

			RemoteMachine machine = element.getValue();
			String urlForResolution = element.getKey();

			try {

				logger.info("trying to resolve in machine key {} and url {}",
						urlForResolution, machine.getURL());

				resolved = machine.resolveRemote(client, dependency);

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

	@Validate
	private void init() {
		try {
			logger.info("Starting...");

			DependencyManager manager = ApamManagers.getManager(CST.APAMMAN);

			if (manager == null) {
				throw new RuntimeException(
						String.format(
								"Distriman could not be initialized, it was not possible to get the instance of ",
								CST.APAMMAN));
			}

			endpointFactory = new CxfEndpointFactory(manager);

			// init the local machine
			providerLocal.init("127.0.0.1", Integer.parseInt(context
					.getProperty("org.osgi.service.http.port")), this);

			// start the discovery
			discovery.start();

			// start the CxfEndpointFactory
			endpointFactory.start(httpserver, providerLocal);

			// Register this local machine servlet
			try {
				httpserver.registerServlet(LocalMachine.INSTANCE.getPath(),
						providerLocal.getServlet(), null, null);
			} catch (Exception e) {
				discovery.stop();
				// TODO distriman:avoid throw here and stoping the instance
				// creation
				throw new RuntimeException(e);
			}

			// publish this local machine over the network!
			try {
				discovery.publishLocalMachine(providerLocal);
			} catch (IOException e) {
				discovery.stop();
				httpserver.unregister(providerLocal.getPath());
				// TODO distriman:avoid throw here and stoping the instance
				// creation
				throw new RuntimeException(e);
			}

			// Add this manager to Apam
			ApamManagers.addDependencyManager(this, APAM_PRIORITY);

			logger.info("Successfully initialized");
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	@Invalidate
	private void stop() {
		logger.info("Stopping...");

		ApamManagers.removeDependencyManager(this);

		discovery.stop();

		endpointFactory.stop(httpserver);

		remotes.destroyRemoteMachines();

		httpserver.unregister(LocalMachine.INSTANCE.getPath());

		logger.info("Successfully stopped");
	}

	public EndpointRegistration resolveDependencyLocalMachine(
			RemoteDependencyDeclaration dependency, String clientURL)
			throws ClassNotFoundException {

		logger.info(
				String.format("client (%s) requested resolution of dependency identifier %s in the provider(%s)",clientURL,
				dependency.getIdentifier(),providerLocal.getURL()).toString());

		logger.info("distriman available machines");

		for (Map.Entry<String, RemoteMachine> entry : remotes.getMachines()
				.entrySet()) {

			logger.info("distriman machine {}", entry.getKey());

		}

		// Get the composite that represent the remote machine asking to resolve
		// the RemoteDependency
		
		RemoteMachine remote = remotes.getMachines().get(providerLocal.getURL());

		logger.info("remote machine recovered key:{} ", remote.getURL());

		// No RemoteMachine corresponding to the given url is available
		if (remote == null) {
			return null;
		}

		return endpointFactory.resolveAndExport(dependency, remote);
	}

	@Override
	public Instance findInstByName(Instance client, String instName) {
		return null; // To change body of implemented methods use File |
						// Settings | File Templates.
	}

	@Override
	public Implementation findImplByName(Instance client, String implName) {
		return null; // To change body of implemented methods use File |
						// Settings | File Templates.
	}

	@Override
	public Specification findSpecByName(Instance client, String specName) {
		return null; // To change body of implemented methods use File |
						// Settings | File Templates.
	}

	@Override
	public Component findComponentByName(Instance client, String compName) {
		return null; // To change body of implemented methods use File |
						// Settings | File Templates.
	}

	@Override
	public Instance resolveImpl(Instance client, Implementation impl,  Dependency dep) {
//			Set<String> constraints, List<String> preferences) {
		return null; // To change body of implemented methods use File |
						// Settings | File Templates.
	}

	@Override
	public Set<Instance> resolveImpls(Instance client, Implementation impl, Dependency dep) {
//			Set<String> constraints) {
		return null; // To change body of implemented methods use File |
						// Settings | File Templates.
	}

	@Override
	public void notifySelection(Instance client, ResolvableReference resName,
			String depName, Implementation impl, Instance inst,
			Set<Instance> insts) {
		// To change body of implemented methods use File | Settings | File
		// Templates.
	}

	@Override
	public ComponentBundle findBundle(CompositeType context,
			String bundleSymbolicName, String componentName) {
		return null; // To change body of implemented methods use File |
						// Settings | File Templates.
	}
}
