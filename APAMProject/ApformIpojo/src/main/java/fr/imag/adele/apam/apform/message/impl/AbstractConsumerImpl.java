/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.imag.adele.apam.apform.message.impl;

import java.util.List;

import org.osgi.service.wireadmin.Wire;

import fr.imag.adele.apam.message.AbstractConsumer;
import fr.imag.adele.apam.message.Message;

public class AbstractConsumerImpl<D> implements AbstractConsumer<D> {

    
    private List<Wire> wires;

    @Override
    public void sendMessage(Message<D> message) {
        for (Wire wire : wires) {
            wire.update(message);
        }
    }

 

	@Override
	public void sendData(D data) {
		// TODO Auto-generated method stub
		
	}
	
	
	//@Override
	//public void updated(Wire wire, Object value) {
	//
	////TODO VERIFICATION DES TYPES:       if (value.getClass().isAssignableFrom(last_Message.getClass())) {
//	        long message_time =  new Date().getTime();
//	        last_Message = new Message(message_time,(D) value);
//	        switch (strategy){
//	            case PUSH :
//	                pushOnMethd(last_Message);
//	                break;
//	             case PULL:
	//
//	                break;
//	        }
//	        
////	    }
	//   

    
}

