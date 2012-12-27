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

import java.util.Timer;

import fr.liglab.adele.icasa.device.GenericDevice;
import fr.liglab.adele.icasa.device.presence.PresenceSensor;
import fr.liglab.adele.icasa.device.util.AbstractDevice;
import fr.liglab.adele.icasa.environment.SimulatedDevice;
import fr.liglab.adele.icasa.environment.SimulatedEnvironment;
import fr.liglab.adele.icasa.environment.SimulatedEnvironmentListener;

/**
 * Implementation of a simulated Oven device.
 * 
 */

public class SimulatedPresence extends AbstractDevice implements PresenceSensor, SimulatedDevice,
        SimulatedEnvironmentListener {

	private String m_serialNumber;

	private String fault;

	private String state;

	private volatile SimulatedEnvironment m_env;

    private double m_threshold=1;

    private boolean m_currentPresence=false;

	protected String location;


    @Override
    public String getSerialNumber() {
        return m_serialNumber;
    }

    @Override
    public synchronized boolean getSensedPresence() {
        return m_currentPresence;
    }

    @Override
    public synchronized void bindSimulatedEnvironment(SimulatedEnvironment environment) {
        m_env = environment;
        m_env.addListener(this);
        location= m_env.getEnvironmentId();
        System.out.println("Bound to simulated environment " + environment.getEnvironmentId());
        detectUsers()       ;
    }

    @Override
    public synchronized String getEnvironmentId() {
        return m_env != null ? m_env.getEnvironmentId() : null;
    }

    @Override
    public synchronized void unbindSimulatedEnvironment(SimulatedEnvironment environment) {
        m_env.removeListener(this);
        m_env = null;
        location="outside";
        System.out.println("Unbound from simulated environment " + environment.getEnvironmentId());
        detectUsers()       ;
    }

    @Override
    public void environmentPropertyChanged(final String propertyName, final Double oldValue, final Double newValue) {

        if (!(fault.equalsIgnoreCase("yes")) && SimulatedDevice.STATE_ACTIVATED.equals(state)) {
            if (SimulatedEnvironment.PRESENCE.equals(propertyName)) {
                final boolean oldPresence = m_currentPresence;
                detectUsers()       ;
                if (oldPresence != m_currentPresence) {
                    System.out.println("Sensed presence : " + m_currentPresence);
                    notifyListeners();
                }
            }
        }
    }


    public String getLocation() {
        return getEnvironmentId();
    }

    /**
     * sets the state
     */
    public void setState(String state) {
        this.state = state;
        detectUsers();
    }

    private void detectUsers(){
        if (SimulatedDevice.STATE_ACTIVATED.equals(state)){
            if (m_env == null){
                m_currentPresence = false;
                return;
            }
            double presence = m_env.getProperty(SimulatedEnvironment.PRESENCE).doubleValue() ;
            if (presence >= m_threshold) {
                m_currentPresence = true;
            } else {
                m_currentPresence = false;
            }
        }
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

}
