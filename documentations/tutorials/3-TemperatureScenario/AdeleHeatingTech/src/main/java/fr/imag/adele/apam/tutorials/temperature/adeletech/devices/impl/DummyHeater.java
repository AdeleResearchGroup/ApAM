/**
 * Copyright 2011-2013 Universite Joseph Fourier, LIG, ADELE team
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
 *
 * DummyHeater.java - 8 juil. 2013
 */
package fr.imag.adele.apam.tutorials.temperature.adeletech.devices.impl;

import fr.imag.adele.apam.tutorials.temperature.devices.Heater;

/**
 * @author thibaud
 *
 */
public class DummyHeater implements Heater{
	
	private String myName;
	
	private boolean status;
	
	/**
	 * Callback method upon Init
	 */
	public void start() {
		System.out.println("Heater : "+myName+".start(), default status is off");		
		status = false;
	}
	
	/**
	 * Callback method
	 */
	public void stop() {
		System.out.println("Heater : "+myName+".stop()");		
	}
	

	/* (non-Javadoc)
	 * @see fr.imag.adele.apam.tutorials.temperature.devices.Heater#turnOn()
	 */
	public void turnOn() {
		System.out.println("Heater : "+myName+".turnOn()");
		status=true;
	}

	/* (non-Javadoc)
	 * @see fr.imag.adele.apam.tutorials.temperature.devices.Heater#turnOff()
	 */
	public void turnOff() {
		System.out.println("Heater : "+myName+".turnOff()");
		status = false;
	}

	/* (non-Javadoc)
	 * @see fr.imag.adele.apam.tutorials.temperature.devices.Heater#getStatus()
	 */
	public boolean getStatus() {
		System.out.println("Heater : "+myName+".getStatus(), returning "+status);		
		return status;
	}

}
