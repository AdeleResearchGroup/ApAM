package fr.imag.adele.apam.pax.test.implS7;

public class S07ImplementationImporter05 implements S07Interface05 {

    public S07Interface04 injected;

    public S07Interface04 getInjected() {
	return injected;
    }

    @Override
    public String whoami() {
	return "";
    }

}
