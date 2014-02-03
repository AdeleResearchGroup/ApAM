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
package fr.imag.adele.apam.device.smoke.impl;



import appsgate.lig.button_switch.sensor.messages.SwitchNotificationMsg;
import appsgate.lig.core.object.messages.NotificationMsg;
import fr.liglab.adele.apam.device.fire.SmokeDetector;

/**
 * Implementation of a simulated Oven device.
 *
 */

public class SimulatedSmokeDetector implements SmokeDetector { 


    protected boolean onFire;
    
    public SimulatedSmokeDetector(){
		super();

		onFire = false;
    }

	@SuppressWarnings("unused")
	private boolean switchStatusChanged(NotificationMsg switchChanged) {
		onFire = ((SwitchNotificationMsg)switchChanged).isOn();
		return onFire;
	}
	

}
