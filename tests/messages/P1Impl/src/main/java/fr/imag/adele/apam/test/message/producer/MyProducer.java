package fr.imag.adele.apam.test.message.producer;

import java.util.Properties;

import fr.imag.adele.apam.message.Message;
import fr.imag.adele.apam.test.message.M1;

public interface MyProducer {
    public Message<M1> produceM1(Properties properties) ;
}
