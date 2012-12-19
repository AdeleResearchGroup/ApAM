package fr.imag.adele.apam.distriman;

import fr.imag.adele.apam.Instance;

/**
 * User: barjo
 * Date: 18/12/12
 * Time: 12:15
 */
public interface EndpointRegistration {

    Instance getInstance();

    RemoteMachine getClient();

    String getEndpointUrl();

    String getProtocol();

    void close();
}

