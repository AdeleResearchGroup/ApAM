package fr.imag.adele.apam.pax.test.performance;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class FibonacciRecursive implements Fibonacci {

	public static int calls = 0;

	Fibonacci moins1;
	Fibonacci moins2;

	BundleContext context;
	
	public FibonacciRecursive(BundleContext context){
		this.context=context;
	}
	
	public int compute(int n) {

		if(moins1==null){
			ServiceReference reference=this.context.getServiceReference(Fibonacci.class.getName());
			moins1=(Fibonacci)context.getService(reference);
		}
		
		if(moins2==null){
			ServiceReference reference=this.context.getServiceReference(Fibonacci.class.getName());
			moins2=(Fibonacci)context.getService(reference);
		}
		
		calls++;

		if (n < 2)
			return 1;

		int returns = moins1.compute(n - 1) + moins2.compute(n - 2);

		return returns;

	}

}
