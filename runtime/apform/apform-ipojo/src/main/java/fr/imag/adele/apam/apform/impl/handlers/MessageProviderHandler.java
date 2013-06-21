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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import fr.imag.adele.apam.apform.impl.ApamComponentFactory;
import fr.imag.adele.apam.apform.impl.ApamAtomicComponentFactory;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.ProviderInstrumentation;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.message.Message;
import fr.imag.adele.apam.message.MessageProducer;
import fr.imag.adele.apam.util.Util;


/**
 * This handler handles message production. It is at the same time a OSGi's WireAdmin producer and an APAM
 * message producer, so that it translates message exchanges over APAM wires into concrete message exchanges
 * over WireAdmin wires.
 * 
 * This handler is also a field interceptor that injects itself into all fields used to transparently produce
 * messages. Fields must be declared of type MessageProducer<D>, and a reference to this handler will be
 * down casted and injected.
 *   
 * @author vega
 *
 */
public class MessageProviderHandler extends ApformHandler implements Producer, MessageProducer<Object>, MethodInterceptor {


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

    	if (!(getFactory() instanceof ApamAtomicComponentFactory))
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
     * This WireAdmin producer can not be polled, it works only in push mode.
     */
	@Override
	public Object polled(Wire wire) {
		return null;
	}

	@Override
	public synchronized void consumersConnected(Wire[] newWires) {
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
	public void push(Object data, Map<String,Object> properties) {
		Message<Object> m = new Message<Object>(data);
		m.getProperties().putAll(properties);
		push(m);
	}

	@Override
	public void push(Object data) {
		push(new Message<Object>(data));
	}

	private void push(Message<Object> message) {
		
	    
		if (message.getData() == null)
			return;
	     
        /*
         * Create a local copy of the current list of wire sto avoid synchronization
         * problems
         */
        
        List<Wire> currentWires = new ArrayList<Wire>(); 
        synchronized (this) {
            currentWires.addAll(wires);
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


	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}
	
	@Override
	public void onExit(Object pojo, Method method, Object returnedObj) {
	    super.onExit(pojo, method, returnedObj);
	    if (returnedObj instanceof Message){
	        push((Message<?>)returnedObj);
	    }else {
	        push(returnedObj);
	    }
	}

}
