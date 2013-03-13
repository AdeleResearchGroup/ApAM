package fr.imag.adele.apam.pax.test.msg.m1.producer.impl;

import java.util.List;
import java.util.Queue;
import java.util.Set;

import fr.imag.adele.apam.pax.test.msg.device.EletronicMsg;

public class M1ConsumerImpl01{

	Queue<EletronicMsg> queue;
	
	List<EletronicMsg> list;
	
	Set<EletronicMsg> set;

	public Queue<EletronicMsg> getQueue() {
		return queue;
	}

	public void setQueue(Queue<EletronicMsg> queue) {
		this.queue = queue;
	}

	public List<EletronicMsg> getList() {
		return list;
	}

	public void setList(List<EletronicMsg> list) {
		this.list = list;
	}

	public Set<EletronicMsg> getSet() {
		return set;
	}

	public void setSet(Set<EletronicMsg> set) {
		this.set = set;
	}


	
}
