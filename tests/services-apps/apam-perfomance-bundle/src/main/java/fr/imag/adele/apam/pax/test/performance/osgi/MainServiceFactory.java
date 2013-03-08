package fr.imag.adele.apam.pax.test.performance.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

public class MainServiceFactory implements ServiceFactory {
	
	@Override
	public Object getService(Bundle bundle, ServiceRegistration registration) {
		// TODO Auto-generated method stub
		//Main main=new Main(bundle.getBundleContext());
		
		return null;//main;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration registration,
			Object service) {
		
	}

}
