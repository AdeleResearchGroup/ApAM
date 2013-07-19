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
 * Heater.java - 8 juil. 2013
 */
package fr.imag.adele.apam.tutorials.temperature.devices;

/**
 * Very simple Heater Device Definition.
 * It does not contains any temperature regulator.
 */
public interface Heater {

	/**
	 * Turns on the heater (whatever previous status)
	 */
	public void turnOn();
	
	/**
	 * Turns off the heater (whatever previous status)
	 */
	public void turnOff();
	
	/**
	 * @return true if heater is currently On,
	 * false otherwise
	 */
	public boolean getStatus();
	
}
