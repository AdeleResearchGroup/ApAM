package fr.imag.adele.apam.hello.world.impl;

import fr.imag.adele.apam.hello.world.spec.HelloService;

public class HelloImplementation implements HelloService{

    /**
     * @see HelloService#sayHello(String) 
     */
    @Override
    public void sayHello(String texte) {
        System.out.println("Hello, " + texte);
    }
}
