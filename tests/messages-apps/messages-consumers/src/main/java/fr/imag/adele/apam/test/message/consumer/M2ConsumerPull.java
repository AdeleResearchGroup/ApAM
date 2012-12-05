package fr.imag.adele.apam.test.message.consumer;

import java.util.Queue;

import fr.imag.adele.apam.test.message.M2;

public class M2ConsumerPull {

    Queue<M2> queueM2;

    public void start() {
        System.out.println("Consumer M2 started");
    }
    
    public  Queue<M2> getQueue() {
       return queueM2;
    }

    public void stop() {
        System.out.println("Consumer M2 stopped");
    }
}
