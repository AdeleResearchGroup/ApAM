package fr.imag.adele.apam.pax.test.performance;

import org.oasisopen.sca.annotation.Remotable;


@Remotable
public interface Fibonacci {

	public int compute (int n) ;
	
	public void callsInit(int val);
	
	public int getCalls();
	
}
