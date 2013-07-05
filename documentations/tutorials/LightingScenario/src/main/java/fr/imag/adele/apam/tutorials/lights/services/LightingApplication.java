/**
 * Copyright 2011-2013 Universite Joseph Fourier, LIG, ADELE team
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
 *
 * LightingApplication.java - 2 juil. 2013
 */
package fr.imag.adele.apam.tutorials.lights.services;

import java.util.Iterator;
import java.util.Set;

import fr.imag.adele.apam.tutorials.lights.devices.BinaryLight;
import fr.imag.adele.apam.tutorials.lights.devices.messages.ButtonPressed;

/**
 * @author thibaud
 *
 */
public class LightingApplication {
	
	private Set<BinaryLight> theLights;
	
	public void start() {
		System.out.println("LightingApplication start");
		//used to make the binding with already existing instances of lights
		/*if(theLights!=null && theLights.size()>0)
			System.out.println("LightingApplication.start() : "
					+theLights.size()+" lights have been found !");
		else
			System.out.println("LightingApplication.start() : "
					+"no lights for the moment");*/
	}
	
	public void stop() {
		System.out.println("LightingApplication stop ");
	}

	public void aButtonHasBeenPressed(ButtonPressed event) {
		System.out.println("LightingApplication.aButtonHasBeenPressed(ButtonPressed event)");
		// Change the lights status
		if (theLights != null && theLights.size()>0) {
			Iterator<BinaryLight> it=theLights.iterator();
			while(it.hasNext())
				it.next().switchLightStatus();			
		}
	}

    public void newLight() {
        System.out.println("LightingApplication.newLight() : there are "
                +theLights.size()+" in this area");
        if (theLights != null && theLights.size()>0) {
            Iterator<BinaryLight> it=theLights.iterator();
            while(it.hasNext())
                System.out.println("-> "+it.next().getName());
        }

    }

}
