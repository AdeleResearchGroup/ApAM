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

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.MethodInterceptor;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.osgi.service.wireadmin.Producer;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdmin;
import org.osgi.service.wireadmin.WireConstants;

import fr.imag.adele.apam.apform.impl.ApamAtomicComponentFactory;
import fr.imag.adele.apam.apform.impl.ApamComponentFactory;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.declarations.ProviderInstrumentation;
import fr.imag.adele.apam.message.Message;
import fr.imag.adele.apam.util.Util;


/**
 * This handler handles message production. It is at the same time a OSGi's WireAdmin producer and an APAM
 * message producer, so that it translates message exchanges over APAM wires into concrete message exchanges
 * over WireAdmin wires.
 * 
 *   
 * @author vega
 *
 */
public class MessageProviderHandler extends ApformHandler implements Producer, MethodInterceptor {


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
	 * Represent the producer persistent id
	 */
	private String producerId;

	/**
	 * Represent the producer execution session id
	 */
	private String sessionId;

	/**
	 * The name of the property used to tag wires with the producer session
	 */
	private final static String ATT_SESSION_ID = "session.id";
	
	/**
	 * A reference to the WireAdmin
	 */
	private WireAdmin wireAdmin;
	
    /**
     * The list of connected consumers, indexed by consumer identification
     */
    private final Map<String,Wire> wires;
	
    public MessageProviderHandler() {
    	this.wires = new HashMap<String, Wire>();
	}
    
	/**
	 * Whether this handler is required for the specified configuration
	 */
	public static boolean isRequired(AtomicImplementationDeclaration componentDeclaration) {
		
    	for (ProviderInstrumentation providerInstrumentation : componentDeclaration.getProviderInstrumentation()) {
    		if (providerInstrumentation instanceof ProviderInstrumentation.MessageProviderMethodInterception)
    			return true;
    	}
    	
    	return false;
	}
	
    /**
     * (non-Javadoc)
     * 
     * @see
     * org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void configure(Element componentMetadata, Dictionary configuration) throws ConfigurationException {

    	if (!(getFactory() instanceof ApamAtomicComponentFactory)) {
    		isRegisteredProducer = false;
    		return;
    	}

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
    	

    	producerId 				= NAME+"["+getInstanceManager().getInstanceName()+"]";
    	sessionId				= Long.toString(System.currentTimeMillis());

    	wires.clear();

    	ApamAtomicComponentFactory implementation	= (ApamAtomicComponentFactory) getFactory();
    	ImplementationDeclaration declaration		= implementation.getDeclaration();
    	
    	if (! (declaration instanceof AtomicImplementationDeclaration))
    		return;
    	
    	/*
    	 * Register instrumentation for message push at the provider side
    	 */
    	PojoMetadata manipulation 					= getFactory().getPojoMetadata();
    	AtomicImplementationDeclaration primitive	= (AtomicImplementationDeclaration) declaration;
    	for (ProviderInstrumentation providerInstrumentation : primitive.getProviderInstrumentation()) {
    		
    	    MessageReference messageReference = providerInstrumentation.getProvidedResource().as(MessageReference.class);
    		if (messageReference == null)
    			continue;

    		if (! (providerInstrumentation instanceof ProviderInstrumentation.MessageProviderMethodInterception))
    			continue;

    		ProviderInstrumentation.MessageProviderMethodInterception interception = 
    				(ProviderInstrumentation.MessageProviderMethodInterception) providerInstrumentation;
    			
			/*
			 * Search for the specified method to intercept, we always look for a perfect match of the 
			 * specified signature, and do not allow ambiguous method names
			 */
			
			MethodMetadata candidate = null;
			for (MethodMetadata method :  manipulation.getMethods(interception.getMethodName())) {
				
				if (interception.getMethodSignature() == null) {
					candidate = method;
					break;
				}
				
				String signature[]	= Util.split(interception.getMethodSignature());
				String arguments[]	= method.getMethodArguments();
				boolean match 		= (signature.length == arguments.length);

				for (int i = 0; match && i < signature.length; i++) {
					if (!signature[i].equals(arguments[i]))
						match = false;
				}
				
				match = match && method.getMethodReturn().equals(messageReference.getJavaType());
				if (match) {
					candidate = method;
					break;
				}
			}
    		
			if (candidate != null) {
				getInstanceManager().register(candidate,this);
				continue;
			}
			
			throw new ConfigurationException("Message producer intercepted methdod not found "+interception.getMethodName()+
									"("+interception.getMethodSignature() != null ? interception.getMethodSignature(): ""+")");
    	}
    	
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
			info.addAttribute(new Attribute("session.id",sessionId));
			info.addAttribute(new Attribute("flavors",Arrays.toString(messageFlavors)));
			info.addAttribute(new Attribute("isRegistered",Boolean.toString(isRegisteredProducer)));
	
	        /*
	         * show the current state of resolution. To avoid unnecessary synchronization overhead make a copy of the
	         * current target services and do not use directly the field that can be concurrently modified
	         */
	        List<Wire> resolutions = new ArrayList<Wire>();
	        synchronized (this) {
	            resolutions.addAll(wires.values());
	        }

			for (Wire wire : resolutions) {
				Element wireInfo = new Element("wire",ApamComponentFactory.APAM_NAMESPACE);
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
     * Creates a new wire, at the wire admin level, towards the specified consumer
     */
    public Wire createWire(MessageInjectionManager consumer) {
        if (wireAdmin != null ) {
            Properties wireProperties = new Properties();
            wireProperties.put(MessageProviderHandler.ATT_SESSION_ID, sessionId);
            
            Wire wire = wireAdmin.createWire(this.producerId, consumer.getConsumerId(), wireProperties);
            
            synchronized (this) {
                wires.put(consumer.getInstance().getDeclaration().getName(),wire);
			}
            
            return wire;
        }
    	
        return null;
    }
    
    /**
     * Deletes an existing wire, at the wire admin level, towards the specified consumer
     */
    public Wire deleteWire(MessageInjectionManager consumer) {

        Wire wire = null;
        synchronized (this) {
        	wire = wires.remove(consumer.getInstance().getDeclaration().getName());
		}

        /*
         * Remove the wire at the WireAdmin level
         */
    	if (wireAdmin != null && wire != null) {
                wireAdmin.deleteWire(wire);
    	}
        	
    	return wire;
    }

    
	/**
	 * The APAM relation handler only manages wires created indirectly by mapping APAM resolution
	 * into WireAdmin events. Those wires are already tracked by this manager, so we do not need
	 * notifications.
	 * 
	 * However, because WireAdmin persists wires, we can get notified of wires from previous executions.
	 * APAM wires on the other hand are not persistent across executions, so we try to avoid duplicates
	 * and do some basic garbage collection here.
	 *  
	 * We use a session id associated with this handler as a tag of wires created in this execution
	 * 
	 */
	@Override
	public void consumersConnected(Wire[] newWires) {

		if (newWires == null || newWires.length == 0)
			return;

		/*
		 *  NOTE The APAM message provider handler only manages wires created indirectly by mapping APAM
		 * 	resolution into WireAdmin events, so we can assume session ids are unique.
		 */
		for (Wire wire : newWires) {
			
			String wireSessionId = (String) wire.getProperties().get(ATT_SESSION_ID);
			
			// delete old wires or not apam wires
			if (wireSessionId == null || ! wireSessionId.equals(sessionId)) {
				if (wireAdmin != null) wireAdmin.deleteWire(wire);
				continue;
			}
			
		}
	}


	/**
	 * Get the return value of the intercepted method and automatically push the message
	 *  
	 */
	@Override
	public void onExit(Object pojo, Member method, Object returnedObj) {
	    super.onExit(pojo, method, returnedObj);
	    if (returnedObj instanceof Message){
	        push((Message<?>)returnedObj);
	    }else {
	    	push(new Message<Object>(returnedObj));
	    }
	}

	/**
	 * Push a message to all registered consumers
	 */
	private void push(Message<?> message) {
		
	    
		if (message.getData() == null)
			return;
	     
        /*
         * Create a local copy of the current list of wires to avoid synchronization
         * problems
         */
        
        List<Wire> currentWires = new ArrayList<Wire>(); 
        synchronized (this) {
            currentWires.addAll(wires.values());
        }
		
        /*
         * broadcast to all wires
         */
		for (Wire wire : currentWires) {

	          if (!wire.isConnected())
	              continue;

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


    /**
     * This WireAdmin producer can not be polled, it works only in push mode.
     */
	@Override
	public Object polled(Wire wire) {
		throw new UnsupportedOperationException("APAM Messages do not support polling");
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}
	

}
