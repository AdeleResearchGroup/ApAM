package fr.imag.adele.apam.pax.test.implS7;

import fr.imag.adele.apam.Specification;

public class S07ImplementationImporter02 implements S07Interface02 {

	public Specification injected;

	@Override
	public String whoami() {
		return "";
	}

	public Specification getInjected() {
		return injected;
	}

}
