package fr.imag.adele.apam.test.message.producer;

import java.util.Properties;

import fr.imag.adele.apam.message.Message;
import fr.imag.adele.apam.test.message.M1;

public class P1Impl {
    
    
    public Message<M1> produceM1(Properties properties) {
        double a =Math.random();
        double b = Math.random();
        Message<M1> m = new Message<M1>(new M1(a, b));
        if (properties!=null)
            m.getProperties().putAll(properties);
        return m ;
    }

    public void start() {
        System.out.println("Producer started");
    }

    public void stop() {
       System.out.println("Producer stopped");
    }
    
 
}
