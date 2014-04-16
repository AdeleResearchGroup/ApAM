package fr.imag.adele.apam.tests.app.relationpack1;

import fr.imag.adele.apam.tests.app.exportpack1.MainClass;

public class ClientClass {
	
	
	public String service() {
		MainClass theMainClass = new MainClass();
		return theMainClass.whoami();
		
	}
	

}
