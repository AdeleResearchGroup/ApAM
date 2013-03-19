package fr.imag.adele.apam.pax.test.msg.m1.producer.impl;

import java.util.Queue;

import fr.imag.adele.apam.pax.test.msg.device.EletronicMsg;

public class M1ConsumerImpl02{

	Queue<EletronicMsg> queue;

	public Queue<EletronicMsg> getQueue() {
		return queue;
	}

	public void setQueue(Queue<EletronicMsg> queue) {
		this.queue = queue;
	}
	
	
}
