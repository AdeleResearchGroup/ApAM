package fr.imag.adele.apam.pax.test.implS7;


public class S07ImplementationImporter03 implements S07Interface03 {

	public S07Interface03 injected;

	@Override
	public String whoami() {
		return "";
	}

	public S07Interface03 getInjected() {
		return injected;
	}

}