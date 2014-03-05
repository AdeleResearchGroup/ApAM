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
package fr.imag.adele.apam.application.fire;

import java.util.Collections;
import java.util.List;

import org.json.JSONException;

import appsgate.lig.button_switch.sensor.messages.SwitchNotificationMsg;
import appsgate.lig.core.object.messages.NotificationMsg;
import fr.liglab.adele.apam.device.access.Lock;
import fr.liglab.adele.apam.device.fire.EmergencyEvent;

/**
 * This class implements simple test application that unlock the doors in
 * case of fire
 * 
 * @author vega
 *
 */
public class SimpleFireEvacuation {

	private List<Lock>		doors;

	/**
	 * This is the notification method used to signal a change in
	 * the fire detection status
	 */
	private EmergencyEvent fireDetected(boolean fire) {
		return new EmergencyEvent(fire);
	}
	
	/**
	 * Notification callback for a smoke detection message
	 */
	private void smokeDetected(boolean smokeDetected) {
		
		/*
		 * calculate emergency state. 
		 * 
		 * In this toy example, simply use the smoke detection value.
		 * 
		 * Notify event to allow conflict arbitration before accessing devices
		 */
		
		fireDetected(smokeDetected);

		/*
		 * Disable doors access control to allow evacuation when smoke detected
		 */
				
		for(Lock door: optional(doors)) {
			if (smokeDetected)
				door.disableAuthorization(false);
			else
				door.enableAuthorization();
		}
		
	}

	private boolean toogle = false;
	
	@SuppressWarnings("unused")
	private void simulationNotification(NotificationMsg notification) {
		try {
			
			SwitchNotificationMsg switchNotification = (SwitchNotificationMsg) notification;
			boolean triggered = switchNotification.isOn() && switchNotification.JSONize().get("varName").equals("buttonStatus");
			if (triggered) {
				toogle = ! toogle;
				smokeDetected(toogle);
			}
		
		} catch (JSONException ignored) {
		}
		
		
	}

	private static <T> List<T> optional(List<T> list) {
		return list != null ? list : Collections.<T> emptyList();
	}

}
