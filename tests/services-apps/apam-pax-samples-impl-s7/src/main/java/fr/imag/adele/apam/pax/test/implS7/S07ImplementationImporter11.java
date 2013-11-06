package fr.imag.adele.apam.pax.test.implS7;

import fr.imag.adele.apam.Implementation;

public class S07ImplementationImporter11 implements S07Interface11 {

    public Implementation injected;

    public Implementation getInjected() {
	return injected;
    }

    @Override
    public String whoami() {
	return "";
    }

}
