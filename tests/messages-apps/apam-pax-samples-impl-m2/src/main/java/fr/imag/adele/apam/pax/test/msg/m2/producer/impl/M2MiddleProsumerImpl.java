package fr.imag.adele.apam.pax.test.msg.m2.producer.impl;

import fr.imag.adele.apam.pax.test.msg.M2;

import java.util.Queue;

public class M2MiddleProsumerImpl  {

	Queue<M2> outter;

    public M2 produceM2(String msg){
        return new M2(msg);
    }

	public String whoami() {
		return this.getClass().getName();
	}

	public M2 getOutter() {
		return outter.poll();
	}

}
