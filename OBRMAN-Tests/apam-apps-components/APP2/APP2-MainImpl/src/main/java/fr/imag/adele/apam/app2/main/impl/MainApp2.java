package fr.imag.adele.apam.app2.main.impl;

import fr.imag.adele.apam.app2.main.spec.App2MainSpec;
import fr.imag.adele.apam.app2.spec.App2Spec;

public class MainApp2 implements App2Spec, App2MainSpec {

    @Override
    public void call(String texte) {
        texte = texte + " >>> " + MainApp2.class.getSimpleName();
        System.out.println(texte + " # {End Of the Call");
    }

    public void callDep(String texte) {
        System.out.println("--- Calling DepApp2 from MainApp2 ---");
        texte = texte + " >>> " + MainApp2.class.getSimpleName();
        System.out.println(texte + " # {End Of the Call");
    }

}
