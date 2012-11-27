package fr.imag.adele.apam.hello.world.impl;

import fr.imag.adele.apam.hello.world.spec.HelloService;

public class HelloImplementation implements HelloService{


    String lang;

    String expr;
    /**
     * @see HelloService#sayHello(String)
     */
    @Override
    public void sayHello(String texte) {
        System.out.println(expr +", " + texte + " con la nueva version Hola Mundo, ");
        System.out.println("Felicidades!");

    }

    @Override
    public String getLang() {
        return lang;
    }

    public void setLang(String language){
        System.out.println("The language has been changed to " + language );
        lang = language;
    }

    //Called by APAM when an instance of this implementation is created
    public void start(){
        System.out.println("--> " + lang +  " HelloService Start");
    }

    // Called by APAM when an instance of this implementation is removed
    public void stop(){
        System.out.println("--> HelloService Stop ");
    }

}
