package fr.imag.adele.apam.test.s1Impl;

import fr.imag.adele.apam.ASMInst;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.test.s1.S1;

public class S1Main implements Runnable, ApamComponent {
    S1 s1;

    public void run() {
        s1.callS1("From S1 Main ");
        System.out.println("End of S1 MAIN");
    }

    public void apamStart(ASMInst inst) {
        System.out.println("S1Main Started : " + inst.getName());
        new Thread(this, "APAM test").start();
    }

    public void apamStop() {
    }

    public void apamRelease() {
    }

}
