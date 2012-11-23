package fr.imag.adele.apam.test.message.producer;

import fr.imag.adele.apam.test.message.M1;
import fr.imag.adele.apam.test.message.M2;

public class M12Producer {
    
    
    public M1 produceM1() {
        double a =Math.random();
        double b = Math.random();
        return new M1(a,b) ;
    }

    public M2 produceM2() {
        double a =Math.random();
        double b = Math.random();
        return new M2(a,b) ;
    }

    public void start() {
        System.out.println("M12 Producer started");
    }

    public void stop() {
       System.out.println("M12 Producer stopped");
    }

}
