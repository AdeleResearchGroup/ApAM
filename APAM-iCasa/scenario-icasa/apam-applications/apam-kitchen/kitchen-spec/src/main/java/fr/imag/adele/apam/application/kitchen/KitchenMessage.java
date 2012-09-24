package fr.imag.adele.apam.application.kitchen;

public class KitchenMessage {

	private final String deviceName;
	
	private final String location;
	
	private final String content;
	
	public KitchenMessage(String deviceName, String location, String content){
		this.deviceName = deviceName;
		this.location = location;
		this.content=content;
	}

	public String getContent() {
		return content;
	}

	public String getLocation() {
		return location;
	}

	public String getDeviceName() {
		return deviceName;
	}
		
}
