package fr.imag.adele.apam.test.s4;

import fr.imag.adele.apam.test.s2.S2;
import fr.imag.adele.apam.test.s5.S5;

public class S4Impl implements S4 {

    S2 s2_inv;
    S5 s5;

    @Override
    public void callS4(String s) {
        System.out.println("S4 called " + s);
        s5.callS5(" from s4");
        s2_inv.callBackS2(" back from s4");
    }

    @Override
    public void callBackS4(String s) {
        System.out.println("called back : " + s);
    }
}
