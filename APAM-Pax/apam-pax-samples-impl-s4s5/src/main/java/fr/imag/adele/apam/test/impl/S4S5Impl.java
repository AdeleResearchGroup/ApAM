package fr.imag.adele.apam.test.impl;

import fr.imag.adele.apam.test.iface.S4;
import fr.imag.adele.apam.test.iface.S5;

public class S4S5Impl implements S4,S5
{

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
