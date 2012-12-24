package fr.imag.adele.apam.pax.test.msg.device.dead;


import fr.imag.adele.apam.pax.test.msg.device.EletronicMsg;

public class DeadsManSwitch {

    public EletronicMsg produceElectronicMsg(String msg){
        return new EletronicMsg(msg);
    }
	
}
