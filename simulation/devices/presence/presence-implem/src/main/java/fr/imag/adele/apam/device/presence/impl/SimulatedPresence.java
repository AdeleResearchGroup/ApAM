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
package fr.imag.adele.apam.device.presence.impl;

import java.util.List;

import fr.liglab.adele.icasa.device.presence.PresenceSensor;
import fr.liglab.adele.icasa.device.util.AbstractDevice;
import fr.liglab.adele.icasa.location.LocatedDevice;
import fr.liglab.adele.icasa.location.Position;
import fr.liglab.adele.icasa.location.Zone;
import fr.liglab.adele.icasa.location.ZoneListener;
import fr.liglab.adele.icasa.simulator.Person;
import fr.liglab.adele.icasa.simulator.SimulatedDevice;
import fr.liglab.adele.icasa.simulator.SimulationManager;
import fr.liglab.adele.icasa.simulator.listener.PersonListener;

/**
 * Implementation of a simulated Oven device.
 * 
 */

public class SimulatedPresence extends AbstractDevice implements PresenceSensor, fr.liglab.adele.icasa.simulator.SimulatedDevice,
        ZoneListener, PersonListener {

	private SimulationManager manager;
	
	private String m_serialNumber;

	private String fault;

	private String state;

	private volatile Zone m_zone;

    private double m_threshold=1;

    private boolean m_currentPresence=false;
	    
	protected String location;

	public SimulatedPresence() {
		super();
        super.setPropertyValue(SimulatedDevice.LOCATION_PROPERTY_NAME, SimulatedDevice.LOCATION_UNKNOWN);
        super.setPropertyValue(PRESENCE_SENSOR_SENSED_PRESENCE, false);
	}
	
    @Override
    public String getSerialNumber() {
        return m_serialNumber;
    }

    @Override
    public synchronized boolean getSensedPresence() {
		Boolean presence = (Boolean) getPropertyValue(PRESENCE_SENSOR_SENSED_PRESENCE);
		if (presence != null)
			return presence;
		return false;
    }

    /**
     * sets the state
     */
    public void setState(String state) {
        this.state = state;
        detectUsers();
    }

    @Override
	public void enterInZones(List<Zone> zones) {
		if (!zones.isEmpty()) {
			m_zone = zones.get(0);
			// setPropertyValue(SimulatedDevice.LOCATION_PROPERTY_NAME,
			// m_zone.getId());
			updateState();
			location=m_zone.getId();
		}

	}
    
    private void detectUsers(){

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
     * @param fault
     *           the fault to set
     */
    public void setFault(String fault) {
        this.fault = fault;
    }

	@Override
	public void zoneVariableAdded(Zone zone, String variableName) {
		
	}

	@Override
	public void zoneVariableRemoved(Zone zone, String variableName) {
		
	}

	@Override
	public void zoneVariableModified(Zone zone, String variableName,
			Object oldValue) {
		
	}

	@Override
	public void zoneAdded(Zone zone) {
		
	}

	@Override
	public void zoneRemoved(Zone zone) {
		
	}

	@Override
	public void zoneMoved(Zone zone, Position oldPosition) {
		
	}

	@Override
	public void zoneResized(Zone zone) {
		
	}

	@Override
	public void zoneParentModified(Zone zone, Zone oldParentZone) {
		
	}

	@Override
	public void personAdded(Person person) {
		updateState();
	}

	@Override
	public void personRemoved(Person person) {
		updateState();
	}

	@Override
	public void personMoved(Person person, Position oldPosition) {
		updateState();
	}

	@Override
	public void personDeviceAttached(Person person, LocatedDevice device) {
	}

	@Override
	public void personDeviceDetached(Person person, LocatedDevice device) {
	}

	/**
	 * Calculates if a person is found in the detection zone of this device. When
	 * there is a change of previous detection a event is sent to listeners
	 */
	private void updateState() {
		if (m_zone != null) {

			boolean personFound = personInZone();
			boolean previousDetection = (Boolean) getPropertyValue(PRESENCE_SENSOR_SENSED_PRESENCE);

			if (!previousDetection) { // New person in Zone
				if (personFound) {
					setPropertyValue(PRESENCE_SENSOR_SENSED_PRESENCE, true);
				}
			} else {
				if (!personFound) { // The person has leave the detection zone
					setPropertyValue(PRESENCE_SENSOR_SENSED_PRESENCE, false);
				}
			}
			
		}
	}

	private boolean personInZone() {
		for (Person person : manager.getPersons()) {
			if (m_zone.contains(person))
				return true;
		}
		return false;
	}

	
	protected void start() {
		manager.addListener(this);
	}
	
	protected void stop() {
		manager.removeListener(this);
	}

	@Override
	public void deviceAttached(Zone arg0, LocatedDevice arg1) {
		
	}

	@Override
	public void deviceDetached(Zone arg0, LocatedDevice arg1) {
		
	}
	
}
