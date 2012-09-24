/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.imag.adele.apam.message;

import java.io.Serializable;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import org.osgi.service.wireadmin.WireConstants;

public class Message<D> implements Serializable {

	private static final long serialVersionUID = -5521385171858034995L;


	/**
	 * The time stamps for producer and consumer
	 */
	private long sendTimeStamp;
	private long receiveTimeStamp;

	/**
	 * The message payload and its associated description
	 */
	private D 			data;
	private Properties 	properties;


	public Message(D value){
		this.data 				= value;
		this.properties 		= new Properties();
		this.sendTimeStamp		= -1L;
		this.receiveTimeStamp	= -1L;
	}

	public D getData() {
		return data;
	}
	
	public void markAsSent() {
		this.sendTimeStamp = new Date().getTime();
	}
	
	public void markAsReceived(Dictionary<Object,Object> wireProperties) {
		this.receiveTimeStamp = new Date().getTime();
		for (Enumeration<Object>  keys = wireProperties.keys(); keys.hasMoreElements();) {
			Object key		=  keys.nextElement();
			Object value 	= wireProperties.get(key);
			this.properties.put(key, value);
		}
	}
	
	public long getSendTimeStamp() {
		return sendTimeStamp;
	}
	
	public long getReceiveTimeStamp() {
		return receiveTimeStamp;
	}

	public String getWireAdminPID() {
		return  (String) properties.get(WireConstants.WIREADMIN_PID);
	}

	public String getProducerPID() {
		return (String) properties.get(WireConstants.WIREADMIN_PRODUCER_PID);
	}

	public String getConsumerPID() {
		return (String) properties.get(WireConstants.WIREADMIN_CONSUMER_PID);
	}

	public Properties getProperties() {
		return properties;
	}

	
}
