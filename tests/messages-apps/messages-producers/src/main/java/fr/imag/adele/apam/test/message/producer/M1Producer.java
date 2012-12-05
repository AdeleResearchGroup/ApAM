package fr.imag.adele.apam.test.message.producer;

import fr.imag.adele.apam.test.message.M1;

public class M1Producer {
    
    
    public M1 produceM1() {
        double a =Math.random();
        double b = Math.random();
        return new M1(a,b) ;
    }

    public void start() {
        System.out.println("M1 Producer started");
    }

    public void stop() {
       System.out.println("M1 Producer stopped");
    }

}
