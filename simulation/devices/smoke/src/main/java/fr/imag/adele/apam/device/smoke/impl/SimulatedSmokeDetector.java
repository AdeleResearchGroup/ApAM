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
package fr.imag.adele.apam.device.smoke.impl;


import java.util.List;

import fr.liglab.adele.apam.device.fire.SmokeDetector;
import fr.liglab.adele.icasa.device.util.AbstractDevice;
import fr.liglab.adele.icasa.location.Zone;
import fr.liglab.adele.icasa.simulator.SimulatedDevice;

/**
 * Implementation of a simulated Oven device.
 *
 */

public class SimulatedSmokeDetector extends AbstractDevice implements SmokeDetector, SimulatedDevice { 

	private final static String FIRE_DETECTED_PROPERTY_NAME = "fire";
	
	private String m_serialNumber;

    private String fault;

    private String state;
    
    protected String location;

    protected boolean onFire;
    
    public SimulatedSmokeDetector(){
		super();

		onFire = false;
		super.setPropertyValue(FIRE_DETECTED_PROPERTY_NAME, onFire);
    }

	@SuppressWarnings("unused")
	private boolean fireStatusChanged(boolean newState) {
		onFire = newState;
		super.setPropertyValue(FIRE_DETECTED_PROPERTY_NAME, onFire);
		return onFire;
	}
	
    @Override
    public String getSerialNumber() {
        return m_serialNumber;
    }
    
    @Override
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public String getFault() {
        return fault;
    }

    @Override
    public void setFault(String fault) {
        this.fault = fault;
    }

    @Override
	public void enterInZones(List<Zone> zones) {
    	
    	/*
    	 * NOTE filed "location" is an APAM injected field that is recalculated
    	 * on each access, use a copy to avoid side-effects on multiple evaluations.
    	 */
    	
    	String currentLocation = location;
    	if (currentLocation == SimulatedDevice.LOCATION_UNKNOWN && zones.isEmpty())
    		return;
    	
    	if (currentLocation != SimulatedDevice.LOCATION_UNKNOWN && zones.contains(currentLocation))
    		return;
    	
    	location = zones.isEmpty() ? SimulatedDevice.LOCATION_UNKNOWN : zones.get(0).getId();

	}
}
