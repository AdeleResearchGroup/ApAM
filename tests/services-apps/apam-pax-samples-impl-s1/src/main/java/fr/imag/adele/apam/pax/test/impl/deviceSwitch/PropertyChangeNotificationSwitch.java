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
package fr.imag.adele.apam.pax.test.impl.deviceSwitch;

import fr.imag.adele.apam.pax.test.iface.device.Eletronic;



public class PropertyChangeNotificationSwitch extends GenericSwitch implements Eletronic{

	String state="old value";
	
	Integer stateChangedCalledCounter=0;

	public Integer getStateChangedCounter() {
		return stateChangedCalledCounter;
	}

	public void setStateChangedCalledCounter(Integer stateChangedCalledCounter) {
		this.stateChangedCalledCounter = stateChangedCalledCounter;
	}

	public void stateChanged(String newValue) {
		stateChangedCalledCounter++;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	
	
}
