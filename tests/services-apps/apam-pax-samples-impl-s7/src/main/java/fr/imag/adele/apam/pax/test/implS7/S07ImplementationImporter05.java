package fr.imag.adele.apam.pax.test.implS7;


public class S07ImplementationImporter05 implements S07Interface05 {

	public S07Interface04 injected;

	@Override
	public String whoami() {
		return "";
	}

	public S07Interface04 getInjected() {
		return injected;
	}

}
