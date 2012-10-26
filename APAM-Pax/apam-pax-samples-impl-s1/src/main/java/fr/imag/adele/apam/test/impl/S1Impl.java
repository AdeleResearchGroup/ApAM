package fr.imag.adele.apam.test.impl;

import java.util.Set;

import fr.imag.adele.apam.test.iface.S1;
import fr.imag.adele.apam.test.iface.S2;
import fr.imag.adele.apam.test.iface.S3;
import fr.imag.adele.apam.test.iface.device.Eletronic;

public class S1Impl implements S1
{

	Eletronic simpleDevice110v;
	S2 s2;
    S3 s3;
    
    Set<Eletronic> eletronicInstancesInSet;
    
    Eletronic[] eletronicInstancesInArray;
    
    Set<Eletronic> eletronicInstancesConstraintsInstance;

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

	public Eletronic getSimpleDevice110v() {
		return simpleDevice110v;
	}

	public void setSimpleDevice110v(Eletronic simpleDevice110v) {
		this.simpleDevice110v = simpleDevice110v;
	}

	public Set<Eletronic> getEletronicInstancesInSet() {
		return eletronicInstancesInSet;
	}

	public void setEletronicInstancesInSet(Set<Eletronic> eletronicInstancesInSet) {
		this.eletronicInstancesInSet = eletronicInstancesInSet;
	}

	public Eletronic[] getEletronicInstancesInArray() {
		return eletronicInstancesInArray;
	}

	public void setEletronicInstancesInArray(Eletronic[] eletronicInstancesInArray) {
		this.eletronicInstancesInArray = eletronicInstancesInArray;
	}

	public Set<Eletronic> getEletronicInstancesConstraintsInstance() {
		return eletronicInstancesConstraintsInstance;
	}

	public void setEletronicInstancesConstraintsInstance(
			Set<Eletronic> eletronicInstancesConstraintsInstance) {
		this.eletronicInstancesConstraintsInstance = eletronicInstancesConstraintsInstance;
	}

}
