package fr.imag.adele.apam.pax.test.implS7;

import fr.imag.adele.apam.Implementation;

public class S07ImplementationImporter01 implements S07Interface01 {

	public Implementation injected;

	@Override
	public String whoami() {
		return "";
	}

	public Implementation getInjected() {
		return injected;
	}

	
}
