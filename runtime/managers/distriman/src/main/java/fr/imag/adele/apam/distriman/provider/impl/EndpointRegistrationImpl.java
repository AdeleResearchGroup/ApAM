package fr.imag.adele.apam.distriman.provider.impl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.distriman.client.RemoteMachine;
import fr.imag.adele.apam.distriman.provider.CxfEndpointFactory;
import fr.imag.adele.apam.distriman.provider.EndpointRegistration;
import fr.imag.adele.apam.impl.InstanceImpl;

/**
 * EndpointRegistration implementation.
 */
public class EndpointRegistrationImpl implements EndpointRegistration {
    private Instance exported;
    private RemoteMachine client;
    private String protocol;
    private String interfaceCanonical;
    private transient final CxfEndpointFactory endPointfactory;
    private final Map<String, String> endpoint = new HashMap<String, String>();

    private static Logger logger = LoggerFactory.getLogger(InstanceImpl.class);

    /**
     * Clone
     * 
     * @param registration
     *            The EndpointRegistration to be cloned.
     */
    public EndpointRegistrationImpl(CxfEndpointFactory factory,
	    EndpointRegistration registration) {
	this(factory, registration.getInstance(), registration.getClient(),
		registration.getProtocol());
    }

    public EndpointRegistrationImpl(CxfEndpointFactory factory,
	    Instance instance, RemoteMachine client, String protocol) {
	if (instance == null || client == null) {
	    throw new NullPointerException(
		    "Instance, RemoteMachine, endpointUrl cannot be null");
	}
	this.endPointfactory = factory;
	this.exported = instance;
	this.client = client;
	this.protocol = protocol;
	client.addEndpointRegistration(this);
    }

    @Override
    public void close() {

	logger.info("destroying endpoints");

	if (exported == null) {
	    return;
	}

	synchronized (endPointfactory.getEndpoints()) {

	    client.rmEndpointRegistration(this);

	    endPointfactory.getEndpoints().remove(getInstance(), getClient());
	    endPointfactory.destroyEndpoints();

	    exported = null;
	    client = null;
	    protocol = null;
	}
    }

    @Override
    public RemoteMachine getClient() {
	return client;
    }

    @Override
    public Map<String, String> getEndpoint() {
	return endpoint;
    }

    @Override
    public Instance getInstance() {
	return exported;
    }

    public String getInterfaceCanonical() {
	return interfaceCanonical;
    }

    @Override
    public String getProtocol() {
	return protocol;
    }

    public void setInterfaceCanonical(String interfaceCanonical) {
	this.interfaceCanonical = interfaceCanonical;
    }

}