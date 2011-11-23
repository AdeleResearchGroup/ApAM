package fr.imag.adele.apam.test.s1Impl;

import fr.imag.adele.apam.test.s1.S1;
import fr.imag.adele.apam.test.s2.S2;
import fr.imag.adele.apam.test.s4.S4;

public class S1Impl implements S1 {

    // Apam handler injected
    S2 s2;
    S4 s4;

    public void callS1(String s) {
        System.out.println("S1 called " + s);
        s2.callS2("from S1Impl internal");
        s4.callS4_final("From S1Impl external");
    }
}
