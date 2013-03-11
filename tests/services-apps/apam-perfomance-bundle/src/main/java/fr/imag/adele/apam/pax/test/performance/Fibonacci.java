package fr.imag.adele.apam.pax.test.performance;



//@Remotable
public interface Fibonacci {

	public int compute (int n) ;
	
	public void callsInit(int val);
	
	public int getCalls();
	
}
