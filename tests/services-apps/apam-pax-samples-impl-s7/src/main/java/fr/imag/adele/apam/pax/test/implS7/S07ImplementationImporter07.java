package fr.imag.adele.apam.pax.test.implS7;

public class S07ImplementationImporter07 implements S07Interface07 {

    public S07Dependency injected;

    public S07Dependency getInjected() {
	return injected;
    }

    @Override
    public String whoami() {
	return "";
    }

}
