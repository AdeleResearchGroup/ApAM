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
package fr.liglab.adele.icasa.device.apam.impl;

import fr.liglab.adele.icasa.device.util.AbstractDevice;
import fr.liglab.adele.icasa.devices.apam.TvScreen;
import fr.liglab.adele.icasa.environment.SimulatedDevice;
import fr.liglab.adele.icasa.environment.SimulatedEnvironment;

/**
 * Implementation of a simulated Tv Screen device.
 * 
 */
public class SimulatedTvScreen extends AbstractDevice implements TvScreen,
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

    

    /**
     * Notify the bound simulated environment that the temperature has changed.
     * 
     * @param temperatureDiff
     *            the temperature difference
     */
    private void notifyEnvironment() {
//        m_env.lock();
//        try {
//            long time = System.currentTimeMillis();
//            double timeDiff = ((double) (time - m_lastUpdateTime)) / 1000.0d;
//            m_lastUpdateTime = time;
//            double current = m_env
//                    .getProperty(SimulatedEnvironment.TEMPERATURE);
//            double volume = m_env.getProperty(SimulatedEnvironment.VOLUME);
//            double decrease = m_maxCapacity * m_powerLevel * timeDiff / volume;
//            if (current > decrease) {
//                m_env.setProperty(SimulatedEnvironment.TEMPERATURE, current
//                        - decrease);                
//            } else {
//                m_env.setProperty(SimulatedEnvironment.TEMPERATURE, 0.0d);           
//            }
//            
//        } finally {
//            m_env.unlock();
//        }
    }

//    /**
//     * The updater thread that updates the current temperature and notify
//     * listeners periodically.
//     * 
//     * @author bourretp
//     */
//    private class UpdaterThread implements Runnable {
//
//        @Override
//        public void run() {
//            boolean isInterrupted = false;
//            while (!isInterrupted) {
//                try {
//                    Thread.sleep(m_period);
//                    synchronized (SimulatedCoolerImpl.this) {
//                        if (m_env != null) {
//                            notifyEnvironment();
//                        }
//                    }
//                } catch (InterruptedException e) {
//                    isInterrupted = true;
//                }
//            }
//        }
//    }
    
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

}
