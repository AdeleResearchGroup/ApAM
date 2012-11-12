package fr.imag.adele.apam.test.s1Impl;

import java.util.List;

import fr.imag.adele.apam.test.s1.S1;
import fr.imag.adele.apam.test.s2.S2;
import fr.imag.adele.apam.test.s3.S3_1;
import fr.imag.adele.apam.test.s3.S3_2;

public class S1Impl implements S1 {

    // Apam handler injected
    S2       s2Spec;
    S2       s2Interf;
    S2       s2Msg;

    S3_1       s3_1;
    S3_2     s3_2;

    List<S3_1> s3List;
    S3_1[]     s3ListBis;

    S2         lastS2;

    public void callS1(String s) {

        System.out.println("=== In fr.imag.adele.apam.test.s1Impl; S2  s2Spec; " + s2Spec.getName());
        System.out.println("=== In fr.imag.adele.apam.test.s1Impl; S2  s2Interf; " + s2Interf.getName());
        System.out.println("=== In fr.imag.adele.apam.test.s1Impl; S3_1 " + s3_1.getName());
        System.out.println("=== In fr.imag.adele.apam.test.s1Impl; S3_2 " + s3_2.getName());

        s2Spec.callS2("From S1Impl to S2 ...") ;
        
        System.out.print("=== In S1Impl; S3_1  s3List :");
        for (S3_1 s3 : s3List) {
            System.out.print("     " + s3.getName());
            s3.callS3_1("freom S1Impl, s3List ") ;
        }
        System.out.println("");
        System.out.print("=== In S1Impl; S3_1  s3ListBis :");
        for (S3_1 s3 : s3ListBis) {
            System.out.print("     " + s3.getName());
        }
        System.out.println("");

        System.out.println("=== In S1Impl; lastS2 " + lastS2.getName());

        //        System.out.println("S2  s2Spec; " + s2Spec);

        //        System.out.println("S1 called " + s);
        //        s2.callS2("from S1Impl internal");
        //        s4.callS4_final("From S1Impl external");
    }
}
