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
import fr.liglab.adele.icasa.location.Position;
import fr.liglab.adele.icasa.location.Zone;
import fr.liglab.adele.icasa.location.ZoneListener;
import fr.liglab.adele.icasa.simulator.SimulatedDevice;
import fr.liglab.adele.icasa.simulator.SimulationManager;

/**
 * Implementation of a simulated Oven device.
 *
 */

public class SimulatedBinaryLight extends AbstractDevice implements BinaryLight, SimulatedDevice, ZoneListener { 

	private String m_serialNumber;

    private String fault;

    private String state;
    
    private SimulationManager manager;

//    private volatile SimulatedEnvironment m_env;
    
    private double m_maxIlluminance;

    private volatile boolean m_powerStatus;

    protected String location;

    public SimulatedBinaryLight(){
		super();
        super.setPropertyValue(SimulatedDevice.LOCATION_PROPERTY_NAME, SimulatedDevice.LOCATION_UNKNOWN);
//		super.setPropertyValue(BinaryLight.BINARY_LIGHT_POWER_STATUS, false);
//		super.setPropertyValue(BinaryLight.BINARY_LIGHT_MAX_POWER_LEVEL, 100.0d);
    }

    public String getSerialNumber() {
        return m_serialNumber;
    }
    
    @Override
    public synchronized boolean getPowerStatus() {
        return m_powerStatus;
    }

    @Override
    public synchronized boolean setPowerStatus(boolean status) {
    	
    	System.out.println("Configuring lights to "+status);
    	
    	setPropertyValue("binaryLight.powerStatus", (Boolean) status);
    	
        m_powerStatus=status;
        
        return m_powerStatus;
    }

//    @Override
//    public synchronized String getEnvironmentId() {
//        return m_env != null ? m_env.getEnvironmentId() : null;
//    }
//
//    @Override
//    public synchronized void bindSimulatedEnvironment(
//            SimulatedEnvironment environment) {
//        m_env = environment;
//        System.out.println("Bound to simulated environment "
//                + environment.getEnvironmentId());
//        location= m_env.getEnvironmentId();
//        notifyEnvironment(illuminance());
//    }
//
//    @Override
//    public synchronized void unbindSimulatedEnvironment(
//            SimulatedEnvironment environment) {
//        notifyEnvironment(-illuminance());
//        m_env = null;
//        location= "outside";
//        System.out.println("Unbound from simulated environment "
//                + environment.getEnvironmentId());
//    }

    /**
     * Notify the bound simulated environment that the illuminance emitted by
     * this light has changed.
     *
     * @param illuminanceDiff
     *            the illuminance difference
     */
    private void notifyEnvironment(double illuminanceDiff) {
//        m_env.lock();
//        try {
//            double current = m_env
//                    .getProperty(SimulatedEnvironment.ILLUMINANCE);
//            m_env.setProperty(SimulatedEnvironment.ILLUMINANCE, current
//                    + illuminanceDiff);
//        } finally {
//            m_env.unlock();
//        }
    }

    /**
     * Return the illuminance currently emitted by this light, according to its
     * state.
     *
     * @return the illuminance currently emitted by this light
     */
    private double illuminance() {
        return m_powerStatus ? m_maxIlluminance : 0.0d;
    }

//    public String getLocation() {
//        return getEnvironmentId();
//    }

    /**
     * sets the state
     */
    public void setState(String state) {
        this.state = state;
    }


    /**
     * @return the state
     */
    public String getState() {
        return state;
    }


    /**
     * @return the fault
     */
    public String getFault() {
        return fault;
    }


    /**
     * @param fault the fault to set
     */
    public void setFault(String fault) {
        this.fault = fault;
    }
	@Override
	public double getMaxPowerLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	protected void start() {
		manager.addListener(this);
	}

	protected void stop() {
		manager.removeListener(this);
	}

	@Override
	public void zoneVariableAdded(Zone zone, String variableName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void zoneVariableRemoved(Zone zone, String variableName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void zoneVariableModified(Zone zone, String variableName,
			Object oldValue) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void zoneAdded(Zone zone) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void zoneRemoved(Zone zone) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void zoneMoved(Zone zone, Position oldPosition) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void zoneResized(Zone zone) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void zoneParentModified(Zone zone, Zone oldParentZone) {
		// TODO Auto-generated method stub
		
	}
	
    @Override
	public void enterInZones(List<Zone> zones) {
		if (!zones.isEmpty()) {
			location = zones.get(0).getId();
			
		}

	}
	
}
