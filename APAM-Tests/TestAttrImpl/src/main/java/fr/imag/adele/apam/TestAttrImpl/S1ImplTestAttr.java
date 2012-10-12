package fr.imag.adele.apam.TestAttrImpl;

import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.TestAttr.S1TestAttr;

public class S1ImplTestAttr implements S1TestAttr, ApamComponent {
//    S2 s2;
    String theFieldAttr ;

	@Override
	public void callS1(String s) {
        System.out.println("entering S1toS2Final " + s);
		theFieldAttr = s ;
        //s2.callS2("From S1toS2Final" ) ;
	}

	@Override
	public void apamInit(Instance apamInstance) {
		System.out.println("S1toS2Final is sarted");
		theFieldAttr = "initial set by program" ;
	}

	@Override
	public void apamRemove() {
	}

}
