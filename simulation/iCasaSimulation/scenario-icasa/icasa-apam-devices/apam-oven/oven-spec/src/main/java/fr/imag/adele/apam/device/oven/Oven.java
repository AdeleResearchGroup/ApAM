package fr.imag.adele.apam.device.oven;


public interface Oven   {

		void keepWarm(int time);

		void start();

		void stop();
	
		String getLocation();
	
}