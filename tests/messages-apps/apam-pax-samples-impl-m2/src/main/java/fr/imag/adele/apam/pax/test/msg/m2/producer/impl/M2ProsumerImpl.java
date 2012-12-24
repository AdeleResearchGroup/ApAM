package fr.imag.adele.apam.pax.test.msg.m2.producer.impl;

import fr.imag.adele.apam.pax.test.msg.M1;
import fr.imag.adele.apam.pax.test.msg.M2;
import fr.imag.adele.apam.pax.test.msg.M4;
import fr.imag.adele.apam.pax.test.msg.M5;
import fr.imag.adele.apam.pax.test.msg.device.EletronicMsg;
import fr.imag.adele.apam.pax.test.msg.device.HouseMeterMsg;

import java.util.Queue;

public class M2ProsumerImpl{

    Queue<EletronicMsg> deadMansSwitch;

    Queue<M4> s4;
    Queue<M5> s5;

    Queue<HouseMeterMsg> houseMeter;

    Queue<M1> inner;

    public M2 produceM2(String msg){
        return new M2(msg);
    }

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

    public HouseMeterMsg getHouseMeter() {
        return houseMeter.poll();
    }

    public EletronicMsg getDeadMansSwitch() {
        return deadMansSwitch.poll();
    }

}
