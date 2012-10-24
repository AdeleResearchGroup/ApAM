package fr.imag.adele.apam.test.s4Impl;

//import fr.imag.adele.apam.test.s2.S2;
import fr.imag.adele.apam.test.s4.S4;
import fr.imag.adele.apam.test.s5.S5;

public class S4Impl implements S4 {

    S5 s5;

    @Override
    public void callS4(String s) {
        System.out.println("S4 called " + s);
        if (s5 != null)
            s5.callS5(" from s4");
    }

    @Override
    public String getName() {
    	return "S4Impl";
    }
    @Override
    public void callBackS4(String s) {
        System.out.println(" In call back S4 : " + s);
    }

    @Override
    public void callS4_final(String msg) {
        System.out.println(" S4_final called " + msg);

    }
}
