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
import fr.liglab.adele.icasa.device.DeviceListener;
import fr.liglab.adele.icasa.device.GenericDevice;
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
public class BasicLightAutomation implements DeviceListener<GenericDevice> {

	private List<BinaryLight>	lights;
	private PresenceSensor 		presenceSensor;

	public void start() {

		if (presenceSensor != null) {
			presenceSensor.addListener(this);
		}
	}

	public void stop() {

		 if (presenceSensor!=null){
			 presenceSensor.removeListener(this);
		 }
	}

	public void bindPresence(Instance instance) {

		System.out.println("New instance bind " + instance.getName());

		presenceSensor.addListener(this);
		
		setLightsStates(presenceSensor.getSensedPresence());
		
	}

	public void unBindPresence(Instance instance) {
		System.out.println("Instance unbind " + instance.getName());
		PresenceSensor presence=(PresenceSensor)instance.getServiceObject();
		presence.setPropertyValue(PresenceSensor.PRESENCE_SENSOR_SENSED_PRESENCE, false);
		presence.removeListener(this);
		
	}

	public void bindLight(Instance instance) {
		System.out.println("Light:New instance bind " + instance.getName());
		setLightsStates((BinaryLight)instance.getServiceObject(),presenceSensor.getSensedPresence());
	}

	public void unBindLight(Instance instance) {
		System.out.println("Light:Instance unbind " + instance.getName());
		setLightsStates((BinaryLight)instance.getServiceObject(),false);
		
	}

	public void setLightsStates(BinaryLight light, boolean state) {
		
		if (light != null && presenceSensor!=null) {
			System.out.println("lights going to " + state);
			light.setPowerStatus(presenceSensor.getSensedPresence());
			light.setPropertyValue(BinaryLight.BINARY_LIGHT_POWER_STATUS, presenceSensor.getSensedPresence());
		}else if (light != null){
			light.setPowerStatus(false);
		}else {
			System.out.println("----- no light or presence sensor found");
		}
			
	}
	
	public void setLightsStates(boolean state) {
		for(BinaryLight light:lights){
			setLightsStates(light,state);
		}
	}

	@Override
	public void deviceAdded(GenericDevice device) {}

	@Override
	public void deviceRemoved(GenericDevice device) {}

	@Override
	public void devicePropertyModified(GenericDevice device, String propertyName, Object oldValue, Object newValue) {
		System.out.println("Device property from device:"+device);
		System.out.println("Device property name modified:"+propertyName);
		System.out.println("Device property old value modified:"+oldValue);
		
		if(presenceSensor != null 
				&& device.equals(presenceSensor) 
				&& propertyName.equals(PresenceSensor.PRESENCE_SENSOR_SENSED_PRESENCE)){
			
			setLightsStates(presenceSensor.getSensedPresence());
			System.out.println("Presence sense " + propertyName);
			
		}
	}

	@Override
	public void devicePropertyAdded(GenericDevice device, String propertyName) {
	}

	@Override
	public void devicePropertyRemoved(GenericDevice device, String propertyName) {
	}

}
