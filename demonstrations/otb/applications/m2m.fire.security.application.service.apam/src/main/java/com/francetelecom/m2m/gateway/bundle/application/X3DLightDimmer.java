package com.francetelecom.m2m.gateway.bundle.application;

import org.osgi.x3d.IX3DDevice;
import org.osgi.x3d.X3DHandler;

/**
 * This class defines is a refined proxy used for control X3D light dimmer
 * @author mpcy8647
 *
 */
public class X3DLightDimmer implements X3DHandler {
	
	private static final String SET_BRIGHTNESS_CMD = "set brightness";
	
	/**
	 * X3D device proxy
	 */
	private final IX3DDevice device;
	
	
	
	/**
	 * Create a new X3DLight
	 * @param pDevice x3D device proxy
	 */
	public X3DLightDimmer(IX3DDevice pDevice) {
		device = pDevice;
	}
	
	public IX3DDevice getX3DDevice() {
		return device;
	}

	/**
	 * 
	 */
	public synchronized void switchOn() {
		device.executeCommand(SET_BRIGHTNESS_CMD, new String[] {"100"}, this);
	}
	
	public synchronized void switchOff() {
		System.out.println("switch off");
		device.executeCommand(SET_BRIGHTNESS_CMD, new String[] {"0"}, this);
	}

	public void onCommandFailure(String arg0, String arg1) {
		System.out.println("onCommandFailure (arg0=" + arg0 + ", arg1=" + arg1 + ")");
		
	}

	public void onCommandSuccess(String arg0, String arg1) {
		System.out.println("onCommandSuccess (arg0=" + arg0 + ", arg1=" + arg1 + ")");
	}
	
}
