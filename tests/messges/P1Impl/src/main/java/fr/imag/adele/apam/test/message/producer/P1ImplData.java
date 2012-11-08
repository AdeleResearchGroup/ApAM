package fr.imag.adele.apam.test.message.producer;

import fr.imag.adele.apam.test.message.M1;

public class P1ImplData {
    Thread t;
    boolean running=true;
    
    public M1 produceM1() {
        double a =Math.random();
        double b = Math.random();
        return new M1(a, b) ;
    }

    public void start() {
        System.out.println("Producer started");
        t = new Thread(new Runnable() {
            @Override
            public void run() {
      
                while (running) {
                    M1 m1 = produceM1();
                    System.out.println(" produce message M1 : " + m1.getMoy() );
                    try {
                        Thread.currentThread().sleep(500);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
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
