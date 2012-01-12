/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.imag.adele.apam.apform.message.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.util.Callback;
import org.osgi.service.wireadmin.Wire;

import fr.imag.adele.apam.apformipojo.Dependency.Resolver;
import fr.imag.adele.apam.message.AbstractProducer;
import fr.imag.adele.apam.message.Message;
import fr.imag.adele.apam.message.Strategy;

public class MessageAbstractProducer<D> implements AbstractProducer<D>, FieldInterceptor {

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
	 * buffer of the messages
	 */
	private List<Message<D>> messages;

	/**
	 * The strategy of this consumer
	 */
	private Strategy strategy;

	/**
	 * The callback method for the push consumer
	 */
	private Callback callback;

	public MessageAbstractProducer(ComponentFactory factory, Resolver resolver, String flavor,Strategy strategy) {
		this.factory = factory;
		this.resolver = resolver;
		this.flavor = flavor;
		this.strategy = strategy;
	}

	public MessageAbstractProducer(ComponentFactory factory, Resolver resolver, String flavor,Strategy strategy, Callback callback) {
		this.factory = factory;
		this.resolver = resolver;
		this.flavor = flavor;
		this.strategy = strategy;
		this.callback = callback;
	}
	
	@Override
	public D getData() {
		Message<D> message = getMessage();
		if (message != null) {
			return message.getData();
		}
		return null;
	}

	public Message<D> getMessage() {
		if (messages!=null && !messages.isEmpty()){
			Message<D> message = messages.remove(0);
			return message;
		}
		return null;
	}

	public List<Message<D>> getAllMessages() {
		List<Message<D>> returnedList ;
		synchronized (messages) {
			returnedList = new ArrayList<Message<D>>(messages);
			messages.clear();
		}
		return returnedList;
	}

	@Override
	public void onSet(Object pojo, String fieldName, Object value) {
		// This action is not authorized
	}

	@Override
	public Object onGet(Object pojo, String fieldName, Object value) {
		return this;
	}

	public void addMessage(Wire wire, Message<D> message) {
		switch (strategy){
		case pull:
			if (messages == null) {
				messages = new ArrayList<Message<D>>();
			}
			messages.add(message);
			break;
			
		case push:
			try{
				Object[] arg = {message};
				callback.call(arg);
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			break;
		}
			
	}

}
