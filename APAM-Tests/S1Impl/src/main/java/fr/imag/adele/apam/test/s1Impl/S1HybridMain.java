package fr.imag.adele.apam.test.s1Impl;

import fr.imag.adele.apam.test.s1.S1;

public class S1HybridMain implements Runnable {
    S1 s1;

    public void run() {
        s1.callS1("From S1 Main ");
        System.out.println("End of S1 MAIN");
    }

    public void start() {
        System.out.println("Hybrid started: " );
        new Thread(this, "APAM test").start();
    }

    public void stop() {
    }


}
