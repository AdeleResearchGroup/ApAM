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
package fr.imag.adele.apam.distriman.discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jmdns.JmDNS;
import javax.jmdns.NetworkTopologyDiscovery;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.distriman.provider.LocalMachine;

/**
 * <p>
 * The MachineDiscovery component allows for the discovery of other machines
 * (Apam/Distriman) over the network, thanks to the mdns protocol.
 * </p>
 * 
 * A RemoteMachine instance is created for each machine discovered.
 * 
 * 
 * User: barjo / jander Date: 04/12/12 Time: 14:48
 */
@Component(name = "Apam::Distriman::Discovery")
@Instantiate
@Provides
public class ApamDiscoveryImpl implements ApamDiscovery,
		NetworkTopologyDiscovery.Factory.ClassDelegate {

	private static Logger logger = LoggerFactory
			.getLogger(ApamDiscoveryImpl.class);

	@Property(name = "inet.host", value = "127.0.0.1", mandatory = true)
	private String HOST;

	private LocalMachine local;

	/**
	 * JmDNS, Java Multicast DNS, use to announce and discovered Apam/Distriman
	 * machine over the network.
	 */
	private Map<JmDNS, String> jmDNSMachines;// ArrayList<JmDNS>();

	/**
	 * Compute a default name for that machine, TODO compute a more relevant
	 * name.
	 */
	private String name = UUID.randomUUID().toString();

	@Requires
	private ApamMachineFactory machineFactory;

	public ApamDiscoveryImpl() {
		super();
		NetworkTopologyDiscovery.Factory.setClassDelegate(this);
	}

	@Validate
	public void start() {

		logger.info("Starting mdns...");

		if (jmDNSMachines != null)
			throw new RuntimeException(
					"Trying to start machine discovery twice.");

		jmDNSMachines = new HashMap<JmDNS, String>();// new
														// CopyOnWriteArrayList<JmDNS>();

		logger.info("Iteratings interfaces..");

		// Create the jmdns server
		for (InetAddress address : NetworkTopologyDiscovery.Factory
				.getInstance().getInetAddresses()) {

			try {

				JmDNS current = JmDNS.create(address);

				current.registerServiceType(MDNS_TYPE);

				jmDNSMachines.put(current, address.getHostAddress());

				for (ServiceInfo sinfo : current.list(MDNS_TYPE)) {
					if (sinfo.getName().equalsIgnoreCase(name)) {
						continue; // ignore my services..
					}

					// Create and Add the machine
					String url = sinfo.getNiceTextString();

					logger.info("mDNS detected the url {} subtype {}", url,
							sinfo.getTypeWithSubtype());

					String id = String.format("%s.%s", sinfo.getName(),
							sinfo.getType());

					machineFactory.newRemoteMachine(url, id, false);
				}

				current.addServiceListener(MDNS_TYPE, this);

			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

		}

		logger.info("/Iteratings interfaces..");

		logger.info("mdns started.");

	}

	@Invalidate
	public void stop() {
		
		for (Map.Entry<JmDNS, String> entry : jmDNSMachines.entrySet()) {

			JmDNS jmDNS = entry.getKey();

			jmDNS.unregisterAllServices();

			// unregister the listener
			jmDNS.removeServiceListener(MDNS_TYPE, this);

			try {
				jmDNS.close();
			} catch (IOException e) {

			}

		}
	}

	public void publishLocalMachine(LocalMachine local) throws IOException {

		this.local = local;

		for (Map.Entry<JmDNS, String> entry : jmDNSMachines.entrySet()) {

			JmDNS jmDNS = entry.getKey();
			String urlaux = entry.getValue();

			String url = String.format("http://%s:%d", urlaux,
					local.getPort());

			// Register a local machine
			logger.info("publishing machine {} on the mdns bus", url);

			jmDNS.registerService(ServiceInfo.create(local.getType(),
					local.getName(), local.getPort(), url));

		}

	}

	@Override
	public void serviceAdded(ServiceEvent serviceEvent) {
		// Ignore, only handle resolved
		logger.info("service added {}", serviceEvent.getInfo()
				.getNiceTextString());
	}

	/**
	 * @param serviceEvent
	 *            The mdns event triggered by a remote machine that is no longer
	 *            available.
	 */
	public void serviceRemoved(ServiceEvent serviceEvent) {

		String id = String.format("%s.%s", serviceEvent.getName(),
				serviceEvent.getType());

		logger.info("service removing {} with id {}", serviceEvent.getInfo()
				.getNiceTextString(), id);

		if (serviceEvent.getName().equalsIgnoreCase(name)) {
			return; // ignore my message
		}

		ServiceInfo info = serviceEvent.getInfo();
		String url = info.getNiceTextString();

		machineFactory.destroyRemoteMachine(url, id);
	}

	private boolean isItLocal(ServiceEvent serviceEvent) {
		for (Map.Entry<JmDNS, String> entry : jmDNSMachines.entrySet()) {

			JmDNS jmDNS = entry.getKey();
			try {
				if (jmDNS
						.getInterface()
						.getHostAddress()
						.equals(serviceEvent.getDNS().getInterface().getHostAddress())
						&& serviceEvent.getInfo().getPort() == this.local
								.getPort()) {
					return true;
				}
			} catch (IOException e) {
				//consider as not local in case of problem (conservative approach
			}

		}
		
		return false;
	}
	
	public void serviceResolved(ServiceEvent serviceEvent) {

		String id = String.format("%s.%s", serviceEvent.getName(),
				serviceEvent.getType());

		logger.info("service resolved {} subtype {}", serviceEvent.getInfo()
				.getNiceTextString(), id);

		boolean isLocalhost = isItLocal(serviceEvent);

		if (serviceEvent.getName().equalsIgnoreCase(name)) {
			return; // ignore this machine message
		}

		ServiceInfo info = serviceEvent.getDNS().getServiceInfo(MDNS_TYPE,
				serviceEvent.getName());
		String url = info.getNiceTextString();

		machineFactory.newRemoteMachine(url, id, isLocalhost);
	}

	
	/**
	 * Factory that determines the euristics to choose/filter the network cards to be considered 
	 */
	@Override
	public NetworkTopologyDiscovery newNetworkTopologyDiscovery() {
		return new NetworkTopology();
	}
}