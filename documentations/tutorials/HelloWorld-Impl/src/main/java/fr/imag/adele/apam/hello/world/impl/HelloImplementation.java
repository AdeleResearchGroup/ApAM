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
    
    public void go(){
        sayHello("ApAM");
    }
    
    public void bye(){
        System.out.println("bye bye!");
    }
}
