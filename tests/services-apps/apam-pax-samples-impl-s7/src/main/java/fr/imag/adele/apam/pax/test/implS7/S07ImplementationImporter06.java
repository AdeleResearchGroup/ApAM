package fr.imag.adele.apam.pax.test.implS7;


public class S07ImplementationImporter06 implements S07Interface06 {

	public S07Interface06 injected;

	@Override
	public String whoami() {
		return "";
	}

	public S07Interface06 getInjected() {
		return injected;
	}

}
