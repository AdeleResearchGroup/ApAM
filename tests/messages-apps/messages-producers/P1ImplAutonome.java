package fr.imag.adele.apam.test.message.producer;

import java.util.Properties;

import fr.imag.adele.apam.message.Message;
import fr.imag.adele.apam.test.message.M1;

public class P1ImplAutonome implements MyProducer {
    Thread t;
    boolean running=true;
    
    public M1 produceM1(Properties prop) {
        double a =Math.random();
        double b = Math.random();
        Message<M1> m = new Message<M1>(new M1(a, b));
        m.getProperties().setProperty("vendor", "mehdi");
        return (new M1(a,b)) ;
    }

    public void start() {
        System.out.println("Producer started");
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                int i=0;
                while (running) {
                    M1 m1 = produceM1(null);
                    System.out.println(" produce message M1 : " + m1.getMoy() );
                    i++;
                   
                }
            }
        });
        t.start();
    }

    public void stop() {
        running = false;
       System.out.println("Producer stopped");
    }
}
