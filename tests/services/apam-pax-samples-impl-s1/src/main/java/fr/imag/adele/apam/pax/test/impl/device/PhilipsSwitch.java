package fr.imag.adele.apam.pax.test.impl.device;

import fr.imag.adele.apam.pax.test.iface.device.Eletronic;



public class PhilipsSwitch implements Eletronic{

	@Override
	public void shutdown() {
		//Proprietary way of shutting down a philips device	
	}

}
