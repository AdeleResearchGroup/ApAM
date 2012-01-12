/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.imag.adele.apam.message;

import java.io.Serializable;
import java.util.Dictionary;
import java.util.Properties;

import org.osgi.service.wireadmin.WireConstants;

public class Message<D> implements Serializable {

	private static final long serialVersionUID = -5521385171858034995L;

	private D data;

	private Long sendTimeStamp;

	private Properties properties;

	private String dataType;

	private Long receiveTimeStamp;

	@SuppressWarnings("unchecked")
	public Message(Long sendTimeStamp, Object value){
		properties = new Properties();
		data = (D) value;
		this.sendTimeStamp = sendTimeStamp;
	}

	public D getData() {
		return data;
	}

	public Long getSendTimeStamp() {
		return sendTimeStamp;
	}
	
	public Long getReceiveTimeStamp() {
		return receiveTimeStamp;
	}

	public void setReceiveTimeStamp(Long receivedTime) {
		 this.receiveTimeStamp =receivedTime ;
	}

	public void addProperties(Dictionary properties) {
		this.properties.putAll(this.properties);
	}
	
	public void addProperties(Properties properties) {
		this.properties.putAll(this.properties);
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

	//Have to do more test to verify if the value is an instanceof D
//	public D getPreviousValue() {
//		return (D) properties.get(WireConstants.WIREVALUE_PREVIOUS);
//	}

	public Object getElapsedTime() {
		return properties.get(WireConstants.WIREVALUE_ELAPSED);
	}

	public Properties getProperties() {
		return properties;
	}

	public String getDataType() {
		return data.getClass().getCanonicalName();
	}


	
	
}
