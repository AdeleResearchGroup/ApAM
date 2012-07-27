/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.imag.adele.apam.message;


public interface AbstractConsumer<D> {

    public void pushMessage(Message<D> message);
    
    public void pushData(D data);
   
}
