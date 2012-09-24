package fr.imag.adele.apam.apformipojo.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.osgi.service.wireadmin.Producer;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
import org.osgi.service.wireadmin.WireConstants;

import fr.imag.adele.apam.apformipojo.ApformIpojoComponent;
import fr.imag.adele.apam.apformipojo.ApformIpojoImplementation;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.MessageProducerFieldInjection;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.MessageReference;
import fr.imag.adele.apam.message.AbstractConsumer;
import fr.imag.adele.apam.message.Message;


/**
 * This handler handles message production. It is at the same time a OSGi's WireAdmin producer and an APAM
 * abstract consumer, so that it translates message exchanges over APAM wires into concrete message exchanges
 * over WireAdmin wires.
 * 
 * This handler is also a field interceptor that injects itself into all fields used to transparently produce
 * messages. Fields must be declared of type AbstractConsumer<D>, and a reference to this handler will be
 * down casted and injected.
 *   
 * @author vega
 *
 */
public class MessageProviderHandler extends ApformHandler implements Producer, AbstractConsumer<Object>, FieldInterceptor {


	/**
	 * The registered name of this iPojo handler
	 */
	public static final String NAME = "producer";

	/**
	 * Whether the handler must register a wire producer to be associated to the managed instance
	 */
	private boolean isRegisteredProducer;
	
	/**
	 * Represent the producer flavors (Registration Property)
	 */
	private Class<?>[] messageFlavors;

	/**
	 * Represent the provider execution id
	 */
	private String providerId;

	/**
	 * Represent the producer persistent id
	 */
	private String producerId;
	
	/**
	 * The name of the wire property used to tag it with the provider identification
	 */
	public final static String ATT_PROVIDER_ID = "provider.id";
	
	/**
	 * A reference to the WireAdmin
	 */
	private WireAdmin wireAdmin;
	
	/**
	 * The list of connected consumers
	 */
	private List<Wire> wires;
	
    /**
     * (non-Javadoc)
     * 
     * @see
     * org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void configure(Element componentMetadata, Dictionary configuration) throws ConfigurationException {

    	if (!(getFactory() instanceof ApformIpojoImplementation))
    		return;

    	Set<MessageReference> providedMessages 	= getFactory().getDeclaration().getProvidedResources(MessageReference.class);
    	Set<Class<?>> providedFlavors 			=  new HashSet<Class<?>>();
    	for (MessageReference providedMessage : providedMessages) {
			try {
				providedFlavors.add(getFactory().loadClass(providedMessage.getJavaType()));
			} catch (ClassNotFoundException e) {
				throw new ConfigurationException(e.getLocalizedMessage());
			}
		}
    	
    	isRegisteredProducer	= !providedFlavors.isEmpty();
    	messageFlavors			= providedFlavors.toArray(new Class[providedFlavors.size()]);
    	wires					= new ArrayList<Wire>();

    	producerId 				= NAME+"["+getInstanceManager().getInstanceName()+"]";
    	providerId				= Long.toString(System.currentTimeMillis());
    	
    	ApformIpojoImplementation implementation	= (ApformIpojoImplementation) getFactory();
    	ImplementationDeclaration declaration		= implementation.getDeclaration();
    	
    	if (! (declaration instanceof AtomicImplementationDeclaration))
    		return;
    	
    	AtomicImplementationDeclaration primitive	= (AtomicImplementationDeclaration) declaration;
    	for (MessageProducerFieldInjection messageField : primitive.getProducerInjections()) {
    		
    		MessageReference messageReference = messageField.getResource().as(MessageReference.class);
    		
    		if (messageReference == null)
    			continue;
    		
    		FieldMetadata field	= getPojoMetadata().getField(messageField.getFieldName());
    
    		getInstanceManager().register(field,this);
		}
    	
    }

    /**
     * The WireAdmin producer Id
     */
    public String getProducerId() {
		return producerId;
	}
    
    /**
     * The identifier of this provider. This is a non-persistent, volatile identifier.
     * 
     * APAM wires are not persistent and are recreated by the resolution process at each execution. On the
     * other hand, WireAdmin wires are persistent across executions of the platform, so we use this identifier
     * to tag the wires so that we can garbage collect old wires at the next execution.
     * 
     */
    public String getProviderId() {
    	return providerId;
    }
    
    /**
     * The description of the state of the handler
     *
     */
    private class Description extends HandlerDescription {

		public Description() {
			super(MessageProviderHandler.this);
		}
		
		@Override
		public Element getHandlerInfo() {
			Element info = super.getHandlerInfo();
			info.addAttribute(new Attribute("producer.id",producerId));
			info.addAttribute(new Attribute("session.id",providerId));
			info.addAttribute(new Attribute("flavors",Arrays.toString(messageFlavors)));
			info.addAttribute(new Attribute("isRegistered",Boolean.toString(isRegisteredProducer)));
			
			for (Wire wire : wires) {
				Element wireInfo = new Element("wire",ApformIpojoComponent.APAM_NAMESPACE);
				wireInfo.addAttribute(new Attribute("consumer.id",(String)wire.getProperties().get(WireConstants.WIREADMIN_CONSUMER_PID)));
				wireInfo.addAttribute(new Attribute("flavors",Arrays.toString(wire.getFlavors())));
				info.addElement(wireInfo);
			}
			return info;
		}
    	
    }
    @Override
    public HandlerDescription getDescription() {
    	return new Description();
    }
    
    @Override
    public String toString() {
        return "APAM Message producer for " + getInstanceManager().getInstanceName();
    }

    /**
     * This WireAdmin producer can not be polled, it works only in push mode.
     */
	@Override
	public Object polled(Wire wire) {
		return null;
	}

	@Override
	public void consumersConnected(Wire[] newWires) {
		wires.clear();

		if (newWires == null || newWires.length == 0)
			return;

		/*
		 *  Because WireAdmin persists wires, we can get notified of wires from previous executions. APAM
		 *  wires on the other hand are not persistent across executions, so we try to avoid duplicates and
		 *  do some basic garbage collection here.
		 *  
		 *  NOTE The APAM message provider handler only manages wires created indirectly by mapping APAM
		 * 	resolution into WireAdmin events, so we can assume providerIds are unique.
		 */
		for (Wire wire : newWires) {
			
			String wireProvider = (String) wire.getProperties().get(ATT_PROVIDER_ID);
			
			// delete old wires or not apam wires
			if (wireProvider == null || ! wireProvider.equals(getProviderId())) {
				if (wireAdmin != null) wireAdmin.deleteWire(wire);
				continue;
			}
			
			// register new wire
			wires.add(wire);
		}
	}

	/**
	 * Broadcast the message to all connected consumers expecting this flavor
	 */
	@Override
	public void pushMessage(Message<Object> message) {
		
		if (message.getData() == null)
			return;
		
		for (Wire wire : wires) {
			
			/*
			 * Verify that the data is one of the supported flavors and send
			 */
			for (Class<?> flavor : wire.getFlavors()) {
				if (flavor.isAssignableFrom(message.getData().getClass())) {
					message.markAsSent();
					wire.update(message);
					break;
				}
			}
			
		}
	}

	@Override
	public void pushData(Object data) {
		pushMessage(new Message<Object>(data));
	}

	/**
	 * Injects this handler in all abstract consumer fields 
	 */
	@Override
	public Object onGet(Object pojo, String fieldName, Object value) {
		if(wires.isEmpty()) return null;
		return this;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

}
