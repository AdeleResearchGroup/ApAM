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
package fr.imag.adele.apam.application.lock;

import java.util.Collections;
import java.util.List;

import org.osgi.service.http.HttpService;
import org.osgi.service.zigbee.ZigBeeCluster;
import org.osgi.service.zigbee.ZigBeeCommand;
import org.osgi.service.zigbee.ZigBeeCommandHandler;
import org.osgi.service.zigbee.ZigBeeEndpoint;
import org.osgi.service.zigbee.ZigBeeException;
import org.osgi.x3d.IX3DDevice;
import org.osgi.x3d.X3DHandler;

import fr.imag.adele.apam.Instance;
//import fr.liglab.adele.apam.device.access.Lock;

/**
 * This class implements a simplistic test application that lock doors and turns off plugs
 * when a trigger is activated
 * 
 * @author vega
 *
 */
public class LockHomeAutomation implements X3DHandler, ZigBeeCommandHandler {


	private Instance 					instance;
	private boolean 					isNight;

	private List<ZigBeeEndpoint> 		locks;
	private IX3DDevice					shutter;
	
	private HttpService					httpService;
	private WebInterface				webInterface;
	
	@SuppressWarnings("unused")
	private void start(Instance instance) {
		this.instance 		= instance;
		this.webInterface 	= new WebInterface(httpService, instance);
	} 

	@SuppressWarnings("unused")
	private void stop() {
		this.webInterface.dispose();
	}
	
	/**
	 * Notification callback to toggle the state of the application
	 */
	private void toggleActivation() {
		
		/*
		 * toggle state, notify state change before accessing devices to allow
		 * conflict arbitration 
		 */
		instance.getComposite().setProperty("locked",isNight);
		
		if (isNight)
			night();
		else
			day();

	}
	
	private void day() {

		for (ZigBeeEndpoint lock : optional(locks)) {
			unlock(lock);
		}

		if (shutter != null) {
			shutter.executeCommand("up", new String[] {}, this);
			System.out.println("shutter up");
		}
		
		
	}
	
	private void night() {

		for (ZigBeeEndpoint lock : optional(locks)) {
			lock(lock);
		}
		
		if (shutter != null) {
			shutter.executeCommand("down", new String[] {}, this);
			System.out.println("shutter down");
		}
		
		
	}
	
	
	private void lock(ZigBeeEndpoint lock) {
		ZigBeeCluster zigbeeCluster = lock.getServerCluster(0x101);
		ZigBeeCommand lockCommand = zigbeeCluster.getCommand(0);

		try {
			lockCommand.invoke(new byte[] {}, this);
			System.out.println("locked");
		} catch (ZigBeeException e) {
		}
	}
	
	private void unlock(ZigBeeEndpoint lock) {
		ZigBeeCluster zigbeeCluster = lock.getServerCluster(0x101);
		ZigBeeCommand lockCommand = zigbeeCluster.getCommand(1);

		try {
			lockCommand.invoke(new byte[] {}, this);
			System.out.println("unlocked");
		} catch (ZigBeeException e) {
		}
	}

	@Override
	public void onCommandFailure(String arg0, String arg1) {
		System.out.println("x3d command failure "+arg0);
	}

	@Override
	public void onCommandSuccess(String arg0, String arg1) {
		System.out.println("x3d command ok "+arg0);
	}

	public void isDayChanged(String isDay) {
		System.out.println("is day changed "+isDay);
		isNight = !(isDay.equals("true"));
		toggleActivation();
	}

	
	private static <T> List<T> optional(List<T> list) {
		return list != null ? list : Collections.<T> emptyList();
	}

	@Override
	public void onSuccess(byte[] response) {
		System.out.println("zigbee command ok ");
	}

	@Override
	public void onFailure(ZigBeeException e) {
		System.out.println("zigbee command failure ");
	}
	
}
