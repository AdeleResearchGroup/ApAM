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
import fr.liglab.adele.icasa.simulator.Person;
import fr.liglab.adele.icasa.simulator.SimulatedDevice;
import fr.liglab.adele.icasa.simulator.SimulationManager;
import fr.liglab.adele.icasa.simulator.listener.PersonListener;

/**
 * Implementation of a simulated Oven device.
 * 
 */

public class SimulatedPresenceSensor extends AbstractDevice implements PresenceSensor, SimulatedDevice, PersonListener {

	private SimulationManager manager;
	
	private String m_serialNumber;

	private String fault;

	private String state;

	private volatile Zone m_zone;

	protected String location;

	@SuppressWarnings("unused")
	private String m_currentPresence;
	
	public SimulatedPresenceSensor() {
		super();

		super.setPropertyValue(SimulatedDevice.LOCATION_PROPERTY_NAME, SimulatedDevice.LOCATION_UNKNOWN);
		location = SimulatedDevice.LOCATION_UNKNOWN;
		m_zone = null;
		
        super.setPropertyValue(PRESENCE_SENSOR_SENSED_PRESENCE, false);
        m_currentPresence = getPropertyValue(PRESENCE_SENSOR_SENSED_PRESENCE).toString();
	}
	
    /**
     * @return the state
     */
	@Override
    public String getState() {
        return state;
    }

    /**
     * sets the state
     */
    @Override
    public void setState(String state) {
        this.state = state;
    }
    
    /**
     * @return the fault
     */
    @Override
    public String getFault() {
        return fault;
    }

    /**
     * @param fault
     *           the fault to set
     */
    @Override
    public void setFault(String fault) {
        this.fault = fault;
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

    @Override
	public void enterInZones(List<Zone> zones) {
    	
    	if (m_zone == null && zones.isEmpty())
    		return;
    	
    	if (m_zone != null && zones.contains(m_zone))
    		return;
    	
    	m_zone = zones.isEmpty() ? null : zones.get(0);
    	location = m_zone != null ? m_zone.getId() : SimulatedDevice.LOCATION_UNKNOWN;
    	
		updateState();
	}

	/**
	 * Calculates if a person is found in the detection zone of this device. When
	 * there is a change of previous detection a event is sent to listeners
	 */
	private boolean updateState() {

		boolean personFound = personInZone();
		boolean previousDetection = (Boolean) getPropertyValue(PRESENCE_SENSOR_SENSED_PRESENCE);

		if (!previousDetection && personFound) { // New person in Zone
				setPropertyValue(PRESENCE_SENSOR_SENSED_PRESENCE, true);
		} 
		
		if (previousDetection && !personFound) {// The person or sensor has leave the detection zone
			setPropertyValue(PRESENCE_SENSOR_SENSED_PRESENCE, false);
		}
		
		m_currentPresence = getPropertyValue(PRESENCE_SENSOR_SENSED_PRESENCE).toString();
		return (Boolean) getPropertyValue(PRESENCE_SENSOR_SENSED_PRESENCE);
	}

	private boolean personInZone() {
		if (m_zone == null)
			return false;
		
		for (Person person : manager.getPersons()) {
			if (m_zone.contains(person))
				return true;
		}
		
		return false;
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
	
	
	@SuppressWarnings("unused")
	private void start() {
		manager.addListener(this);
	}
	
	@SuppressWarnings("unused")
	private void stop() {
		manager.removeListener(this);
	}
	

}
