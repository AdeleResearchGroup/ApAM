package fr.imag.adele.apam.test.message.consumer;

import java.util.Queue;

import fr.imag.adele.apam.test.message.M1;
import fr.imag.adele.apam.test.message.M2;

public class M12ConsumerPull {


    Queue<M1> queueM1;
    
    Queue<M2> queueM2;
    
    

    public void start() {
        System.out.println("Consumer M12 started");
    }
    
    public  Queue<M1> getQueueM1() {
       return queueM1;
    }

    public  Queue<M2> getQueueM2() {
        return queueM2;
     }
    
 
    
    public void stop() {
        System.out.println("Consumer M12 stopped");
    }
}
