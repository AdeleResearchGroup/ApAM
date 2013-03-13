package fr.imag.adele.apam.pax.test.performance;


//@Scope("STATELESS")
public class FibonacciRecursive implements Fibonacci {

	public static int calls = 0;

//	@Reference(required=false)
	Fibonacci moins1;

//	@Reference(required=false)
	Fibonacci moins2;

	public FibonacciRecursive(){
		System.out.println("############## Building ########### ");	
	}
	
	/*BundleContext context;
	
	public FibonacciRecursive(){}
	
	public FibonacciRecursive(BundleContext context){
		this.context=context;
	}*/
	
	synchronized public int compute(int n) {

//		pure osgi
//		if(moins1==null){
//			ServiceReference reference=this.context.getServiceReference(Fibonacci.class.getName());
//			moins1=(Fibonacci)context.getService(reference);
//		}
//		
//		if(moins2==null){
//			ServiceReference reference=this.context.getServiceReference(Fibonacci.class.getName());
//			moins2=(Fibonacci)context.getService(reference);
//		}

		//moins1=Main.dom.getServiceReference(FibonacciRecursive.class, "FibonacciComponent").getService();
		//moins2=Main.dom.getServiceReference(FibonacciRecursive.class, "FibonacciComponent").getService();
		
		//String message=String.format("Instances are %d ----  %s/%s/%s", n,this,moins1,moins2);
		//System.out.println(message);
		
		calls++;

		if (n < 2)
			return 1;
		
		int returns = moins1.compute(n - 1) + moins2.compute(n - 2);

		return returns;

	}

	public Fibonacci getMoins1() {
		return moins1;
	}

	public void setMoins1(Fibonacci moins1) {
		this.moins1 = moins1;
	}

	public Fibonacci getMoins2() {
		return moins2;
	}

	public void setMoins2(Fibonacci moins2) {
		this.moins2 = moins2;
	}

	@Override
	public int getCalls() {
		return calls;
	}

	@Override
	public void callsInit(int val) {
		calls=val;
	}

	
}
