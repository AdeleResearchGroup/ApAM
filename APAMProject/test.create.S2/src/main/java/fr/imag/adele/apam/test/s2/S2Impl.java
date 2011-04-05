package fr.imag.adele.apam.test.s2;

import fr.imag.adele.apam.test.s3.S3;

public class S2Impl implements S2 {

    // ipojo injected
    S3 s3;

    public void callS2(String s) {
        System.out.println("S2 called " + s);
        s3.callS3("from S2Impl");
    }
}
