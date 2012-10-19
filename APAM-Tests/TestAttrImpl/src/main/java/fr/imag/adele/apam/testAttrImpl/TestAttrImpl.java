package fr.imag.adele.apam.testAttrImpl;

import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.testAttr.TestAttr;

public class TestAttrImpl implements TestAttr, ApamComponent {
//    S2 s2;
    String theFieldAttr ;

	@Override
	public void callS1(String s) {
        System.out.println("In TestAttr " + s);
		theFieldAttr = s ;
        //s2.callS2("From S1toS2Final" ) ;
	}

	@Override
	public void apamInit(Instance apamInstance) {
		
		System.out.println("TestAttrImpl is started. xml fieldAttr value: " + apamInstance.getProperty("fieldAttr"));
		theFieldAttr = "initial set by program" ;
		System.out.println(" initialized fieldAttr value: " + apamInstance.getProperty("fieldAttr"));
	}

	@Override
	public void apamRemove() {
	}

}
