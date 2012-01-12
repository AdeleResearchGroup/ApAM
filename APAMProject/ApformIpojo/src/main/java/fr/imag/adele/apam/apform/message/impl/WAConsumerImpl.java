package fr.imag.adele.apam.apform.message.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.wireadmin.Consumer;
import org.osgi.service.wireadmin.Wire;

import fr.imag.adele.apam.message.Message;

public class WAConsumerImpl implements Consumer{
	
	//injected by iPOJO
	private String consumerPid;

	private List<Wire> wires;
	
	//injected by iPOJO
	private Map<String,List<MessageAbstractProducer>> flavorForAbstractProducer ;
	
	//Injected by iPOJO
	private String[] flavors;

	
	
	@Override
	public void updated(Wire wire, Object value) {
		if (!(value instanceof Message)){
			value = new Message(null,value);
		}
		Message message = (Message) value;
		message.setReceiveTimeStamp(new Date().getTime());
		message.addProperties(wire.getProperties());
		if (flavorForAbstractProducer.get(message.getDataType())!=null){
			for (MessageAbstractProducer messageProduer  : flavorForAbstractProducer.get(message.getDataType())) {
				messageProduer.addMessage(wire, message);
			}
		}else {
			System.err.println("Consumer configuration error: This case should never been happen! it meens that any consumer is looking for this type");
		}
			
		
	}

	@Override
	public void producersConnected(Wire[] wires) {
		if (wires!=null){
			this.wires = Arrays.asList(wires);
		}else {
			this.wires = null;
		}
		 
	}


	
	/**
	 * Start this consumer
	 */
	public void start(){
		
	}
	
	
	
	/**
	 * Stop this consumer
	 */
	public void stop(){
		
	}
	
	
	
	
}
