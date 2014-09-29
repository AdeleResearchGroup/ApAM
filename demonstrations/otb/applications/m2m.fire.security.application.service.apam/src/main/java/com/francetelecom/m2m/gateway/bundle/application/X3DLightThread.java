package com.francetelecom.m2m.gateway.bundle.application;

import java.util.Date;

/**
 * This class makes blinking a X3D light.
 * @author mpcy8647
 *
 */
public class X3DLightThread implements Runnable {
	
	/**
	 * x3d light
	 */
	private final X3DLightDimmer x3dLight;
	
	/**
	 * blink duration is ms
	 */
	private int duration;

	/**
	 * Create a new x3d light thread
	 * @param pX3DLight x3d light
	 * @param pDuration in ms
	 */
	public X3DLightThread(X3DLightDimmer pX3DLight, int pDuration) {
		x3dLight = pX3DLight;
		duration = pDuration;
	}

	public void run() {
		long startTime = new Date().getTime();
		long endingTime = startTime + duration;
		
		while(new Date().getTime() < endingTime) {
			x3dLight.switchOn();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			x3dLight.switchOff();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			
		}
		
	}
	
	public void start() {
		Thread t = new Thread(this);
		t.start();
	}

}
