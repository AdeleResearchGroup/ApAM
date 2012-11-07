package fr.imag.adele.apam.pax.test.impl;

import java.util.List;
import java.util.Set;

import fr.imag.adele.apam.pax.test.iface.S1;
import fr.imag.adele.apam.pax.test.iface.S2;
import fr.imag.adele.apam.pax.test.iface.S3;
import fr.imag.adele.apam.pax.test.iface.device.Eletronic;

public class S1Impl implements S1
{

	String stateInternal;
	String stateNotInternal;
	
	Eletronic simpleDevice110v;
	
	S2 s2;
    S3 s3;
    
    Set<Eletronic> eletronicInstancesInSet;
    
    Eletronic[] eletronicInstancesInArray;
    
    Set<Eletronic> eletronicInstancesConstraintsInstance;
    
    Eletronic devicePreference110v;
    
    Set<Eletronic> devicesPreference110v;
    
    Eletronic deviceConstraint110v;
    
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

	public String getStateNotInternal() {
		return stateNotInternal;
	}

	public void setStateNotInternal(String stateNotInternal) {
		this.stateNotInternal = stateNotInternal;
	}

	public String getStateInternal() {
		return stateInternal;
	}

	public void setStateInternal(String stateInternal) {
		this.stateInternal = stateInternal;
	}

	public Eletronic getDevicePreference110v() {
		return devicePreference110v;
	}

	public void setDevicePreference110v(Eletronic devicePreference110v) {
		this.devicePreference110v = devicePreference110v;
	}

	public Eletronic getDeviceConstraint110v() {
		return deviceConstraint110v;
	}

	public void setDeviceConstraint110v(Eletronic deviceConstraint110v) {
		this.deviceConstraint110v = deviceConstraint110v;
	}

	public Set<Eletronic> getDevicesPreference110v() {
		return devicesPreference110v;
	}

	public void setDevicesPreference110v(Set<Eletronic> devicesPreference110v) {
		this.devicesPreference110v = devicesPreference110v;
	}

}
