package fr.imag.adele.apam.pax.test.implS7;

import fr.imag.adele.apam.Implementation;

public class S07ImplementationImporter08 implements S07Interface08 {

    public Implementation injected;

    public Implementation getInjected() {
	return injected;
    }

    @Override
    public String whoami() {
	return "";
    }

}
