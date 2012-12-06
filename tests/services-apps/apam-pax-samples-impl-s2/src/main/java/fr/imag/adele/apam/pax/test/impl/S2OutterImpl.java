package fr.imag.adele.apam.pax.test.impl;

import fr.imag.adele.apam.pax.test.iface.S2;

public class S2OutterImpl implements S2 {

	public String whoami() {
		return this.getClass().getName();
	}

}
