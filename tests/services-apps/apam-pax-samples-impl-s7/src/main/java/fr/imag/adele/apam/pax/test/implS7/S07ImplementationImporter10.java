package fr.imag.adele.apam.pax.test.implS7;

import fr.imag.adele.apam.Implementation;

public class S07ImplementationImporter10 implements S07Interface10 {

	public Implementation injected;

	@Override
	public String whoami() {
		return "";
	}

	public Implementation getInjected() {
		return injected;
	}

}
