package fr.imag.adele.apam.pax.test.impl.p1;

import fr.imag.adele.apam.pax.test.iface.P1Spec;
import fr.imag.adele.apam.pax.test.iface.P2Spec;

public class P1Impl implements P1Spec{

	P2Spec p2;
	
	public P2Spec getP2() {
		return p2;
	}

	public void start(){
		System.out.println("Starting P1, the P2 value injected was:"+p2.getName());
	}
	
	public void stop(){
		System.out.println("Stopping P1");
	}
	
}
