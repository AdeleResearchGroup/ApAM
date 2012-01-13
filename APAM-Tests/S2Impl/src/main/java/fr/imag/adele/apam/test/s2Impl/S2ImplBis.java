package fr.imag.adele.apam.test.s2Impl;

import java.util.Set;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.test.s2.S2;
import fr.imag.adele.apam.test.s4.S4;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.apam.util.AttributesImpl;

public class S2ImplBis implements S2, ApamComponent {

    // Apam injected
    Apam     apam;
    S4       s4Bis;
//    S4      s4_2;
//    Set<S3> s3s;

    Instance myInst;

    @Override
    public void apamStart(Instance inst) {
        myInst = inst;
        System.out.println("S2ImplBis Started : " + inst.getName());
    }

    // @Override
    @Override
    public void callBackS2(String s) {
        System.out.println("Back in S2 : " + s);
    }

    // @Override
    @Override
    public void callS2(String s) {
//        int i = 1;
//        for (S3 s3 : s3s) {
//            s3.callS3(i + " from S2Impl");
//            i++;
//        }

        Attributes c2Attrs = new AttributesImpl();
        c2Attrs.setProperty(CST.A_SHARED, CST.V_TRUE);

        Attributes c3Attrs = new AttributesImpl();
        c3Attrs.setProperty(CST.A_SHARED, CST.V_FALSE);
        // implBroker.addImpl(compo3, "ApamS5impl", "S5Impl", "S5", c3Attrs);

        System.out.println("S2 Bis called " + s);
        if (s4Bis != null)
            s4Bis.callS4("depuis S2 Bis (s4Bis) ");
//        if (s4_2 != null)
//            s4_2.callS4("depuis S2 (s4_2) ");
    }

    @Override
    public void apamStop() {
    }

    @Override
    public void apamRelease() {
    }
}
