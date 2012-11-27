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
    
    //Called by APAM when an instance of this implementation is created
    public void start(){
        System.out.println("HelloService Start");
    }
    
    // Called by APAM when an instance of this implementation is removed
    public void stop(){
        System.out.println("HelloService Stop");
    }
}
