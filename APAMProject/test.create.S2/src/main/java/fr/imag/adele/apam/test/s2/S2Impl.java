package fr.imag.adele.apam.test.s2;

import java.util.Set;

import fr.imag.adele.apam.apamAPI.ASMImplBroker;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.apamAPI.ApamComponent;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.test.s3.S3;
import fr.imag.adele.apam.test.s4.S4;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;

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

    // @Override
    public void callBackS2(String s) {
        System.out.println("Back in S2 : " + s);
    }

    // @Override
    public void callS2(String s) {
        int i = 1;
        for (S3 s3 : s3s) {
            s3.callS3(i + " from S2Impl");
            i++;
        }

        Composite compo2 = null;
        Composite compo3 = null;
        if (myInst != null) {
            compo2 = apam.createComposite(myInst.getComposite(), "Compo2", null);
            compo3 = apam.createComposite(myInst.getComposite(), "Compo3", null);
        }
        if (compo2 == null) {
            System.out.println("composite 2 n'a pas pu etre cree");
            return;
        }
        if (compo3 == null) {
            System.out.println("compo 3 n'a pas pu etre cree");
            return;
        }

        ASMImplBroker implBroker = apam.getImplBroker();

        Attributes c2Attrs = new AttributesImpl();
        c2Attrs.setProperty(Attributes.SHARED, Attributes.COMPOSITE);
        implBroker.addImpl(compo2, "ApamS4impl", "S4Impl", "S4In", c2Attrs);

        Attributes c3Attrs = new AttributesImpl();
        c3Attrs.setProperty(Attributes.SHARED, Attributes.LOCAL);
        implBroker.addImpl(compo3, "ApamS5impl", "S5Impl", "S5", c3Attrs);

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
