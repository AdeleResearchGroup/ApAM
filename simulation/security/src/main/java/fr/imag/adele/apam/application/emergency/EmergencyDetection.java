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
package fr.imag.adele.apam.application.emergency;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.PropertyManager;
//import fr.liglab.adele.apam.device.fire.EmergencyEvent;

/**
 * This class keeps track of the current state of the fire management domain.
 * 
 * Applications in this domain may signal an emergency by sending an EmergencyEvent message
 * 
 * @author vega
 *
 */
public class EmergencyDetection implements PropertyManager {

	/**
	 * The identified emergency states in the domain
	 */
	public enum State {
		
		NORMAL("normal"),
		EMERGENCY("emergency");

		private final String label;
		
		private State(String label) {
			this.label = label;
		}	
		
		public String getLabel() {
			return label;
		}
	}
	
	/**
	 * The current emergency state, that is calculate from all events coming from applications
	 * in the fire management domain.
	 */
	private State state;
	
	/**
	 * This is the APAM internal property used to notify state changes
	 */
	private String	stateProperty;
	
	/**
	 * The APM insatnce
	 */
	private Instance instance;
	
	public EmergencyDetection() {
		state = State.NORMAL;
		stateProperty = state.getLabel();
	}
	
	@SuppressWarnings("unused")
	private void start(Instance instance) {
		this.instance = instance;
		ApamManagers.addPropertyManager(this);

	}
	
	/**
	 * Evaluate the state of the fire management domain, from different notifications
	 * coming from the enclosed applications 
	 */
//	@SuppressWarnings("unused")
//	private void onEvent(EmergencyEvent event) {
//		setState(event.isEmergency());
//	}


	private void setState(boolean onFire) {
		switch (state) {
		
		case NORMAL:
			if (onFire) {
				state = State.EMERGENCY;
			}
			break;
			
		case EMERGENCY:
			if (!onFire) {
				state = State.NORMAL;
			}
			break;
		}

		stateProperty = state.getLabel();
		instance.getComposite().setProperty("emergency",stateProperty);
		
	}

	@Override
	public void attributeChanged(Component component, String attr, String newValue, String oldValue) {
		
		System.out.println("emergency detection : attribute changed  "+attr+" ="+newValue+" component "+component+ " " +component.getClass().getName());
		
		if (!(component instanceof Instance))
			return;
		
		Instance source = (Instance) component;
		
		System.out.println("emergency detection : component  "+component+" in composite ="+source.getComposite());
		System.out.println("emergency detection : my composite  "+this.instance.getComposite());
		
		if (source.getComposite().equals(this.instance.getComposite()) && attr.equals("onFire")) {
			System.out.println("emergency detected, onfire ="+newValue);
			setState(newValue.equalsIgnoreCase("true"));
		} 

	}

	@Override
	public void attributeAdded(Component component, String attr, String newValue) {
		attributeChanged(component, attr, newValue, null);
	}

	@Override
	public void attributeRemoved(Component component, String attr,	String oldValue) {
		attributeChanged(component, attr, null, oldValue);
	}


	@Override
	public String getName() {
		return "EmergencyDetection";
	}
}
