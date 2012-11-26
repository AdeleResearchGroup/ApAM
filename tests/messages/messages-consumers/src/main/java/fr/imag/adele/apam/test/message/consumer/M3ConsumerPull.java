package fr.imag.adele.apam.test.message.consumer;

import java.util.Queue;

import fr.imag.adele.apam.test.message.M3;

public class M3ConsumerPull {

    Queue<M3> queueM3;

    public void start() {
        System.out.println("Consumer M3 started");
    }
    
    public  Queue<M3> getQueue() {
       return queueM3;
    }

    public void stop() {
        System.out.println("Consumer M3 stopped");
    }
}
