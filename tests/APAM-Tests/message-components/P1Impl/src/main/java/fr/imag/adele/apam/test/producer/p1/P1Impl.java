package fr.imag.adele.apam.test.producer.p1;

import fr.imag.adele.apam.message.AbstractConsumer;

public class P1Impl {

	AbstractConsumer<String> producer;
	
	
	public void sendMessage(){
		producer.pushData("Hello from P1Impl");
	}
}
