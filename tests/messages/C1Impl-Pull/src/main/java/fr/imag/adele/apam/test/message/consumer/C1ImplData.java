package fr.imag.adele.apam.test.message.consumer;

import java.util.Queue;

import fr.imag.adele.apam.test.message.M1;

public class C1ImplData {
    Thread    t;

    Queue<M1> queueM1;

    public void start() {
        System.out.println("Consumer started");
    }
    
    public  Queue<M1> getQueue() {
       return queueM1;
    }

    public void stop() {
        System.out.println("Consumer stopped");
    }
}
