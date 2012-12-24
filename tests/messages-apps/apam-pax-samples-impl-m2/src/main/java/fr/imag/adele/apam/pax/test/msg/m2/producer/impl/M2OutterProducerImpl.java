package fr.imag.adele.apam.pax.test.msg.m2.producer.impl;


import fr.imag.adele.apam.pax.test.msg.M2;

public class M2OutterProducerImpl  {

    public M2 produceM2(String msg){
        return new M2(msg);
    }
}
