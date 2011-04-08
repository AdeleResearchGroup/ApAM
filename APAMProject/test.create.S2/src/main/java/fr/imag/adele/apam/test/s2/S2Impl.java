package fr.imag.adele.apam.test.s2;

import java.util.Set;

import fr.imag.adele.apam.test.s3.S3;

public class S2Impl implements S2 {

    // ipojo injected
    Set<S3> s3s;

    public void callS2(String s) {
        System.out.println("S2 called " + s);
        int i = 1;
        for (S3 s3 : s3s) {
            s3.callS3(i + " from S2Impl");
            i++;
        }
    }
}
