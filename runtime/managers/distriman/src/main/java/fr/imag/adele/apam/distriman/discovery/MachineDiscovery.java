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

import fr.imag.adele.apam.distriman.provider.LocalMachine;

import org.apache.felix.ipojo.annotations.*;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

/**
 * <p>The MachineDiscovery component allows for the discovery of other
 * machines (Apam/Distriman) over the network, thanks to the mdns protocol.</p>
 *
 * A RemoteMachine instance is created for each machine discovered.
 *
 *
 * User: barjo
 * Date: 04/12/12
 * Time: 14:48
 */
@Component(name = "Apam::Distriman::Discovery")
@Instantiate
public class MachineDiscovery implements ServiceListener {
    /**
     * The mdns type to be used.
     */
    public static String MDNS_TYPE = "_apam._http._tcp.local.";


    /**
     * JmDNS, Java Multicast DNS,
     * use to announce and discovered Apam/Distriman machine over the network.
     */
    private JmDNS jmDNS;

    /**
     * Compute a default name for that machine,
     * TODO compute a more relevant name.
     */
    private String name = UUID.randomUUID().toString();


    @Requires
    private ApamMachineDiscovery machineFactory;

    /**
     * @param machineFactory the RemoteMachineFactory that instantiate new RemoteMachine.
     */
    public MachineDiscovery(RemoteMachineFactory machineFactory) {
        this.machineFactory = machineFactory; //singleton
    }

    /**
     * Start the MachineDiscovery instance. Initialize <code>jmDNS</code>.
     *
     * @param host the hostname of the InetAddress to be used.
     */
    @Validate
    public void start(String host) {
        try {
            //Create the jmdns server
            InetAddress address = InetAddress.getByName(host);
            jmDNS = JmDNS.create(address);
        } catch (IOException e){
            //TODO log an error
            throw new RuntimeException(e);
        }

        //Register the Apam type, MDNS_TYPE
        jmDNS.registerServiceType(MDNS_TYPE);


        //Retrieve all existing machines
        for (ServiceInfo sinfo : jmDNS.list(MDNS_TYPE)){
            if(sinfo.getName().equalsIgnoreCase(name)){
                continue; //ignore my services..
            }

            //Create and Add the machine
            String url = sinfo.getURL();
            
            machineFactory.newRemoteMachine(url);
        }

        //Add this as a listener in order to track change
        jmDNS.addServiceListener(MDNS_TYPE,this);
    }

    @Invalidate
    public void stop()  {
        //unregister this machine.
        jmDNS.unregisterAllServices();

        //unregister the listener
        jmDNS.removeServiceListener(MDNS_TYPE,this);

        try {
            jmDNS.close();
        } catch (IOException e) {
            //TODO log WARNING
        }
    }

    public void publishLocalMachine(LocalMachine local) throws IOException{
        //Register a local machine
        jmDNS.registerService(ServiceInfo.create(local.getType(), local.getName(), local.getPort(), local.getURL()));
    }


    // ========================
    // JmDns Service Listeners
    // ========================

    @Override
    public void serviceAdded(ServiceEvent serviceEvent) {
        //Ignore, only handle resolved
    }

    /**
     * @param serviceEvent The mdns event triggered by a remote machine that is no longer available.
     */
    public void serviceRemoved(ServiceEvent serviceEvent) {
        if(serviceEvent.getName().equalsIgnoreCase(name)){
            return; //ignore my message
        }

        ServiceInfo info = serviceEvent.getInfo();
        String url = info.getNiceTextString(); //Get the content
        machineFactory.destroyRemoteMachine(url);
    }

    /**
     * @param serviceEvent The mdns event triggered by a remote machine that is now available.
     */
    public void serviceResolved(ServiceEvent serviceEvent) {
        if(serviceEvent.getName().equalsIgnoreCase(name)){
            return; // ignore this machine message
        }

        ServiceInfo info = jmDNS.getServiceInfo(MDNS_TYPE, serviceEvent.getName());
        String url = info.getNiceTextString();

        machineFactory.newRemoteMachine(url);
    }
}