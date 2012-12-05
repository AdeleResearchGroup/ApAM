package fr.imag.adele.apam.test.message.consumer;

import java.util.Queue;

import fr.imag.adele.apam.test.message.M1;
import fr.imag.adele.apam.test.message.M3;

public class M13ConsumerPull {


    Queue<M1> queueM1;
    
    Queue<M3> queueM3;

    public void start() {
        System.out.println("Consumer M13 started");
    }
    
    public  Queue<M1> getQueueM1() {
       return queueM1;
    }

    public  Queue<M3> getQueueM3() {
        return queueM3;
     }
    
    public void stop() {
        System.out.println("Consumer M13 stopped");
    }
}
