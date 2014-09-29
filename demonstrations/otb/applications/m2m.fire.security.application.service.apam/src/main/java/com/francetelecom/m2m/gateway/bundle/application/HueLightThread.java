package com.francetelecom.m2m.gateway.bundle.application;

import com.orange.openthebox.hab.HueLightDevice;

/**
 * This thread makes blinking a hue light
 * @author mpcy8647
 *
 */
public class HueLightThread implements Runnable {

	/**
	 * hue light
	 */
	private final HueLightDevice hueLight;
	
	/**
	 * blinking duration in ms 
	 */
	private int duration;
	
	
	/**
	 * New Hue light thread
	 * @param pHueLight hue light
	 * @param pDuration duration in ms
	 */
	public HueLightThread(HueLightDevice pHueLight, int pDuration) {
		hueLight = pHueLight;
		duration = pDuration;
	}
	

	public void run() {
		hueLight.setState(true, 255, 255, 65535, 0, 1);
		try {
			Thread.sleep(duration);
		} catch (InterruptedException e) {
		}

		hueLight.setState(false, -1, -1, -1, 0, 0);
		
	}

	public void start() {
		Thread t = new Thread(this);
		t.start();
	}
}
