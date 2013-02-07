package fr.imag.adele.apam.fibonacci;

import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;

public class FibMain implements Runnable, ApamComponent{

	Fibonacci fib = null ;
	
	int nb = 15 ;
	
	public void run() {

		System.out.println("Starting new fibonacci " + nb );
		int fibResult = fib.compute (nb) ;
		System.out.println("Resultat pour Fibonacci " + nb + " : " + fibResult);
	}



	public void apamInit(Instance apamInstance) {
		new Thread(this, "APAM perf test").start();
	}

	public void apamRemove() {

	}

	@Override
	public void wiredFor(String resource) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unWiredFor(String resource) {
		// TODO Auto-generated method stub
		
	}


}
