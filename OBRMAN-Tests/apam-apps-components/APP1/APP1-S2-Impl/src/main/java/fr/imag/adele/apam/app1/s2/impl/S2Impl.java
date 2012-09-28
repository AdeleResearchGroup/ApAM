package fr.imag.adele.apam.app1.s2.impl;

import fr.imag.adele.apam.app1.s2.spec.S2;
import fr.imag.adele.apam.app1.s3.spec.S3;

public class S2Impl implements S2 {

    private S3 s3;

    @Override
    public void call(String texte) {
        texte = texte + " >>> " + S2.class.getSimpleName();
        System.out.println(texte);
        s3.call(texte);

    }
}
