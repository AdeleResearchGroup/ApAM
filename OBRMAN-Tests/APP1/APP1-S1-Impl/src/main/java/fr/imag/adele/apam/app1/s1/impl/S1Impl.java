package fr.imag.adele.apam.app1.s1.impl;

import fr.imag.adele.apam.app1.s1.spec.S1;
import fr.imag.adele.apam.app1.spec.App2Spec;

public class S1Impl implements S1 {

    private App2Spec app2;

    @Override
    public void call(String texte) {
        texte = texte + " >>> " + S1.class.getSimpleName();
        System.out.println(texte);
        app2.call(texte);
    }
}
