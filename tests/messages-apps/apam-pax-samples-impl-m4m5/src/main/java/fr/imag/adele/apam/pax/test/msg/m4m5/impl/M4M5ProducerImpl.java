package fr.imag.adele.apam.pax.test.msg.m4m5.impl;

import fr.imag.adele.apam.pax.test.msg.M4;
import fr.imag.adele.apam.pax.test.msg.M5;

public class M4M5ProducerImpl{

    public M4 produceM4(String msg){
        return new M4(msg);
    }

    public M5 produceM5(String msg){
        return new M5(msg);
    }

    public String whoami()
    {
        return this.getClass().getName();
    }
    
    public void start(){
    	System.out.println("Starting:"+this.getClass().getName());
    }
    
    public void stop(){
    	System.out.println("Stopping:"+this.getClass().getName());
    }

}
