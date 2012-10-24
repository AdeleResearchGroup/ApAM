package fr.imag.adele.apam.test.impl;

import fr.imag.adele.apam.test.iface.S3;


public class S3Impl implements S3
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
