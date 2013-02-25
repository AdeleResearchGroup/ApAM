package fr.imag.adele.apam.pax.test.performance.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import fr.imag.adele.apam.pax.test.performance.Fibonacci;
import fr.imag.adele.apam.pax.test.performance.FibonacciRecursive;

public class FibonacciServiceFactory implements ServiceFactory {

	@Override
	public Object getService(Bundle bundle, ServiceRegistration registration) {
		// TODO Auto-generated method stub
		Fibonacci fib=new FibonacciRecursive(bundle.getBundleContext());
		return fib;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration registration,
			Object service) {
		// TODO Auto-generated method stub
		
	}

}
