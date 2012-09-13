package fr.imag.adele.apam.app2.main.impl;

import fr.imag.adele.apam.app2.spec.App2Spec;

public class MainApp2 implements App2Spec {

    @Override
    public void call(String texte) {
        texte = texte + " >>> " + MainApp2.class.getSimpleName();
        System.out.println(texte + " # {End Of the Call");
    }

}
