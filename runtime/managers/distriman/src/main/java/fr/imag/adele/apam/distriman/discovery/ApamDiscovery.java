package fr.imag.adele.apam.distriman.discovery;

import java.io.IOException;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import fr.imag.adele.apam.distriman.provider.LocalMachine;

public interface ApamDiscovery extends ServiceListener{

    /**
     * The mdns type to be used.
     */
    public static String MDNS_TYPE = "_apam._http._tcp.local.";
	
    public void publishLocalMachine(LocalMachine local) throws IOException;

    public void serviceAdded(ServiceEvent serviceEvent);

    public void serviceRemoved(ServiceEvent serviceEvent);

    public void serviceResolved(ServiceEvent serviceEvent);
	
}
