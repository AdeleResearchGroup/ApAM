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

import fr.imag.adele.apam.Instance;
//import fr.liglab.adele.apam.device.access.Lock;

import org.osgi.x3d.IX3DDevice;
import org.osgi.x3d.X3DHandler;

/**
 * This class implements a simplistic test application that lock doors and turns off plugs
 * when a trigger is activated
 * 
 * @author vega
 *
 */
public class LockHomeAutomation implements X3DHandler {


	private Instance 					instance;
	private boolean 					activated;

	//private List<Lock>					doors;
	private IX3DDevice					shutter;
	
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
		instance.getComposite().setProperty("locked",activated);

		/* 
		 * lock doors on activation, and enable card-based authorization
		 * on deactivation
		 * 
		 */
		
		/*
		for (Lock door: optional(doors)){
			if (activated)
				door.disableAuthorization(true);
			else
				door.enableAuthorization();
		}
		*/	
		if (shutter != null) {
			if (activated) {
				shutter.executeCommand("down", new String[] {}, this);
			}
			else{
				shutter.executeCommand("up", new String[] {}, this);
			}
		}
		
	}
	
	@Override
	public void onCommandFailure(String arg0, String arg1) {
		System.out.println("x3d command failure "+arg0);
	}

	@Override
	public void onCommandSuccess(String arg0, String arg1) {
		System.out.println("x3d command ok "+arg0);
	}

	public void isDayChanged(String isDay) {
		System.out.println("is day changed "+isDay);
		activated = !(isDay.equals("true"));
		toggleActivation();
	}

	/*
	private static <T> List<T> optional(List<T> list) {
		return list != null ? list : Collections.<T> emptyList();
	}
	*/
}
