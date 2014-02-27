package fr.imag.adele.apam.tests.app;

import org.osgi.framework.Version;

public class ClientObject {
	private Version myVersionInjected;

	public Version getMyVersionInjected() {
		return myVersionInjected;
	}

	public void setMyVersionInjected(Version myVersionInjected) {
		this.myVersionInjected = myVersionInjected;
	}
	

}
