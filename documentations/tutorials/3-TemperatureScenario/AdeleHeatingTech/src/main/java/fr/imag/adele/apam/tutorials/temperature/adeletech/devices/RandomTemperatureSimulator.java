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
 * RandomTemperatureSimulator.java - 8 juil. 2013
 */
package fr.imag.adele.apam.tutorials.temperature.adeletech.devices;

import fr.imag.adele.apam.tutorials.temperature.devices.TemperatureSensor;

/**
 * @author thibaud
 *
 */
public class RandomTemperatureSimulator implements TemperatureSensor {
	
	private String myName;
	
	/**
	 * Callback method upon Init
	 */
	public void start() {
		System.out.println("RandomTemperatureSimulator : "+myName+".start()");		
	}
	
	/**
	 * Callback method
	 */
	public void stop() {
		System.out.println("RandomTemperatureSimulator : "+myName+".stop()");		
	}


	/* (non-Javadoc)
	 * @see fr.imag.adele.apam.tutorials.temperature.devices.TemperatureSensor#getCurrentTemperature()
	 */
	public int getCurrentTemperature() {
		System.out.println("RandomTemperatureSimulator : "+myName+".getCurrentTemperature()");
		// dummy random value
		return (int)(Math.random() * (20)) + 12;
	}

}
