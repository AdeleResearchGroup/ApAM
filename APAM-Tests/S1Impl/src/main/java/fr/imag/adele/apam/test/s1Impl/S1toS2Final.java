package fr.imag.adele.apam.test.s1Impl;

import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.test.s1.S1;
import fr.imag.adele.apam.test.s2.S2;

public class S1toS2Final implements S1, ApamComponent {
    S2 s2;

	@Override
	public void callS1(String s) {
        System.out.println("entering S1toS2Final " + s);
        s2.callS2("From S1toS2Final" ) ;
	}

	@Override
	public void apamStart(Instance apamInstance) {
		System.out.println("S1toS2Final is sarted");
	}

	@Override
	public void apamStop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void apamRelease() {
		// TODO Auto-generated method stub
		
	}

}
