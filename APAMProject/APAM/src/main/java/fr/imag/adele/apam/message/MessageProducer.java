/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.imag.adele.apam.message;

import java.util.Map;


public interface MessageProducer<D> {

    public void push(D data, Map<String, Object> metaData);
    
    public void push(D data);
   
}
