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
package fr.imag.adele.apam.application.room.impl;

import java.util.List;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.application.room.Room;
import fr.liglab.adele.icasa.device.DeviceListener;
import fr.liglab.adele.icasa.device.GenericDevice;
import fr.liglab.adele.icasa.device.light.BinaryLight;
import fr.liglab.adele.icasa.device.presence.PresenceSensor;

public class RoomImpl implements Room, DeviceListener {

	private List<BinaryLight> lights;// =new ArrayList<BinaryLight>();

	private PresenceSensor presenceSensor;

	//private SimulationManager manager;

	// private MyPresenceListener presenceListener = new MyPresenceListener();

	private boolean presence = false;

	public void start() {

		System.out.println("Start OK!");
		if (presenceSensor != null) {
			System.out.println("Presence OK!");
			// presenceSensor.addListener(presenceListener);
			presence = presenceSensor.getSensedPresence();
			setLightsStates(presence);
		}
	}

	public void stop() {
		System.out.println("Stop OK!");
		// if (presenceSensor!=null){
		// presenceSensor.removeListener(presenceListener);
		// }
	}

	public void bindPresence(Instance instance) {

		System.out.println("New instance bind " + instance.getName());
		// presenceSensor.removeListener(presenceListener);
		presenceSensor.addListener(this);
		presence = presenceSensor.getSensedPresence();
		setLightsStates(presence);
	}

	public void unBindPresence(Instance instance) {
		System.out.println("Instance unbind " + instance.getName());
		
		PresenceSensor presence=(PresenceSensor)instance.getServiceObject();
		
		presence.removeListener(this);
		
	}

	public void bindLight(Instance instance) {
		System.out.println("Light:New instance bind " + instance.getName());
		setLightsStates(presence);
	}

	public void unBindLight(Instance instance) {
		System.out.println("Light:Instance unbind " + instance.getName());
	}

	public void setLightsStates(boolean state) {
		if (lights != null) {
			System.out.println("lights going to " + state);
			for (BinaryLight light : lights) {
				light.setPowerStatus(presence);
			}
		}
	}

	@Override
	public void deviceAdded(GenericDevice device) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deviceRemoved(GenericDevice device) {
		// TODO Auto-generated method stub

	}

	@Override
	public void devicePropertyModified(GenericDevice device,
			String propertyName, Object oldValue) {
		// TODO Auto-generated method stub
		System.out.println("Device property from device:"+device);
		System.out.println("Device property name modified:"+propertyName);
		System.out.println("Device property old value modified:"+oldValue);
		
		if(presenceSensor != null 
				&& device.equals(presenceSensor) 
				&& propertyName.equals(PresenceSensor.PRESENCE_SENSOR_SENSED_PRESENCE)){
			presence = presenceSensor.getSensedPresence();
			setLightsStates(presence);
			System.out.println("Presence sense " + propertyName);
			
		}
	}

	@Override
	public void devicePropertyAdded(GenericDevice device, String propertyName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void devicePropertyRemoved(GenericDevice device, String propertyName) {
		// TODO Auto-generated method stub

	}

}
