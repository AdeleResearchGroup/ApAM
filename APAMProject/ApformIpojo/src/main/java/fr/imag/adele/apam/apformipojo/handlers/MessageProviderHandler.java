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
import fr.imag.adele.apam.core.FieldInjection;
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
	 * Represent the producer flavors (Registration Property)
	 */
	private Class<?>[] messageFlavors;

	/**
	 * Represent the producer persistent id
	 */
	private String producerId;
	
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

    	if (! getFactory().hasInstrumentedCode())
    		return;
    	
    	if ( ! (getFactory() instanceof ApformIpojoImplementation))
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
    	
    	messageFlavors	= providedFlavors.toArray(new Class[providedFlavors.size()]);
    	producerId 		= NAME+"["+getInstanceManager().getInstanceName()+"]";
    	wires			= new ArrayList<Wire>();
    	
    	ApformIpojoImplementation implementation	= (ApformIpojoImplementation) getFactory();
    	ImplementationDeclaration declaration		= implementation.getDeclaration();
    	
    	if (! (declaration instanceof AtomicImplementationDeclaration))
    		return;
    	
    	AtomicImplementationDeclaration primitive	= (AtomicImplementationDeclaration) declaration;
    	for (FieldInjection messageField : primitive.getProducerInjections()) {
    		
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
			info.addAttribute(new Attribute("flavors",Arrays.toString(messageFlavors)));
			
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
		 * 	resolution into WireAdmin events, so we can assume consumerIds are unique to sinlge dependency
		 * injection.
		 */
		Set<String> consumers = new HashSet<String>();
		for (Wire wire : newWires) {
			
			String consumerID = (String) wire.getProperties().get(WireConstants.WIREADMIN_CONSUMER_PID);
			
			// delete duplicates
			if (consumers.contains(consumerID)) {
				if (wireAdmin != null) wireAdmin.deleteWire(wire);
				continue;
			}
			
			// register new wire
			wires.add(wire);
			consumers.add(consumerID);
			
		}
	}

	/**
	 * Broadcast the message to all connected consumers expecting this flavor
	 */
	@Override
	public void sendMessage(Message<Object> message) {
		
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
	public void sendData(Object data) {
		sendMessage(new Message<Object>(data));
	}

	/**
	 * Injects this handler in all abstract consumer fields 
	 */
	@Override
	public Object onGet(Object pojo, String fieldName, Object value) {
		return this;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

}
