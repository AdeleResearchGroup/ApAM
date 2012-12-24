package fr.imag.adele.apam.pax.test.msg.devices.impl;


import fr.imag.adele.apam.pax.test.msg.device.EletronicMsg;


public class GenericSwitch {

    public EletronicMsg produceEletronicMsg(String msg){
        return new EletronicMsg(msg);
    }

}
