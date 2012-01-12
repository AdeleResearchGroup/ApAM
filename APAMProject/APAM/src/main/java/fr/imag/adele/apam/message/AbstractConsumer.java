/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.imag.adele.apam.message;


public interface AbstractConsumer<D> {

    public void sendMessage(Message<D> message);
    
    public void sendData(D data);
   
}
