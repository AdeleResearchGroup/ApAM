package fr.imag.adele.apam.test.message.consumer.impl;

import java.util.Queue;

import fr.imag.adele.apam.test.message.M1;

public class C1Impl {
    Thread    t;
    boolean   running = true;

    Queue<M1> queueM1;

    public void start() {
        System.out.println("Consumer started");
        t = new Thread(new Runnable() {
            @Override
            public void run() {

                while (running) {

                    try {
                        Thread.currentThread().sleep(3000);
                        if (queueM1 == null) {
                            System.err.println("no producer wired");
                        } else {
                            if (!queueM1.isEmpty())
                                System.err.println(" value" + queueM1.poll().getMoy());
                        }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();

                    }
                }
            }

        });
        t.start();
    }
    
    public void consumeM1(M1 m1) {
        System.err.println("-- PUSH : "+ m1.getMoy());
    }

    public void stop() {
        running = false;
        System.out.println("Consumer stopped");
    }
}
