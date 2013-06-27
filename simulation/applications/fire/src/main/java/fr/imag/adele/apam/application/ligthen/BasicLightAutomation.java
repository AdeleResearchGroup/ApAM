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
package fr.imag.adele.apam.application.ligthen;

import java.util.List;

import fr.imag.adele.apam.Instance;
import fr.liglab.adele.icasa.device.light.BinaryLight;
import fr.liglab.adele.icasa.device.presence.PresenceSensor;

/**
 * This class implements a basic automatic lightning service. It  listen to
 * presence detection events in the room and automatically turn on/off all
 * lights in the room
 * 
 * @author vega
 *
 */
public class BasicLightAutomation {

	private List<BinaryLight>	lights;
	private PresenceSensor 		presenceSensor;

	/**
	 * Notification callback for a presence change
	 */
	@SuppressWarnings("unused")
	private void presenceChanged(boolean state) {
		
		List<BinaryLight> boundLights = lights;
		
		if (boundLights == null)
			return;
		
		for(BinaryLight light:boundLights){
			setLightsState(light,state);
		}
	}

	public void bindLight(Instance instance) {
		if (presenceSensor != null) {
			setLightsState((BinaryLight)instance.getServiceObject(),presenceSensor.getSensedPresence());
		}
	}

	public void unBindLight(Instance instance) {
	}

	public static void setLightsState(BinaryLight light, boolean state) {
		light.setPowerStatus(state);
	}
	

}
