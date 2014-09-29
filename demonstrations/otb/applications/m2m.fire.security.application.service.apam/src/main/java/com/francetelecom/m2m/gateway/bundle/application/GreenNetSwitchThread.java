package com.francetelecom.m2m.gateway.bundle.application;

import com.st.greennet.service.Actuator;
import com.st.greennet.service.Device;

public class GreenNetSwitchThread implements Runnable {
	
	private  final Device device;
	private final boolean startBreathing;
	

	
	public GreenNetSwitchThread(Device pDevice, boolean pBreathing) {
		device = pDevice;
		startBreathing = pBreathing;
	}

	public void run() {
		
		if (startBreathing) {
			System.out.println("lance ventilateur ");
			((Actuator) device).actuate("WON");
		} else {
			try {
				Thread.sleep(15000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("eteint ventilateur");
			((Actuator) device).actuate("WOFF");
		}
		
//		try {
//			Thread.sleep(25000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		
	}
	
	
	public void start() {
		Thread t = new Thread(this);
		t.start();
	}

}
