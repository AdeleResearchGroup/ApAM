package fr.imag.adele.apam.test.impl;

import fr.imag.adele.apam.test.iface.S1;
import fr.imag.adele.apam.test.iface.S2;
import fr.imag.adele.apam.test.iface.S3;

public class S1Impl implements S1
{

    S2 s2;
    S3 s3;

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
