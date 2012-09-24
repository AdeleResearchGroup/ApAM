/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.imag.adele.apam.message;

import java.util.List;


public interface MessageConsumer<D> {

	public Message<D> pullMessage();
 
	public List<Message<D>> getAllMessages();
	
	public D pull();
	
}
