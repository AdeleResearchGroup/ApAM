package fr.liglab.adele.icasa.application.apam.kitchen.impl;


public interface KitchenApp {
	
	public void stopAllDevices();
	
	public void stopMicrowave();
	
	public void stopOven();
	
	public void startMicrowave(int time);
	
	public void startOven();
	
	public void ovenKeepWarm(int time);
}
