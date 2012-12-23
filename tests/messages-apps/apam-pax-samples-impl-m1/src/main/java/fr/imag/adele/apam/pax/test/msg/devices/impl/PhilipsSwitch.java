package fr.imag.adele.apam.pax.test.msg.devices.impl;

import fr.imag.adele.apam.pax.test.msg.device.EletronicMsg;


public class PhilipsSwitch {

    public EletronicMsg produceElectronicMsg(String msg){
        return new EletronicMsg(msg);
    }


}
