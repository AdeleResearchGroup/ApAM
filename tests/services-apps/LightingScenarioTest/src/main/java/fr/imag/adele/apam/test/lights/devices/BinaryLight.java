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
 * BinaryLight.java - 2 juil. 2013
 */
package fr.imag.adele.apam.test.lights.devices;

import fr.imag.adele.apam.test.lights.devices.messages.ButtonPressed;
import fr.imag.adele.apam.test.lights.devices.messages.LightStatusChanged;

/**
 * @author thibaud
 * Very simple light that can be on or off
 * (freely inspired from the upnp device, but not compliant)
 */
public interface BinaryLight  extends Device {
	
	/**
	 * @return true if light is on
	 */
	public boolean isLightOn();
	
	/**
	 * Set the light on or off
	 * @param newStatus true mean to set the light on, false to put it off
	 * (whatever was the previous status)
	 */
	public void setLightStatus(boolean newStatus);
	
	/**
	 * Change the light status on <-> off depending on the previous status
	 */
	public void switchLightStatus();
	
	/**
	 * This indicate that the light status has changed (on or off)
	 * this is a simple message producer
	 * @return a LightStatusChanged object (which is a message).
	 * Use with the method isLightOn() on this object
	 */
	public LightStatusChanged fireLightStatus();
	
}
