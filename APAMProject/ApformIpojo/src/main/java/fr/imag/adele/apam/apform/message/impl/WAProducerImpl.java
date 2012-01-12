package fr.imag.adele.apam.apform.message.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.osgi.service.wireadmin.Producer;
import org.osgi.service.wireadmin.Wire;

import fr.imag.adele.apam.message.Message;

public class WAProducerImpl implements Producer {

	//injected by iPOJO
	private String producerPid;
	
	//Injected by iPOJO
	private String[] flavors;

	
	private List<Wire> wires;

	@Override
	public Object polled(Wire wire) {
		/*
		 * Pull is not managed
		 */
		return null;
	}

	@Override
	public void consumersConnected(Wire[] wires) {
		if (wires!=null){
			this.wires = Arrays.asList(wires);
		}else {
			this.wires = null;
		}
		
	}

	public void sendMessage(String type, Object data){	
		List<String> flavorsList = Arrays.asList(flavors);
		for (Wire wire : wires) {
			if (flavorsList.contains(type)){
				if (!(data instanceof Message)){
					data= new Message(new Date().getTime(), data);	
				}
				Message message=(Message) data;
				wire.update(message);	
			}else{
				System.err.println("WARNING : You are trying to send an unssuported message type : " + type);
			}
		}
	}
	
	
	
	/**
	 * Start this Producer
	 */
	public void start(){
		
	}
	
		
	/**
	 * Stop this Producer
	 */
	public void stop(){
		
	}
	
	
}
