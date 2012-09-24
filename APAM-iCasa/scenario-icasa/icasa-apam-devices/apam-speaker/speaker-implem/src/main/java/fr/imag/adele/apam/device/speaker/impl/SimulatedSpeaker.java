/**
 *
 *   Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
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
package fr.imag.adele.apam.device.speaker.impl;

import fr.imag.adele.apam.device.speaker.Speaker;
import fr.liglab.adele.icasa.device.util.AbstractDevice;
import fr.liglab.adele.icasa.environment.SimulatedDevice;
import fr.liglab.adele.icasa.environment.SimulatedEnvironment;

/**
 * Implementation of a simulated Speaker device.
 * 
 */

public class SimulatedSpeaker extends AbstractDevice implements Speaker,
        SimulatedDevice {
    
	private String m_serialNumber;
	
    private String state;

    private String fault;


    private volatile SimulatedEnvironment m_env;

	private String location;
	
	public String getSerialNumber() {
		return m_serialNumber;
	}

	@Override
	public synchronized void bindSimulatedEnvironment(SimulatedEnvironment environment) {
		m_env = environment;
		location = getEnvironmentId();
	}

	@Override
	public synchronized String getEnvironmentId() {
		return m_env != null ? m_env.getEnvironmentId() : null;
	}

	@Override
	public synchronized void unbindSimulatedEnvironment(SimulatedEnvironment environment) {
		m_env = null;
		location = getEnvironmentId();
	}

 
    public String getLocation() {
        return getEnvironmentId();
     }


     
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
	public void start() {
		setState(STATE_ACTIVATED);
		
	}

	@Override
	public void stop() {
		setState(STATE_DEACTIVATED);
	} 

}
