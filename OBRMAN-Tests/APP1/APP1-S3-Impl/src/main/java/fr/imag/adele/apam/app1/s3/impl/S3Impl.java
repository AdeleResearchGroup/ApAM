package fr.imag.adele.apam.app1.s3.impl;

import fr.imag.adele.apam.app1.s3.spec.S3;

public class S3Impl implements S3 {

    @Override
    public void call(String texte) {
        texte = texte + " >>> " + S3.class.getSimpleName();
        System.out.println(texte + " # {End Of the Call");
    }
}
