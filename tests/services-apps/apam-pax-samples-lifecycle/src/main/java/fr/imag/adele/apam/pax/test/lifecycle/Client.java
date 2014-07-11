package fr.imag.adele.apam.pax.test.lifecycle;

import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;

public class Client implements ApamComponent {

	/**
	 * This is the injected service dependency
	 */
	private Service service;
	
	@Override
	public void apamInit(Instance apamInstance) {
		service.action();
	}


	@Override
	public void apamRemove() {
	}
	
	public void bind(Service service) {
		System.out.println("field injection bind "+service+" "+this.service);
	}
	
	public void unbind(Instance instance) {
		System.out.println("field injection unbind  was "+instance.getServiceObject());
	}

}
