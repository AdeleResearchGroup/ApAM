package fr.imag.adele.apam.pax.test.performance;

import java.util.Locale;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import fr.imag.adele.apam.pax.test.performance.util.Checkpoint;
import fr.imag.adele.apam.pax.test.performance.util.Measure;
import fr.imag.adele.apam.pax.test.performance.util.MeasureHolder;

public class Main {
	BundleContext context;
	
	public Main(BundleContext context){
		this.context=context;
	}
	
	boolean active;

	Fibonacci fib;

	public void start() {
		
		Locale.setDefault(new Locale("en","US"));
		
		System.err.println("#FIB\tVALUE\tMEMORY(bytes)\tTIME(ms)\tcalls");
		
		if(fib==null){
			ServiceReference reference=this.context.getServiceReference(Fibonacci.class.getName());
			fib=(Fibonacci)context.getService(reference);
		}
		
		fib.compute(30);
		
		((FibonacciRecursive)fib).calls=0;
		
		for (int i = 0; i <= 30; i++) {
			
			//System.gc();
			
			Checkpoint p1 = new Checkpoint();

			int value = fib.compute(i);

			Checkpoint p2 = new Checkpoint();

			MeasureHolder holder = Measure.time(p1, p2);
			
			System.err.println(String.format("%d\t%d\t%s\t%s\t%d", i,value,
					holder.usedMemory(),holder.asMili(),((FibonacciRecursive)fib).calls));

			System.out.println(String.format(
					"thread %s took %s used memory %s", this.toString(),
					holder.asMili(), holder.usedMemory()));

			System.out.println(String.format(
					"Instance %s called, and the value was: %d",
					fib.toString(), value));

			//System.gc();

		}

	}
	
	public static void main(String[] args) {
		
		Main m=new Main(null);
		
		FibonacciRecursive recursive = new FibonacciRecursive(null);
		recursive.moins1 = recursive;
		recursive.moins2 = recursive;

		m.fib= recursive;
		m.start();
		
	}

	public void stop() {
		active = false;

	}

}
