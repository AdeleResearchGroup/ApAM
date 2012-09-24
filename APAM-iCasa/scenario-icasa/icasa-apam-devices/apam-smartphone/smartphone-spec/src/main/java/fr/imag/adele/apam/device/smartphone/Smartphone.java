package fr.imag.adele.apam.device.smartphone;


public interface Smartphone{

	public String deployApp(String url);
	
	public void startApp(String appName);
	
	public void stopApp(String appName);
	
}
