package fr.liglab.adele.icasa.application.apam.living.impl;

import fr.imag.adele.apam.message.AbstractConsumer;
import fr.liglab.adele.icasa.application.apam.kitchen.impl.KitchenMessage;
import fr.liglab.adele.icasa.devices.apam.Smartphone;


public class LivingRoomAppImpl implements LivingRoomApp {

//	KitchenApp kitchen;
	
	Smartphone smartphone;
	
	AbstractConsumer<Notification> notificationConsumers;	
	String appRunning = null;
	@Override
	public void stopRunningApp() {
		if (appRunning!=null){
			smartphone.stopApp(appRunning);
			appRunning = null;
		}
	}
	
	
	public void consumeKitchenMessage(KitchenMessage kitchen){
		triggerSmartphone("http://kitchenapp");
		//TODO Customize notification.
		notificationConsumers.pushData(new Notification());
	}


	private void triggerSmartphone(String app) {
		// TODO 
		String appName = smartphone.deployApp(app);
		smartphone.startApp(appName);
		appRunning = appName;
	}
	
	
	
	
}
