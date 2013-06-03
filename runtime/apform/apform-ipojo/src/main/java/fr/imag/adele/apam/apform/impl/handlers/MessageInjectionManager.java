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
package fr.imag.adele.apam.apform.impl.handlers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Callback;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.wireadmin.Consumer;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
import org.osgi.service.wireadmin.WireConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.impl.ApamComponentFactory;
import fr.imag.adele.apam.apform.impl.ApamInstanceManager;
import fr.imag.adele.apam.declarations.RelationInjection;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.message.Message;
import fr.imag.adele.apam.util.ApAMQueue;


/**
 * This handler handles message consumers. It is at the same time a OSGi's
 * WireAdmin consumer and an APAM message consumer, so that it translates
 * message exchanges over APAM wires into concrete message exchanges over
 * WireAdmin wires.
 * 
 * This handler is also a field interceptor that injects itself into all fields
 * used to transparently consume messages. Fields must be declared of type
 * MessageConsumer<D>, and a reference to this handler will be down casted and
 * injected.
 * 
 * This handler is also in charge of triggering lazy resolution, if data is
 * consumed and there is no producer bound to this relation. It also translates
 * APAM notifications for wire creation and deletion into appropriate actions at
 * the WireAdmin level.
 * 
 * @author vega
 * 
 */
public class MessageInjectionManager implements RelationInjectionManager, Consumer {// MessageConsumer<Object>

    Logger logger  = LoggerFactory.getLogger(getClass());
    /**
     * The registered name of this iPojo handler
     */
    public static final String NAME = "consumer";
    
    /**
	 * The source component of the relation
	 */
    private final ApamComponentFactory  component;
    
    /**
     * The associated resolver
     */
    private final ApamInstanceManager   instance;
    
    /**
	 * The relation injection managed by this relation
	 */
    private final RelationInjection injection;
    
    /**
     * The list of target services.
     */
    private final Set<Instance> targetServices;

    /**
	 * The WireAdmin consumer registration. A consumer is registered when the
	 * relation is resolved and automatically unregistered when the relation
	 * gets unresolved.
	 * 
	 */
    private ServiceRegistration consumer;

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
    private final ArrayBlockingQueue<Object> buffer;
    
    /**
     * In case of method callback, an object to allow direct invocation of the instance
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

    /**
     * The field injected in the instance
     */
    private ApAMQueue<Object> fieldBuffer;
    
    
    public MessageInjectionManager(ApamComponentFactory component, ApamInstanceManager instance, RelationInjection injection) throws ConfigurationException {
        
        assert injection.getResource() instanceof MessageReference;
        
        this.component  = component;
        this.instance   = instance;
        this.injection  = injection;
        
        if (injection instanceof RelationInjection.CallbackWithArgument) {
            MethodMetadata callbackMetadata = null;
            String callbackParameterType    = null;
            
            for (MethodMetadata method : this.component.getPojoMetadata().getMethods(injection.getName())) {
                if (method.getMethodArguments().length == 1) {
                    callbackMetadata        = method;
                    callbackParameterType   = method.getMethodArguments()[0];
                }
            }
            
            this.callback           = new Callback(callbackMetadata, instance);
            this.isMessageCallback  = Message.class.getCanonicalName().equals(callbackParameterType);

        }
        else {
            this.callback           = null;
            this.isMessageCallback  = false;
        }
        
        this.targetServices = new HashSet<Instance>();
        this.consumer       = null;

        try {
            this.messageFlavors = new Class<?>[] {this.component.loadClass(injection.getResource().getJavaType())};
            this.consumerId     = NAME+"["+instance.getInstanceName()+","+injection.getRelation().getIdentifier()+","+injection.getName()+"]";
            this.wires          = new HashMap<String,Wire>();
            this.buffer         = new ArrayBlockingQueue<Object>(MAX_BUFFER_SIZE);
            this.fieldBuffer = new ApAMQueue<Object>(buffer);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(e.getLocalizedMessage());
        }
       
        instance.addInjection(this);
    }

    /**
	 * The relation injection associated to this manager
	 */
    @Override
    public RelationInjection getRelationInjection() {
        return injection;
    }
    
    /**
	 * Get an XML representation of the state of this relation
	 */
    @Override
	public Element getDescription() {

        Element consumerDescription = new Element("injection", ApamComponentFactory.APAM_NAMESPACE);
		consumerDescription.addAttribute(new Attribute("relation", injection
				.getRelation().getIdentifier()));
        consumerDescription.addAttribute(new Attribute("target", injection.getRelation().getTarget().toString()));
        consumerDescription.addAttribute(new Attribute("field", injection.getName()));
        consumerDescription.addAttribute(new Attribute("type", injection.getResource().toString()));
        consumerDescription.addAttribute(new Attribute("isAggregate",   Boolean.toString(injection.isCollection())));

        /*
         * show the current state of resolution. To avoid unnecessary synchronization overhead make a copy of the
         * current target services and do not use directly the field that can be concurrently modified
         */
        Set<Wire> resolutions = new HashSet<Wire>();
        synchronized (this) {
            resolutions.addAll(wires.values());
        }

            
        consumerDescription.addAttribute(new Attribute("resolved",Boolean.toString(!resolutions.isEmpty())));
        consumerDescription.addAttribute(new Attribute("consumer.id",consumerId));
        consumerDescription.addAttribute(new Attribute("flavors",Arrays.toString(messageFlavors)));
        consumerDescription.addAttribute(new Attribute("buffered",Integer.toString(buffer.size())));

        for (Wire wire : resolutions) {
            
            Element wireInfo = new Element("wire",ApamComponentFactory.APAM_NAMESPACE);
            wireInfo.addAttribute(new Attribute("producer.id",(String)wire.getProperties().get(WireConstants.WIREADMIN_PRODUCER_PID)));
            wireInfo.addAttribute(new Attribute("flavors",Arrays.toString(wire.getFlavors())));
            consumerDescription.addElement(wireInfo);
        }
        

        
        return consumerDescription;
    
    }
    
    /**
     * Get the reference to an Apform handler associated to an instance
     * 
     * NOTE This performs an unchecked down casts, as it assumes the calling client knows the exact class of the
     * requested handler
     */
    @SuppressWarnings("unchecked")
    private static <T extends Handler> T getHandler(InstanceManager instance, String namespace, String handlerId) {
        String qualifiedHandlerId = namespace+":"+handlerId;
        return (T) instance.getHandler(qualifiedHandlerId);
        
    }
    
    @SuppressWarnings("unchecked")
    private <T extends Handler> T getHandler(String namespace, String handlerId) {
        return (T) getHandler(instance,namespace,handlerId);
    }
    
    /**
     * Handle modification of the injected field
     */
    @Override
	public void onSet(Object pojo, String fieldName, Object value) {
        // Nothing to do, this should never happen as we exclusively handle the field's value
    }

    /**
     * Handle access to the injected field
     */
    @Override
	public Object onGet(Object pojo, String fieldName, Object value) {

        synchronized (this) {
            if (consumer != null)
                return fieldBuffer;
        }

        /*
		 * Ask APAM to resolve the relation. Depending on the application
		 * policies this may throw an error, or block the thread until the
		 * relation is fulfilled, or keep the relation unresolved in the case of
		 * optional dependencies.
		 * 
		 * Resolution has as side-effect a modification of the target services.
		 */ 
        instance.resolve(this);
      
        return consumer !=null ? fieldBuffer : null;
    }

    
    /**
	 * Get access to the wireadmin, via the relationInjectionHandler
	 * 
	 */
    private WireAdmin getWireAdmin() {
        RelationInjectionHandler handler = getHandler(ApamComponentFactory.APAM_NAMESPACE,RelationInjectionHandler.NAME);
        return handler.getWireAdmin();
    }

    /**
     * This implementation of message injection requires that the WireAdmin service be available on the platform
     */
    @Override
    public boolean isValid() {
        WireAdmin wireAdmin = getWireAdmin();
        
        if (wireAdmin == null) {
            System.err.println("The WireAdmin service must be available in the platform to handle message consumer injection \""+injection.toString()+"\"");
        }
        
        return wireAdmin != null;
    }
    
    /**
     * The consumer Id associated to this manager
     */
    public String getConsumerId() {
        return consumerId;
    }
    
    /**
     * The identification of the APAM message producer. It is composed of the identification of the
     * provider and the WireAdmin producer identifier
     * @author vega
     *
     */
    private class MessageProducerIdentifier {
        public final String providerId;
        public final String producerId;
        
        public MessageProducerIdentifier(String providerId, String producerId) {
            this.providerId = providerId;
            this.producerId = producerId;
        }
        
    }
    /**
     * The message producer associated to the given target instance
     * 
     * NOTE This performs an unchecked down cast, as it assumes the target instance  is an Apform-iPojo provided
     * instance
     */
    public MessageProducerIdentifier getMessageProducer(Instance target) {
        MessageProviderHandler providerHandler = getHandler(((ApamInstanceManager.Apform)target.getApformInst()).getManager(),ApamComponentFactory.APAM_NAMESPACE,MessageProviderHandler.NAME);
        return new MessageProducerIdentifier(providerHandler.getProviderId(),providerHandler.getProducerId());
    }
    
     
    /*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.imag.adele.apam.apform.impl.handlers.relationInjectionManager#addTarget
	 * (fr.imag.adele.apam.Instance)
	 */
    @Override
    public void addTarget(Component target) {

    	/*
    	 * Messages can only be exchanged between instances
    	 */
    	if (! (target instanceof Instance))
    		return;
    	
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
                
                MessageProviderHandler providerHandler = getHandler(ApamComponentFactory.APAM_NAMESPACE,MessageProviderHandler.NAME);
                consumer = providerHandler.getHandlerManager().getContext().registerService(Consumer.class.getCanonicalName(), this, properties);
            }
            
            targetServices.add((Instance)target);
            
            /*
             * Create a wire at the WireAdmin level
             */
            WireAdmin wireAdmin = getWireAdmin();
            if (wireAdmin != null) {
                MessageProducerIdentifier messageProducer = getMessageProducer((Instance)target);
                Properties wireProperties = new Properties();
                wireProperties.put(MessageProviderHandler.ATT_PROVIDER_ID, messageProducer.providerId);
                Wire wire = wireAdmin.createWire(messageProducer.producerId, getConsumerId(), wireProperties);
                wires.put(target.getName(),wire);
                
            }
        }

    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.imag.adele.apam.apform.impl.handlers.relationInjectionManager#removeTarget
	 * (fr.imag.adele.apam.Instance)
	 */
    @Override
    public void removeTarget(Component target) {

        /*
         * Remove this target and invalidate cache
         */
        synchronized (this) {

            /*
             * Remove the wire at the WireAdmin level
             */
            WireAdmin wireAdmin = getWireAdmin();
            Wire wire           = wires.remove(target.getName());
            if (wireAdmin != null && wire != null)
                wireAdmin.deleteWire(wire);
            
            targetServices.remove(target);
            
            /*
			 * Unregister the consumer if the relation becomes unresolved
			 */
            if (targetServices.isEmpty()) {
                consumer.unregister();
                consumer = null;
            }
            
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
        
        if (isMessageCallback){
            Message<Object> message = (Message<Object>) value;
            message.markAsReceived(wire.getProperties());
            buffer.offer(message);
        }else {
            Message<Object> message = (Message<Object>) value;
            message.markAsReceived(wire.getProperties());
            buffer.offer(message.getData());
        }
       
        
        /*
         * If a callback is registered consume message directly and push it to the component
         */
        if (callback != null) {
            try {
                Object consumed  = buffer.poll(); 
                if (consumed != null) {
                    if (isMessageCallback)
                        callback.call(new Object[] {(Message<?>)consumed});
                    else
                        callback.call(new Object[] {consumed});
                }
            } catch (Exception e) {
                System.err.println("error invoking callbaack "+e);
            }   
        }
    }

    @Override
    public void producersConnected(Wire[] newWires) {
        /*
		 * The APAM relation handler only manages wires created indirectly by
		 * mapping APAM resolution into WireAdmin events. Those wires are
		 * already tracked by this manager, so we can ignore asynchronous
		 * notifications
		 */
    }

//    /**
//     * Retrieves the first message in the buffer if available
//     */
//    @Override
//    public Message pullMessage() {
//        return buffer.poll();
//    }
//
//    @Override
//    public List<Message<Object>> getAllMessages() {
//        List<Message<Object>> messages = new ArrayList<Message<Object>>(buffer.size());
//        
//        while (true) {
//            Message<Object> message = buffer.poll();
//            
//            /*
//             * finish if no more messages available
//             */
//            if (message == null)
//                break;
//            
//            messages.add(message);
//        }
//
//        return ! messages.isEmpty() ? messages : null;
//    }
//
//    @Override
//    public Object pull() {
//        Message<Object> message = pullMessage();
//        return message != null ? message.getData() : null;
//    }

}