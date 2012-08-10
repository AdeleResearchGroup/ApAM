package fr.imag.adele.apam.application.kitchen.impl;

import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.application.kitchen.KitchenApp;
import fr.imag.adele.apam.application.kitchen.KitchenMessage;
import fr.imag.adele.apam.device.microwave.Microwave;
import fr.imag.adele.apam.device.oven.Oven;
import fr.imag.adele.apam.message.AbstractConsumer;


public class KitchenAppImpl implements KitchenApp,ApamComponent {

	private Microwave microwave;

	private Oven oven;

	/**
	 * Send kitchen Message
	 */
	private AbstractConsumer<KitchenMessage> sendKitchenMessage;

	@Override
	public void stopAllDevices() {
		if (oven != null) {
			oven.stop();
		}
		
		if (microwave != null) {
			microwave.stop();
		}
	}

	public void consumeOvenMessage(String event) {
		System.out.println("KitchenApp >> New Message from Oven : " + event);
//		System.out.println("Start keep warm  10s" );
//		oven.keepWarm(10);
		sendKitchenMessage.pushData(new KitchenMessage("Oven", oven.getLocation(), event));
	}
	
	public void consumeMicrowaveMessage(String event) {
		System.out.println("KitchenApp >> New Message from Microwave : " + event);
		sendKitchenMessage.pushData(new KitchenMessage("Microwave", microwave.getLocation(), event));
	}

	@Override
	public void startOven() {
		if (oven != null)
			oven.start();
	}

	@Override
	public void ovenKeepWarm(int time) {
		if (oven != null)
			oven.keepWarm(time);
	}

	@Override
	public void stopOven() {
		if (oven != null)
			oven.stop();
	}

	@Override
	public void startMicrowave(int time) {
		if (microwave != null)
			microwave.setCookTime(time);
			microwave.start();

	}

	@Override
	public void stopMicrowave() {
		if (microwave != null)
			microwave.stop();
	}

	@Override
	public void apamStart(Instance apamInstance) {
		// TODO 		
	}

	@Override
	public void apamStop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void apamRelease() {
		// TODO Auto-generated method stub
		
	}

}