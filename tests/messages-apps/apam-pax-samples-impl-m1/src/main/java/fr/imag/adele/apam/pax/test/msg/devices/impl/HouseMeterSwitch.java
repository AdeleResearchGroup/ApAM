package fr.imag.adele.apam.pax.test.msg.devices.impl;


import fr.imag.adele.apam.pax.test.msg.device.HouseMeterMsg;

public class HouseMeterSwitch{

    public HouseMeterMsg produceHouseMeterMsg(String msg){
        return new HouseMeterMsg(msg);
    }

}
