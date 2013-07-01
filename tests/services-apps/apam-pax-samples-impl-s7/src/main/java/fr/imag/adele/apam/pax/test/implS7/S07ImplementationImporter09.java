package fr.imag.adele.apam.pax.test.implS7;

import fr.imag.adele.apam.Specification;


public class S07ImplementationImporter09 implements S07Interface09 {

	public Specification injected;

	@Override
	public String whoami() {
		return "";
	}

	public Specification getInjected() {
		return injected;
	}

}
