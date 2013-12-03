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
	private D data;
	private Properties properties;

	public Message(D value) {
		this.data = value;
		this.properties = new Properties();
		this.sendTimeStamp = -1L;
		this.receiveTimeStamp = -1L;
	}

	public String getConsumerPID() {
		return (String) properties.get(WireConstants.WIREADMIN_CONSUMER_PID);
	}

	public D getData() {
		return data;
	}

	public String getProducerPID() {
		return (String) properties.get(WireConstants.WIREADMIN_PRODUCER_PID);
	}

	public Properties getProperties() {
		return properties;
	}

	public long getReceiveTimeStamp() {
		return receiveTimeStamp;
	}

	public long getSendTimeStamp() {
		return sendTimeStamp;
	}

	public String getWireAdminPID() {
		return (String) properties.get(WireConstants.WIREADMIN_PID);
	}

	public void markAsReceived(Dictionary<Object, Object> wireProperties) {
		this.receiveTimeStamp = new Date().getTime();
		for (Enumeration<Object> keys = wireProperties.keys(); keys.hasMoreElements();) {
			Object key = keys.nextElement();
			Object value = wireProperties.get(key);
			this.properties.put(key, value);
		}
	}

	public void markAsSent() {
		this.sendTimeStamp = new Date().getTime();
	}

}
