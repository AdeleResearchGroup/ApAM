package fr.imag.adele.apam.pax.test.performance;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.pax.test.performance.util.Checkpoint;
import fr.imag.adele.apam.pax.test.performance.util.Measure;
import fr.imag.adele.apam.pax.test.performance.util.MeasureHolder;

//@Scope("COMPOSITE")
//@EagerInit
//@AllowsPassByReference
public class Main {
	BundleContext context;
	static AtomicBoolean busy = new AtomicBoolean(false);
 
	public Main() {}
	
	public Main(BundleContext context) { this.context = context; }

//	@Reference
	Fibonacci fibonacci;

//	@Init
	public void start() {

		if (busy.compareAndSet(false, true)) {

			Locale.setDefault(new Locale("en", "US"));

			System.err.println("#FIB\tVALUE\tMEMORY(bytes)\tTIME(ms)\tcalls");

			// if(fibonacci==null){
			// reference =
			// this.context.getServiceReference(Fibonacci.class.getName());
			// fibonacci = (Fibonacci) context.getService(reference); }

			// if(fibonacci==null){
			// SCADomain dom=SCADomain.newInstance("fibonacci.composite");
			// fibonacci = dom.getService(FibonacciRecursive.class,
			// "FibonacciComponent");
			// }
			
			final int SIZE=20;
			
			//fibonacci.compute(SIZE);
			
			fibonacci.callsInit(0);
			for (int i = 0; i <= SIZE; i++) {
				
				Checkpoint p1 = new Checkpoint();

				int value = fibonacci.compute(i);

				Checkpoint p2 = new Checkpoint();

				MeasureHolder holder = Measure.time(p1, p2);

				System.err.println(String.format("%d\t%d\t%s\t%s\t%d", i,
						value, holder.usedMemory(), holder.asMili(),
						fibonacci.getCalls()));

				System.out.println(String.format(
						"thread %s took %s used memory %s", this.toString(),
						holder.asMili(), holder.usedMemory()));

				System.out.println(String.format(
						"Instance %s called, and the value was: %d",
						fibonacci.toString(), value));

			}

			busy.set(false);

		} else { // this is done two avoid the aries issue of calling initmethod
					// twice
			try {
				Thread.currentThread().wait();
			} catch (InterruptedException e) {
			}
		}

	}

	public static void main(String[] args) {
		Main m = new Main();
		FibonacciRecursive recursive = new FibonacciRecursive();
		recursive.moins1 = recursive;
		recursive.moins2 = recursive;

		m.fibonacci = recursive;
		m.start();

	}

	public void stop() {
		//DO SOMETHING!
	}

	//Scaffolds are here for a matter of compatilbilty (i.e tuscany need them)
	
	public Fibonacci getFibonacci() {
		return fibonacci;
	}

	public void setFibonacci(Fibonacci fibonacci) {
		this.fibonacci = fibonacci;
	}

}
