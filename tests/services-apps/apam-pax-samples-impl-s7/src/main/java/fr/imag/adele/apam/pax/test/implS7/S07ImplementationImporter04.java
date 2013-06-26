package fr.imag.adele.apam.pax.test.implS7;

import fr.imag.adele.apam.Implementation;

public class S07ImplementationImporter04 implements S07Interface04 {

	public Implementation injected;

	@Override
	public String whoami() {
		// TODO Auto-generated method stub
		return "helloooo";
	}

	public Implementation getInjected() {
		return injected;
	}

}
