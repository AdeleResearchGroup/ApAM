package fr.imag.adele.apam.pax.test.implS2;

import fr.imag.adele.apam.pax.test.iface.S2;

public class S2InnerImpl implements S2 {

	S2 middle;
	
	public String whoami() {
		return this.getClass().getName();
	}

	public S2 getMiddle() {
		return middle;
	}

	public void setMiddle(S2 middle) {
		this.middle = middle;
	}

}
