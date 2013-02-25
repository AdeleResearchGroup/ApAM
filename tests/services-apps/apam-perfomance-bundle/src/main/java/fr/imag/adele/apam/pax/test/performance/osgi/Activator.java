package fr.imag.adele.apam.pax.test.performance.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import fr.imag.adele.apam.pax.test.performance.Fibonacci;
import fr.imag.adele.apam.pax.test.performance.Main;

public class Activator 	implements BundleActivator {

	ServiceRegistration fibonacciServiceRegistration;
	ServiceRegistration mainServiceRegistration;
	
	@Override
	public void start(BundleContext context) throws Exception {
		
		FibonacciServiceFactory factory=new FibonacciServiceFactory();
		MainServiceFactory mainFactory=new MainServiceFactory();
		fibonacciServiceRegistration=context.registerService(Fibonacci.class.getName(), factory, null);
		mainServiceRegistration=context.registerService(Main.class.getName(), mainFactory, null);
		
		((Main)context.getService(mainServiceRegistration.getReference())).start();
		
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub
		fibonacciServiceRegistration.unregister();
		mainServiceRegistration.unregister();
	}

}
