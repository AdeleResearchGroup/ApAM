package fr.imag.adele.apam.test.message.consumer;

import java.util.Queue;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.test.message.M1;

public class C1ImplAutonome {
    Thread    t;
    boolean   running = true;

    Queue<String> queueM1;

    public void start() {
        System.out.println("Consumer started");
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    if (queueM1 == null) {
                        System.err.println("no producer wired");
                    } else {
                        if (!queueM1.isEmpty())
                            System.err.println(" value" + queueM1.poll());
                    }
                   

                }
                System.out.println("Out!");
            }

        });
        t.start();
    }

    public void consumeM1(M1 m1) {
        System.err.println("-- PUSH : " + m1.getMoy());
    }

    public void stop() {
        running = false;
        System.out.println("Consumer stopped");
    }

    public void bindProducer(Instance inst) {
        System.out.println("---------------BIND-------------------");
        System.out.println("-------------" + inst.getName() + " -------------------");
    }

    public void unBindProducer(Instance inst) {
        System.out.println("---------------UNBIND-------------------");
        System.out.println("-------------" + inst.getName() + " -------------------");
    }

}
