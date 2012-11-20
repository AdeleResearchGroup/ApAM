package fr.imag.adele.apam.pax.test.impl;

import fr.imag.adele.apam.pax.test.iface.S2;

public class S2MiddleImpl implements S2 {

	S2 outter;

	public String whoami() {
		return this.getClass().getName();
	}

	public S2 getOutter() {
		return outter;
	}

	public void setOutter(S2 outter) {
		this.outter = outter;
	}

}
