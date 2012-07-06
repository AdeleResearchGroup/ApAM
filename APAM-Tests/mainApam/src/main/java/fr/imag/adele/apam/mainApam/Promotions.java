package fr.imag.adele.apam.mainApam;


import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Apam;
//import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.test.s1.S1;
import fr.imag.adele.apam.test.s2.S2;

public class Promotions implements Runnable, ApamComponent {
    // injected
    Apam apam;

    public void run() {
        System.out.println("====================================\n" +
                "======= Starting Promotions ========\n" +
        "====================================");

        System.out.println("\n\nLoading S5");
        CST.apamResolver.findSpecByName(null, "S5");
        System.out.println("S5 Loaded\n\n ");

        System.out.println("\n\nLoading S3Impl to get the instances");
        CST.apamResolver.findImplByName(null, "S3Impl");
        System.out.println("S3Impl Loaded\n\n ");

        System.out.println("\n\ncreating S1Main-Appli only loading the bundle containing S1Main");
        CST.apamResolver.findImplByName(null, "S1Impl");
        System.out.println("after apamResolver.findImplByName(null, \"S1Main\")\n\n");

    }

    public void apamStart(Instance apamInstance) {
        new Thread(this, "APAM test").start();
    }

    public void apamStop() {

    }

    public void apamRelease() {

    }
}