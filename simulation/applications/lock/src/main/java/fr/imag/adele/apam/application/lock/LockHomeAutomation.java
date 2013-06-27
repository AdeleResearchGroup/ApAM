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

import java.util.List;

import fr.liglab.adele.apam.device.access.Lock;

/**
 * This class implements a basic automatic lightning service. It  listen to
 * presence detection events in the room and automatically turn on/off all
 * lights in the room
 * 
 * @author vega
 *
 */
public class LockHomeAutomation {

	private List<Lock>		doors;

	/**
	 * Notification callback for a presence change
	 */
	@SuppressWarnings("unused")
	private void presenceChanged(boolean presenceSensed) {
		
		if (!presenceSensed)
			return;
		
		List<Lock> boundDoors = doors;
		
		if (boundDoors == null)
			return;
		
		for(Lock lock:boundDoors){
			lock.lock();
		}
	}

}
