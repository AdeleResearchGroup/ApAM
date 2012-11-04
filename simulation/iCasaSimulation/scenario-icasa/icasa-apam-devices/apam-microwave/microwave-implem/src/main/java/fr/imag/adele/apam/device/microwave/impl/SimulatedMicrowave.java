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
package fr.imag.adele.apam.device.microwave.impl;

import java.util.Timer;
import java.util.TimerTask;

import fr.imag.adele.apam.device.microwave.Microwave;
import fr.imag.adele.apam.message.AbstractConsumer;
import fr.liglab.adele.icasa.device.util.AbstractDevice;
import fr.liglab.adele.icasa.environment.SimulatedDevice;
import fr.liglab.adele.icasa.environment.SimulatedEnvironment;

/**
 * Implementation of a simulated Microwave device.
 * 
 */

public class SimulatedMicrowave extends AbstractDevice implements Microwave,
		SimulatedDevice {

	private String m_serialNumber;

	private String state;

	private String fault;

	private volatile SimulatedEnvironment m_env;

	private AbstractConsumer<String> room;

	private Timer timer;

	private int cookTime = 10;

	private String location;


	public String getSerialNumber() {
		return m_serialNumber;
	}

	@Override
	public synchronized void bindSimulatedEnvironment(
			SimulatedEnvironment environment) {
		m_env = environment;
		location = getEnvironmentId();	
	}

	@Override
	public synchronized String getEnvironmentId() {
		return m_env != null ? m_env.getEnvironmentId() : null;
	}

	@Override
	public synchronized void unbindSimulatedEnvironment(
			SimulatedEnvironment environment) {
		m_env = null;
	}

	public String getLocation() {
		return getEnvironmentId();
	}

	/**
	 * sets the state
	 */
	public void setState(String state) {
		if (state.equals(this.getState())){
			return;
		}
		this.state = state;
		timer = new Timer(true);
		if (STATE_ACTIVATED.equals(state)) {
			timer.schedule(new CookTask("Cook Finished"), cookTime * 1000);
		} else if (STATE_DEACTIVATED.equals(state)) {
			if (timer != null) {
				timer.purge();
			}
		}
	}

	/**
	 * @return the State
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
	 *            the fault to set
	 */
	public void setFault(String fault) {
		this.fault = fault;
	}

	private class CookTask extends TimerTask {
		String message;

		public CookTask(String string) {
			this.message = string;
		}

		public void run() {
			System.out.println("Microwave ("+location+")>> Timer end for the task --> "
					+ message);
			if (room != null) {
				System.out.println("Microwave ("+location+")  >> consumer not null!");
				room.pushData(message);
			} else {
				System.out
						.println("Microwave ("+location+")>> Unavailable Microwave message consumers");
			}
			timer.cancel();
			setState(STATE_DEACTIVATED);
		}
	}

	@Override
	public void setCookTime(int time) {
		cookTime = time;

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
