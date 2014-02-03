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
package fr.imag.adele.apam.application.lock;

import java.util.Collections;
import java.util.List;

import org.json.JSONException;

import appsgate.lig.button_switch.sensor.messages.SwitchNotificationMsg;
import appsgate.lig.core.object.messages.NotificationMsg;
import appsgate.lig.smartplug.actuator_sensor.spec.CoreSmartPlugSpec;
import fr.imag.adele.apam.Instance;
import fr.liglab.adele.apam.device.access.Lock;

/**
 * This class implements a simplistic test application that lock doors and turns off plugs
 * when a trigger is activated
 * 
 * @author vega
 *
 */
public class LockHomeAutomation {


	private Instance 					instance;
	private boolean 					activated;

	private List<Lock>					doors;
	private List<CoreSmartPlugSpec>		plugs;

	@SuppressWarnings("unused")
	private void start(Instance instance) {
		this.instance = instance;
	} 

	/**
	 * Notification callback to toggle the state of the application
	 */
	private void toggleActivation() {
		
		/*
		 * toggle state, notify state change before accessing devices to allow
		 * conflict arbitration 
		 */
		activated = !activated;
		instance.getComposite().setProperty("locked",activated);

		/* 
		 * lock/unlock doors on activation
		 * 
		 */
		for (Lock door: optional(doors)){
			if (activated)
				door.disableAcces(true);
			else
				door.enableAcces();
		}
			
		
		/* 
		 * switch power on/off
		 * 
		 */
		for (CoreSmartPlugSpec plug: optional(plugs)){
			if (activated)
				plug.off();
			else
				plug.on();
		}
		
		
	}

	@SuppressWarnings("unused")
	private void simulationNotification(NotificationMsg notification) {
		
		try {
			
			SwitchNotificationMsg switchNotification = (SwitchNotificationMsg) notification;
			boolean triggered = switchNotification.isOn() && switchNotification.JSONize().get("varName").equals("buttonStatus");
			if (triggered)
				toggleActivation();
		
		} catch (JSONException ignored) {
		}
		
		
	}

	private static <T> List<T> optional(List<T> list) {
		return list != null ? list : Collections.<T> emptyList();
	}
}
