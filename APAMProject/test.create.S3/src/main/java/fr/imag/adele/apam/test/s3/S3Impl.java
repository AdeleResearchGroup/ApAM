package fr.imag.adele.apam.test.s3;

public class S3Impl implements S3 {
    // ipojo injected
    // S s2 ;
    public void callS3(String s) {
        System.out.println("S3 called " + s);
        // s2.callS2 ("from S1Impl") ;
    }
}
