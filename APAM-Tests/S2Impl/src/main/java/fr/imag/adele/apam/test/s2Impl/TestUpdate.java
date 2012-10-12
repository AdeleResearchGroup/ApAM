package fr.imag.adele.apam.test.s2Impl;

import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.TestAttr.CapteurTemp;

public class TestUpdate implements ApamComponent, Runnable{

	CapteurTemp temperature ;

	@Override
	public void run() {
		int prev = temperature.getTemp () ;
		System.err.println("Got previous value : " + prev);
		while (temperature.getTemp () == prev){
			if (! (temperature instanceof CapteurTemp)) {
				System.err.println("bat type for temperature: " + temperature.getClass().getCanonicalName()) ;
			}
			if (temperature.getTemp () != prev) break ;
		}
		System.err.println ("previous = " + prev + ". nouveau= " + temperature.getTemp ()) ;
	}


	@Override
	public void apamInit(Instance apamInstance) {
		new Thread(this, "test Update").start();
	}
	@Override
	public void apamRemove() {
	}

}
