package fr.imag.adele.apam.test.s1Impl;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.test.s1.S1;

public class S1Main implements Runnable, ApamComponent {
    S1 s1;

    public void run() {
        System.out.println("=== executing  s1.callS1(\"From S1 Main \") ");
        s1.callS1("From S1 Main ");
        System.out.println("End of S1 MAIN");
        //        s1 = null; // ne fait rien, reprend la meme valeur
        //        s1.callS1("Deuxieme from S1 Main");
    }

    public void apamInit(Instance inst) {
        System.out.println("S1Main Started : " + inst.getName());
        new Thread(this, "APAM test").start();
    }

    public void apamRemove() {
    }

}
