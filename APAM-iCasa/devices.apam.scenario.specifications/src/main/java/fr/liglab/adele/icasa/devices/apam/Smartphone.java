package fr.liglab.adele.icasa.devices.apam;

import fr.liglab.adele.icasa.device.GenericDevice;

public interface Smartphone extends GenericDevice  {

	public String deployApp(String url);
	
	public void startApp(String appName);
	
	public void stopApp(String appName);
	
}
