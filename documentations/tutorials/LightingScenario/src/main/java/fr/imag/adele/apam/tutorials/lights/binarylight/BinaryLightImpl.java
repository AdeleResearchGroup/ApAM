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
 * BinaryLightImpl.java - 2 juil. 2013
 */
package fr.imag.adele.apam.tutorials.lights.binarylight;

import fr.imag.adele.apam.tutorials.lights.devices.BinaryLight;
import fr.imag.adele.apam.tutorials.lights.devices.messages.LightStatusChanged;

/**
 * @author thibaud
 *
 */
public class BinaryLightImpl implements BinaryLight {
	
	protected boolean currentStatus;
    private String myLocation;
    private String myName;
	
	/**
	 * @param currentStatus
	 */
	public BinaryLightImpl(boolean currentStatus) {
		super();
		this.currentStatus = currentStatus;
	}

    public void started() {
        System.out.println("A light named " + myName+" have been started in the "+myLocation);
    }

    public void stopped() {
        System.out.println("The light named "+myName+" have been stopped in the "+myLocation);
    }

	/* (non-Javadoc)
	 * @see fr.imag.adele.apam.tutorials.lights.devices.BinaryLight#isLightOn()
	 */
	public boolean isLightOn() {
		return currentStatus;
	}

	/* (non-Javadoc)
	 * @see fr.imag.adele.apam.tutorials.lights.devices.BinaryLight#setLightStatus(boolean)
	 */
	public void setLightStatus(boolean newStatus) {
		currentStatus=newStatus;
        fireLightStatus();
	}

	/* (non-Javadoc)
	 * @see fr.imag.adele.apam.tutorials.lights.devices.BinaryLight#switchLightStatus()
	 */
	public void switchLightStatus() {
		if(currentStatus)
			currentStatus=false;
		else currentStatus=true;
        fireLightStatus();
		
	}

    public LightStatusChanged fireLightStatus() {
        System.out.println("BinaryLightImpl.fireLightStatus()");
        return new LightStatusChanged(currentStatus);
    }


    public String getName() {
        return myName;
    }

    public String getLocation() {
        return myLocation;
    }
}
