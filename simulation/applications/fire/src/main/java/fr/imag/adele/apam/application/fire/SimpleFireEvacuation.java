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

import java.util.List;

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
	@SuppressWarnings("unused")
	private void smokeDetected(boolean smokeDetected) {
		
		/*
		 * calculate emergency state. 
		 * 
		 * In this toy example, simply use the smoke detection value
		 */
		
		fireDetected(smokeDetected);

		if (!smokeDetected)
			return;

		/*
		 * Unlock doors to allow evacuation
		 */
		
		List<Lock> boundDoors = doors;
		
		if (boundDoors == null)
			return;
		
		for(Lock lock:boundDoors){
			lock.unlock();
		}
		
	}


}
