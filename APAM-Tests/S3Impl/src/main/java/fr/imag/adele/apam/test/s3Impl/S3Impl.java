package fr.imag.adele.apam.test.s3Impl;

import fr.imag.adele.apam.test.s3.S3_1;
import fr.imag.adele.apam.test.s3.S3_2;

public class S3Impl implements S3_1, S3_2 {
    // ipojo injected
    // S s2 ;
    @Override
    public void callS3_1(String s) {
        System.out.println("S3_1 called " + s);
        // s2.callS2 ("from S1Impl") ;
    }

    @Override
    public void callS3_2(String s) {
        System.out.println("S3_2 called " + s);
        // s2.callS2 ("from S1Impl") ;
    }
}
