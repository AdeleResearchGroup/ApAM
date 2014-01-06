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

import fr.imag.adele.apam.Instance;
import fr.liglab.adele.apam.device.fire.EmergencyEvent;

/**
 * This class keeps track of the current state of the fire management domain.
 * 
 * Applications in this domain may signal an emergency by sending an EmergencyEvent message
 * 
 * @author vega
 *
 */
public class EmergencyDetection {

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
	}
	
	/**
	 * Evaluate the state of the fire management domain, from different notifications
	 * coming from the enclosed applications 
	 */
	@SuppressWarnings("unused")
	private void onEvent(EmergencyEvent event) {
		switch (state) {
		
		case NORMAL:
			if (event.isEmergency()) {
				state = State.EMERGENCY;
			}
			break;
			
		case EMERGENCY:
			if (!event.isEmergency()) {
				state = State.NORMAL;
			}
			break;
		}
		
		stateProperty = state.getLabel();
		instance.getComposite().setProperty("emergency",stateProperty);
	}


}
