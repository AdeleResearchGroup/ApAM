package fr.imag.adele.apam.pax.test.implS2;
import fr.imag.adele.apam.pax.test.iface.S1;
import fr.imag.adele.apam.pax.test.iface.S2;
import fr.imag.adele.apam.pax.test.iface.S4;
import fr.imag.adele.apam.pax.test.iface.S5;
import fr.imag.adele.apam.pax.test.iface.device.Eletronic;
import fr.imag.adele.apam.pax.test.iface.device.HouseMeter;

public class S2Impl implements S2
{

	Eletronic deadMansSwitch;
	
    S4 s4;
    S5 s5;
    
    HouseMeter houseMeter;
    
    S1 inner;

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

	public HouseMeter getHouseMeter() {
		return houseMeter;
	}

	public void setHouseMeter(HouseMeter houseMeter) {
		this.houseMeter = houseMeter;
	}

	public Eletronic getDeadMansSwitch() {
		return deadMansSwitch;
	}

	public void setDeadMansSwitch(Eletronic deadMansSwitch) {
		this.deadMansSwitch = deadMansSwitch;
	}

}
