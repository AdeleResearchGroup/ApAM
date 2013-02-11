package fr.imag.adele.apam.fibonacci;

import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.Instance;

public class FibMain implements Runnable, ApamComponent{

	public static int nbInst = 0 ;
	Fib fib ;

	int nb = 12 ;

	public void run() {

		System.out.println("Starting new fibonacci " + nb );
		long deb = System.currentTimeMillis() ;
		int fibResult = fib.computeSmart (nb) ;
		long fin = System.currentTimeMillis() ;
		long duree = fin - deb ;
		System.out.println("initialization de " + nbInst + " instances. Duree: " + duree + " milis");
//		System.out.println("initialization: " + duree + " milis");

		for (int i = 0 ; i < 20; i++) {
			deb = System.nanoTime() ;
			fibResult = fib.computeSmart (nb) ;
			fin = System.nanoTime() ;
			duree = fin - deb ;
			System.out.println("execution de " + nbInst + " appels. Duree: " + duree/1000 + " micros");
		}

	//	System.out.println("Resultat pour Fibonacci " + nb + " : " + fibResult);
	}


	public void apamInit(Instance apamInstance /*, String[] params */) {
		System.out.println("Starting new fibonacci " + nb );
		new Thread(this, "APAM perf test").start();
	}

	public void apamRemove() {
	}

	@Override
	public void wiredFor(String resource) {
	}

	@Override
	public void unWiredFor(String resource) {
	}


}
