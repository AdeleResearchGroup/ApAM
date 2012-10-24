package fr.imag.adele.apam.test.impl;
import fr.imag.adele.apam.test.iface.S2;
import fr.imag.adele.apam.test.iface.S4;
import fr.imag.adele.apam.test.iface.S5;

public class S2Impl implements S2
{

    S4 s4;
    S5 s5;

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
