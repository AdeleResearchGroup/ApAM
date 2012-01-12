package fr.imag.adele.apam.apform.message.impl;

import java.lang.reflect.TypeVariable;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.FieldInterceptor;

import fr.imag.adele.apam.apformipojo.Dependency.Resolver;
import fr.imag.adele.apam.message.AbstractConsumer;
import fr.imag.adele.apam.message.Message;
import fr.imag.adele.apam.message.Strategy;

public class MessageAbstractConsumer<D> implements AbstractConsumer<D>, FieldInterceptor  {
		
	/**
	 * The factory of the source component of the dependency
	 */
	private final ComponentFactory factory;

	/**
	 * The associated resolver
	 */
	private final Resolver resolver;

	/**
	 * the flavor of the message
	 */
	private final String flavor;

	/**
	 * The strategy of this producer
	 */
	private Strategy strategy;

	/**
	 * the wire Admin producer
	 */
	private WAProducerImpl wa_producer;

	/**
	 * 
	 */
	private String field;
	
	
	public MessageAbstractConsumer(ComponentFactory factory, Resolver resolver, String flavor, Strategy strategy,String field) {
		this.factory = factory;
		this.resolver = resolver;
		this.flavor = flavor;
		this.strategy = strategy;
		this.field= field;
	}

	@Override
	public void onSet(Object pojo, String fieldName, Object value) {
		// This action is not authorized
	}

	@Override
	public Object onGet(Object pojo, String fieldName, Object value) {
		return this;
	}

	@Override
	public void sendMessage(Message<D> message) {
		TypeVariable[] typeVrial = message.getClass().getTypeParameters();
		if (typeVrial!=null && typeVrial.length==1){
			String type = typeVrial[0].getClass().getCanonicalName();
			wa_producer.sendMessage(type, message);
		}else {
			System.err.println("No Type Parameter was specified for the message sent by  " + field);
		}
	}

	@Override
	public void sendData(D data) {
		wa_producer.sendMessage(data.getClass().getCanonicalName(), data);
	}

	
	
}
