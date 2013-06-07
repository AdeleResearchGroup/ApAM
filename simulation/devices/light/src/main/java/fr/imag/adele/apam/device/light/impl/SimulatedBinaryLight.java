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
package fr.imag.adele.apam.device.light.impl;


import java.util.List;

import fr.liglab.adele.icasa.device.light.BinaryLight;
import fr.liglab.adele.icasa.device.util.AbstractDevice;
import fr.liglab.adele.icasa.location.Zone;
import fr.liglab.adele.icasa.simulator.SimulatedDevice;

/**
 * Implementation of a simulated Oven device.
 *
 */

public class SimulatedBinaryLight extends AbstractDevice implements BinaryLight, SimulatedDevice { 

	private String m_serialNumber;

    private String fault;

    private String state;
    
    private volatile boolean m_powerStatus;

    protected String location;

    public SimulatedBinaryLight(){
		super();
        super.setPropertyValue(SimulatedDevice.LOCATION_PROPERTY_NAME, SimulatedDevice.LOCATION_UNKNOWN);
        super.setPropertyValue(BinaryLight.BINARY_LIGHT_POWER_STATUS, false);
		super.setPropertyValue(BinaryLight.BINARY_LIGHT_MAX_POWER_LEVEL, 100.0d);
    }

    @Override
    public synchronized boolean setPowerStatus(boolean status) {
    	
    	System.out.println("Configuring lights to "+status);
    	
    	setPropertyValue(BinaryLight.BINARY_LIGHT_POWER_STATUS, status);
    	
        m_powerStatus=status;
        
        return m_powerStatus;
    }
    
    @Override
    public boolean getPowerStatus() {
        return m_powerStatus;
    }

	@Override
	public void turnOff() {
		setPowerStatus(false);
	}

	@Override
	public void turnOn() {
		setPowerStatus(true);
	}

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
	public double getMaxPowerLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

    @Override
	public void enterInZones(List<Zone> zones) {
		if (!zones.isEmpty()) {
			location = zones.get(0).getId();
			
		}

	}
}
