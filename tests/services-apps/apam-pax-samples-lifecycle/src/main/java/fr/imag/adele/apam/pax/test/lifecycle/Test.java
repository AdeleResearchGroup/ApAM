package fr.imag.adele.apam.pax.test.lifecycle;

public class Test implements Service {

	/**
	 * Injected property to force component to self-destruction
	 */
	private boolean selfDestroy;
	
	
	/**
	 * Component creation callback
	 */
	@SuppressWarnings("unused")
	private void start() {
		if (selfDestroy)
			throw new IllegalArgumentException("Exception in component start");
		
		System.out.println("Hello APAM");
	}
	
	/**
	 * Component remove callback
	 */
	private void stop()  {
		System.out.println("Goodbye APAM");
	}
	
	
	public void action() {
		if (selfDestroy)
			stop();
		
		System.out.println("At your service master");
	}
}
