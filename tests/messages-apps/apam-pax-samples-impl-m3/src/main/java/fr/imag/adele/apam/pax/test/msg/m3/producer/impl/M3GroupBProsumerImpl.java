package fr.imag.adele.apam.pax.test.msg.m3.producer.impl;

import fr.imag.adele.apam.pax.test.msg.M3;
import fr.imag.adele.apam.pax.test.msg.device.EletronicMsg;

import java.util.Queue;


public class M3GroupBProsumerImpl {

    public M3 produceM3(String msg){
        return new M3(msg);
    }

	Queue<EletronicMsg> element;

    Queue<M3> d;
	
	Queue<M3> e;
	
    public String whoami()
    {
        return this.getClass().getName();
    }

	public EletronicMsg getElement() {
		return element.poll();
	}

	public M3 getD() {
		return d.poll();
	}

	public M3 getE() {
		return e.poll();
	}

}
