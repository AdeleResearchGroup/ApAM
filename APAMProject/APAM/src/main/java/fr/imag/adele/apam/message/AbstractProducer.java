/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.imag.adele.apam.message;

import java.util.List;


public interface AbstractProducer<D> {

	public Message<D> getMessage();
 
	public List<Message<D>> getAllMessages();
	
	public D getData();
	
}
