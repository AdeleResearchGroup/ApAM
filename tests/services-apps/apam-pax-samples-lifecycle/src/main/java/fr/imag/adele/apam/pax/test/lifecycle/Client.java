package fr.imag.adele.apam.pax.test.lifecycle;

public class Client {

	/**
	 * This is the injected service dependency
	 */
	private Service service;
	
	
	@SuppressWarnings("unused")
	private void start() {
		service.action();
	}
}
