package fr.imag.adele.apam.device.microwave;


/**
 * 
 * @author Mehdi
 *
 */
public interface Microwave {

	void setCookTime(int time);

	void start();

	void stop();
	
	String getLocation();
}
