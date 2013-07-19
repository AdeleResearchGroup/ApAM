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
 * SimpleButton.java - 2 juil. 2013
 */
package fr.imag.adele.apam.tutorials.lights.devices;

import fr.imag.adele.apam.tutorials.lights.devices.messages.ButtonPressed;

/**
 * @author thibaud
 *
 */
public interface SimpleButton extends Device {
	
	/**
	 * This indicate that the button has been pressed,
	 * this is a simple message producer
	 * @return a simple ButtonPressed object (which is a message)
	 */
	public ButtonPressed pressButton();

}
