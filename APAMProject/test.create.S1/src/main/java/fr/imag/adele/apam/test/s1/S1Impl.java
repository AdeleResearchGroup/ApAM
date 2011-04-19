package fr.imag.adele.apam.test.s1;

import fr.imag.adele.apam.test.s2.S2;

public class S1Impl implements S1 {

    // Apam handler injected
    S2 s2;

    public void callS1(String s) {
        System.out.println("S1 called " + s);
        s2.callS2("from S1Impl");
    }
}
