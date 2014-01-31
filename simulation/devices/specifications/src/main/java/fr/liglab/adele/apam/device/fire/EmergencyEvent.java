package fr.liglab.adele.apam.device.fire;

/**
 * A simple message used by applications to notify there is an emergency
 * at  home
 * 
 * @author vega
 *
 */
public class EmergencyEvent {

	private final boolean fireDectected;

	public EmergencyEvent(boolean fireDetected) {
		this.fireDectected = fireDetected;
	}
	
	public boolean isEmergency() {
		return fireDectected;
	}
}
