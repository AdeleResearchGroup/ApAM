package fr.imag.adele.apam.distriman.provider.impl;

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
	private String url;
	private String protocol;
	private String interfaceCanonical;
	private transient final CxfEndpointFactory endPointfactory;
	private static Logger     logger           = LoggerFactory.getLogger(InstanceImpl.class);

	public String getInterfaceCanonical() {
		return interfaceCanonical;
	}

	public void setInterfaceCanonical(String interfaceCanonical) {
		this.interfaceCanonical = interfaceCanonical;
	}

	public EndpointRegistrationImpl(CxfEndpointFactory factory,Instance instance,
			RemoteMachine client, String endpointUrl, String protocol,
			String ifaceCanonical) {
		if (instance == null || client == null || endpointUrl == null) {
			throw new NullPointerException(
					"Instance, RemoteMachine, endpointUrl cannot be null");
		}
		this.endPointfactory=factory;
		this.exported = instance;
		this.client = client;
		this.url = endpointUrl;
		this.protocol = protocol;
		this.interfaceCanonical = ifaceCanonical;
		client.addEndpointRegistration(this);
		logger.info("creating EndpointRegistration {}",endpointUrl);
	}

	/**
	 * Clone
	 * 
	 * @param registration
	 *            The EndpointRegistration to be cloned.
	 */
	public EndpointRegistrationImpl(CxfEndpointFactory factory,EndpointRegistration registration) {
		this(factory,registration.getInstance(), registration.getClient(),
				registration.getEndpointUrl(), registration.getProtocol(),
				registration.getInterfaceCanonical());
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
			url = null;
			protocol = null;
		}
	}

}