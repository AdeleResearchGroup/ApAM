package fr.imag.adele.apam.test.s2;

import java.util.Set;

import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.apamAPI.ApamComponent;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.test.s3.S3;
import fr.imag.adele.apam.test.s4.S4;

public class S2Impl implements S2, ApamComponent {

    // Apam injected
    Apam    apam;
    S4      s4_1;
    S4      s4_2;
    Set<S3> s3s;

    ASMInst myInst;

    public void apamStart(ASMInst inst) {
        myInst = inst;
    }

    public void callS2(String s) {
        int i = 1;
        for (S3 s3 : s3s) {
            s3.callS3(i + " from S2Impl");
            i++;
        }

        Composite compo;
        if (myInst != null) {
            compo = apam.createComposite(myInst.getComposite(), "Composite2", null);
        } else {
            System.out.println("my inst pas instnacie");
            compo = apam.createComposite(apam.getApplication("TestS1").getMainComposite(), "Composite2", null);
            if (compo == null) {
                System.out.println("pas pu creer composite 2");
                return;
            }
        }

        ASMImplBroker implBroker = apam.getImplBroker();

        implBroker.addImpl(compo, "ApamS4impl", "S4Impl", "S4In", null);
        System.out.println("S2 called " + s);
        s4_1.callS4("depuis S4_1 ");
        s4_2.callS4("depuis S4_2 ");

    }

    public void apamStop() {
        // TODO Auto-generated method stub

    }

    public void apamRelease() {
        // TODO Auto-generated method stub

    }
}
