package fr.imag.adele.apam.pax.test.impl.deviceSwitch;

import fr.imag.adele.apam.pax.test.iface.device.Eletronic;



public class PhilipsSwitch extends GenericSwitch implements Eletronic{

	@Override
	public void shutdown() {
		//Proprietary way of shutting down a philips device	
	}

}
