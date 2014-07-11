package fr.imag.adele.apam.pax.test.lifecycle;

public class ConstructorTest implements Service {

	
	public ConstructorTest() {
		throw new IllegalArgumentException("Exception in component constructor");
	}

	@Override
	public void action() {
		throw new UnsupportedOperationException("Should never be able to invoke this service");
	}

}
