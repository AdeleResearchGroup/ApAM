package fr.imag.adele.apam.test.message.producer;

import fr.imag.adele.apam.test.message.M2;

public class M2Producer {
    
    
    public M2 produceM2() {
        double a =Math.random();
        double b = Math.random();
        return new M2(a,b) ;
    }

    public void start() {
        System.out.println("M2 Producer started");
    }

    public void stop() {
       System.out.println("M2 Producer stopped");
    }

}
