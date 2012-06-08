package fr.imag.adele.apam.apformipojo.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Callback;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.wireadmin.Consumer;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
import org.osgi.service.wireadmin.WireConstants;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apformipojo.ApformIpojoComponent;
import fr.imag.adele.apam.apformipojo.ApformIpojoInstance;
import fr.imag.adele.apam.core.DependencyInjection;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.message.AbstractProducer;
import fr.imag.adele.apam.message.Message;

/**
 * This handler handles message consumers. It is at the same time a OSGi's WireAdmin consumer and an APAM
 * abstract producer, so that it translates message exchanges over APAM wires into concrete message exchanges
 * over WireAdmin wires.
 * 
 * This handler is also a field interceptor that injects itself into all fields used to transparently consume
 * messages. Fields must be declared of type AbstractProducer<D>, and a reference to this handler will be
 * down casted and injected.
 * 
 * This handler is also in charge of triggering lazy resolution, if data is consumed and there is no producer
 * bound to this dependency. It also translates APAM notifications for wire creation and deletion into 
 * appropriate actions at the WireAdmin level.
 * 
 * @author vega
 *
 */public class MessageInjectionManager implements DependencyInjectionManager, Consumer, AbstractProducer<Object> {

	/**
	 * The registered name of this iPojo handler
	 */
	public static final String NAME = "consumer";
	
	/**
	 * The source component of the dependency
	 */
	private final ApformIpojoComponent 	component;
	
	/**
	 * The associated resolver
	 */
	private final ApformIpojoInstance	instance;
	
	/**
	 * The dependency injection managed by this dependency
	 */
	private final DependencyInjection injection;
	
    /**
     * The list of target services.
     */
    private final Set<Instance> targetServices;

    /**
     * The WireAdmin consumer registration. A consumer is registered when the dependency is
     * resolved and automatically unregistered when the dependency gets unresolved.
     * 
     */
    private ServiceRegistration	consumer;

    /**
     * The list of connected producers, indexed by target identification
     */
    private final Map<String,Wire> wires;

    /**
     * The maximum size of the buffer
     */
    private final static int MAX_BUFFER_SIZE = 100;
    
	/**
	 * The buffer of received messages that are waiting for being consumed 
	 */
	private final Queue<Message<Object>> buffer;
    
	/**
	 * In case of method callback, an aobject to allow direct invocation of the instance
	 */
	private final Callback callback;
	
	private final boolean isMessageCallback;
	
    /**
	 * Represent the consumed flavors (Registration Property)
	 */
	private final Class<?>[] messageFlavors;

	/**
	 * Represent the consumer persistent id
	 */
	private final String consumerId;
	
    
    public MessageInjectionManager(ApformIpojoComponent component, ApformIpojoInstance instance, DependencyInjection injection) throws ConfigurationException {
        
    	assert injection.getResource() instanceof InterfaceReference;
    	
    	this.component	= component;
        this.instance 	= instance;
        this.injection	= injection;
        
        if (injection instanceof DependencyInjection.Callback) {
        	MethodMetadata callbackMetadata	= null;
        	String callbackParameterType	= null;
        	
        	for (MethodMetadata method : component.getPojoMetadata().getMethods(injection.getName())) {
        		if (method.getMethodArguments().length == 1) {
        			callbackMetadata 		= method;
        			callbackParameterType	= method.getMethodArguments()[0];
        		}
			}
        	
        	this.callback			= new Callback(callbackMetadata, instance);
        	this.isMessageCallback	= Message.class.getCanonicalName().equals(callbackParameterType);

        }
        else {
        	this.callback 			= null;
        	this.isMessageCallback	= false;
        }
        
        this.targetServices = new HashSet<Instance>();
        this.consumer 		= null;

    	try {
			this.messageFlavors	= new Class<?>[] {component.loadClass(injection.getResource().getJavaType())};
	    	this.consumerId 	= NAME+"["+instance.getInstanceName()+","+injection.getDependency().getIdentifier()+","+injection.getName()+"]";
	        this.wires			= new HashMap<String,Wire>();
	        this.buffer			= new ArrayBlockingQueue<Message<Object>>(MAX_BUFFER_SIZE);

    	} catch (ClassNotFoundException e) {
			throw new ConfigurationException(e.getLocalizedMessage());
		}
       
    	instance.addInjection(this);
    }

    /**
     * The dependency injection associated to this manager
     */
    @Override
    public DependencyInjection getDependencyInjection() {
    	return injection;
    }
    
    /**
     * Get an XML representation of the state of this dependency
     */
    public Element getDescription() {

    	Element consumerDescription = new Element("injection", ApformIpojoComponent.APAM_NAMESPACE);
    	consumerDescription.addAttribute(new Attribute("dependency", injection.getDependency().getIdentifier()));
    	consumerDescription.addAttribute(new Attribute("target", injection.getDependency().getTarget().toString()));
    	consumerDescription.addAttribute(new Attribute("field", injection.getName()));
    	consumerDescription.addAttribute(new Attribute("type", injection.getResource().toString()));
    	consumerDescription.addAttribute(new Attribute("isAggregate",	Boolean.toString(injection.isCollection())));
    	consumerDescription.addAttribute(new Attribute("resolved",Boolean.toString(isResolved())));

		if (isResolved()) {
			
			consumerDescription.addAttribute(new Attribute("consumer.id",consumerId));
	    	consumerDescription.addAttribute(new Attribute("flavors",Arrays.toString(messageFlavors)));
	    	consumerDescription.addAttribute(new Attribute("buffered",Integer.toString(buffer.size())));

			if (isResolved()) {
				for (Wire wire : wires.values()) {
					
					Element wireInfo = new Element("wire",ApformIpojoComponent.APAM_NAMESPACE);
					wireInfo.addAttribute(new Attribute("producer.id",(String)wire.getProperties().get(WireConstants.WIREADMIN_PRODUCER_PID)));
					wireInfo.addAttribute(new Attribute("flavors",Arrays.toString(wire.getFlavors())));
					consumerDescription.addElement(wireInfo);
				}
			}
	    	
		}
		
		return consumerDescription;
	
    }
    
    public void onSet(Object pojo, String fieldName, Object value) {
        // Nothing to do, this should never happen as we exclusively handle the field's value
    }

    public Object onGet(Object pojo, String fieldName, Object value) {

        /*
         * Verify if there is a service fault (a required dependency is not present) and delegate to APAM handling of
         * this case.
         */
        if (!isResolved()) {

            /*
             * Ask APAM to resolve the dependency. Depending on the application policies this may throw an error, or
             * block the thread until the dependency is fulfilled, or keep the dependency unresolved in the case of
             * optional dependencies.
             * 
             * Resolution has as side-effect a modification of the target services.
             */ 
        	instance.resolve(this);
        }

         return getFieldValue(fieldName);
    }

    /**
     * Get the value to be injected in the field. The returned object depends on the resolution of the dependency
     * associated to this injection.
     * 
     * If the dependency is resolved we inject this manager in the field to handle the message buffer. Otherwise
     * we return null to signal the program an unresolved dependency.
     * 
     */
    private Object getFieldValue(String fieldName) {
       synchronized (this) {
            return (consumer != null) ? this : null;        		
        }

    }
    
    /**
     * Get access to the wireadmin, via the DependencyInjectionHandler
     * 
     */
    private WireAdmin getWireAdmin() {
    	String injectionHandlerId = ApformIpojoComponent.APAM_NAMESPACE+":"+DependencyInjectionHandler.NAME;
    	DependencyInjectionHandler handler = (DependencyInjectionHandler) instance.getHandler(injectionHandlerId);
    	return handler.getWireAdmin();
    }
    
    /**
     * The consumer Id associated to this manager
     */
    public String getConsumerId() {
		return consumerId;
	}
    
    /**
     * The identification of the APAM message producer. It is composed of the identification of the
     * provider and the WireAdmin consumer identifier
     * @author vega
     *
     */
    private class MessageProducerIdentifier {
    	public final String providerId;
    	public final String producerId;
    	
    	public MessageProducerIdentifier(String providerId, String producerId) {
			this.providerId = providerId;
			this.producerId	= producerId;
		}
    	
    }
    /**
     * The message producer associated to the given target instance
     * 
     * NOTE This performs an unchecked down casts, as it assumes the target instance  is an Apform-iPojo provided
     * instance
     */
    public MessageProducerIdentifier getMessageProducer(Instance target) {
    	String providerHandlerId = ApformIpojoComponent.APAM_NAMESPACE+":"+MessageProviderHandler.NAME;
        ApformIpojoInstance producer = (ApformIpojoInstance) target.getApformInst();
    	MessageProviderHandler providerHandler = (MessageProviderHandler) producer.getHandler(providerHandlerId);
    	
    	return new MessageProducerIdentifier(providerHandler.getProviderId(),providerHandler.getProducerId());
    }
    
     
    /* (non-Javadoc)
	 * @see fr.imag.adele.apam.apformipojo.handlers.DependencyInjectionManager#addTarget(fr.imag.adele.apam.Instance)
	 */
    @Override
	public void addTarget(Instance target) {

        /*
         * Add this target and invalidate cache
         */
        synchronized (this) {

            /*
             * Register the consumer on the first resolution
             */
            if (targetServices.isEmpty()) {
            	Properties properties = new Properties();
            	properties.put(WireConstants.WIREADMIN_CONSUMER_FLAVORS,messageFlavors);
            	properties.put("service.pid",consumerId);
            	consumer = component.getBundleContext().registerService(Consumer.class.getCanonicalName(), this, properties);
            }
            
            targetServices.add(target);
            
            /*
             * Create a wire at the WireAdmin level
             */
            WireAdmin wireAdmin = getWireAdmin();
            if (wireAdmin != null) {
            	MessageProducerIdentifier messageProducer = getMessageProducer(target);
            	Properties wireProperties = new Properties();
            	wireProperties.put(MessageProviderHandler.ATT_PROVIDER_ID, messageProducer.providerId);
            	Wire wire = wireAdmin.createWire(messageProducer.producerId, getConsumerId(), wireProperties);
            	wires.put(target.getName(),wire);
            }
        }

    }

    /* (non-Javadoc)
	 * @see fr.imag.adele.apam.apformipojo.handlers.DependencyInjectionManager#removeTarget(fr.imag.adele.apam.Instance)
	 */
    @Override
	public void removeTarget(Instance target) {

        /*
         * Remove this target and invalidate cache
         */
        synchronized (this) {

            /*
             * Remove the wire at the WireAdmin level
             */
            WireAdmin wireAdmin = getWireAdmin();
            Wire wire			= wires.remove(target.getName());
            if (wireAdmin != null && wire != null)
            	wireAdmin.deleteWire(wire);
        	
            targetServices.remove(target);
            
            /*
             * Unregister the consumer if the dependency becomes unresolved
             */
            if (targetServices.isEmpty()) {
            	consumer.unregister();
            	consumer = null;
            }
            
        }
    }

    /* (non-Javadoc)
	 * @see fr.imag.adele.apam.apformipojo.handlers.DependencyInjectionManager#substituteTarget(fr.imag.adele.apam.Instance, fr.imag.adele.apam.Instance)
	 */
    @Override
	public void substituteTarget(Instance oldTarget, Instance newTarget) {

        /*
         * substitute the target atomically and invalidate the cache
         */
        synchronized (this) {

            if (!targetServices.contains(oldTarget))
                return;

            /*
             * Update wires at the WireAdmin level
             */
            WireAdmin wireAdmin = getWireAdmin();
            
            Wire wire	= wires.remove(oldTarget.getName());
            if (wireAdmin != null && wire != null)
            	wireAdmin.deleteWire(wire);

            if (wireAdmin != null) {
            	MessageProducerIdentifier messageProducer = getMessageProducer(newTarget);
            	Properties wireProperties = new Properties();
            	wireProperties.put(MessageProviderHandler.ATT_PROVIDER_ID, messageProducer.providerId);
            	wire = wireAdmin.createWire(messageProducer.producerId, getConsumerId(), wireProperties);
            	wires.put(newTarget.getName(),wire);
            }

            targetServices.remove(oldTarget);
            targetServices.add(newTarget);
        }
    }

    /**
     * Whether this dependency is satisfied by a target service.
     * 
     */
    public boolean isResolved() {
        synchronized (this) {
            return !targetServices.isEmpty();
        }

    }

    /**
     * Consumes a message and put it in the buffer for later retrieval.
     * 
     * NOTE This methods unsafely down cast the received value to a message, as we assume that
     * we only exchange messages with APAM producers.
     */
	@Override
	@SuppressWarnings("unchecked")
	public void updated(Wire wire, Object value) {
		
		if (!(value instanceof Message))
			return;
		
		Message<Object> message = (Message<Object>) value;
		message.markAsReceived(wire.getProperties());
		buffer.offer(message);
		
		/*
		 * If a callback is registered consume message directly and push it to the component
		 */
		if (callback != null) {
			try {
				Message<Object> consumed  = getMessage(); 
				if (consumed != null) {
					if (isMessageCallback)
						callback.call(new Object[] {consumed});
					else
						callback.call(new Object[] {consumed.getData()});
				}
			} catch (Exception e) {
				System.err.println("error invoking callbaack "+e);
			} 	
		}
	}

	@Override
	public void producersConnected(Wire[] newWires) {
		/*
		 * The APAM dependency handler only manages wires created indirectly by mapping APAM
		 * resolution into WireAdmin events. Those wires are already tracked by this manager,
		 * so we can ignore asynchronous notifications
		 */
	}

	/**
	 * Retrieves the first message in the buffer if available
	 */
	@Override
	public Message<Object> getMessage() {
		return buffer.poll();
	}

	@Override
	public List<Message<Object>> getAllMessages() {
		List<Message<Object>> messages = new ArrayList<Message<Object>>(buffer.size());
		
		while (true) {
			Message<Object> message = buffer.poll();
			
			/*
			 * finish if no more messages available
			 */
			if (message == null)
				break;
			
			messages.add(message);
		}

		return ! messages.isEmpty() ? messages : null;
	}

	@Override
	public Object getData() {
		Message<Object> message = getMessage();
		return message != null ? message.getData() : null;
	}

}
