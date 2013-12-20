package fr.imag.adele.apam.mainApam;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.test.s1.S1;

public class perfWire implements Runnable, ApamComponent {
	// injected
	Apam apam;
	S1 testPerf ;
	S1 testReaction ;
	S1 testPerfPrefere ;
	S1 testSimple ;
	Instance thisInstance ;

	public void testReactionSimple () {
		System.out.println("=========== start testReaction test Simple");

		System.out.println("creating instance");
		Implementation implS1 = CST.apamResolver.findImplByName(null,"S1ImplEmpty");
		implS1.createInstance(null, null);
		Instance test = CST.componentBroker.getInstService(testSimple) ;
		System.out.println("connected to " + test.getName());

		
		long overHead = 0 ;
		long fin ;
		long duree ;
		long deb ;
		int nb = 1000;

		deb = System.nanoTime();
		for (int i = 0; i < nb; i++) {
			Link l = thisInstance.getLink("testSimple") ;
//			l.reevaluate(true, true) ;
		}
		fin = System.nanoTime();
		overHead = (fin - deb) ;
		System.out.println(" : duree de " + nb + " appels. Overhead : " + (overHead/1000000) + " milli secondess");
		
		deb = System.nanoTime();
		for (int i = 0; i < nb; i++) {
			Link l = thisInstance.getLink("testSimple") ;
			l.reevaluate(true, true) ;
		}
		fin = System.nanoTime();
		duree = (fin - deb - overHead)/1000000 ;
		System.out.println(" : duree de " + nb + " appels avec changement de dep : " + duree + " milli secondess");		
	}
	
	
	public void testReaction () {
			System.out.println("=========== start testReaction test");

			System.out.println("creating 2 instances");
			Implementation implS1 = CST.apamResolver.findImplByName(null,"S1ImplEmpty");
			Instance s1 = implS1.createInstance(null, null);
			Instance s2 = implS1.createInstance(null, null);
			
			Instance test = CST.componentBroker.getInstService(testReaction) ;
			System.out.println("connected to " + test.getName());
			if (test == s1) 
				s2.setProperty("debit", 100) ;
			else 
				s1.setProperty("debit", 100) ;
			
			thisInstance.setProperty("need", 20) ;
			test = CST.componentBroker.getInstService(testReaction) ;
			System.out.println("connected to " + test.getName());
		}

	public void testPerfLink () {
		System.out.println("=========== start testPerfLink test");
		Implementation impl= CST.apamResolver.findImplByName(null,"S2Impl");

		long overHead = 0 ;
		long fin ;
		long duree ;
		long deb ;
		int nb = 100;
		int nbInst = 0 ;

		System.out.println("creating 2 instances");
		Implementation implS1 = CST.apamResolver.findImplByName(null,"S1ImplEmpty");
		implS1.createInstance(null, null);
		nbInst++ ;
		implS1.createInstance(null, null);
		nbInst++ ;

		Instance test = null ;
		deb = System.nanoTime();
		for (int i = 0; i < nb; i++) {
			test = CST.componentBroker.getInstService(testPerf) ;
			test.setProperty("debit", 10) ;
			testPerf.getName() ;
			test.setProperty("debit", 10) ;
		}
		fin = System.nanoTime();
		overHead = (fin - deb) ;
		System.out.println(nbInst + " : duree de " + nb + " appels a getInstService, setProp sans changement : " + (overHead/1000000) + " milli secondess");

		deb = System.nanoTime();
		for (int i = 0; i < nb; i++) {
			test = CST.componentBroker.getInstService(testPerf) ;
			test.setProperty("debit", 2) ;
			testPerf.getName() ;
			test.setProperty("debit", 10) ;
		}
		fin = System.nanoTime();
		duree = (fin - deb - overHead)/1000000 ;
		System.out.println(nbInst +  " : duree de " + nb + " appels avec changement de dep : " + duree + " milli secondess");




		for (int j = 0; j < 10; j++) {
			System.out.println("creating 100 instances");
			for (int i = 0; i < 100; i++) {
				implS1.createInstance(null, null);
				nbInst++ ;
			}

			deb = System.nanoTime();
			for (int i = 0; i < nb; i++) {
				test = CST.componentBroker.getInstService(testPerf) ;
				test.setProperty("debit", 10) ;
				testPerf.getName() ;
				test.setProperty("debit", 10) ;
			}
			fin = System.nanoTime();
			overHead = (fin - deb) ;
			System.out.println(nbInst + " : duree de " + nb + " appels a getInstService, setProp sans changement : " + (overHead/1000000) + " milli secondess");

			deb = System.nanoTime();
			nb = 100 ;
			for (int i = 0; i < nb; i++) {
				test = CST.componentBroker.getInstService(testPerf) ;
				test.setProperty("debit", 2) ;
				testPerf.getName() ;
				test.setProperty("debit", 10) ;
			}
			fin = System.nanoTime();
			duree = (fin - deb - overHead)/1000000 ;
			System.out.println(nbInst +  "duree de " + nb + " appels avec changement de dep : " + duree + " milli secondess");
		}
	}


	@Override
	public void run() {
		System.out.println("Starting test perf Link");
		//testReactionSimple () ;
		testReaction () ;
		//testPerfLink  () ;
	}


	@Override
	public void apamInit(Instance apamInstance) {
		thisInstance = apamInstance ;
		new Thread(this, "MainApam perftest").start();
	}

	@Override
	public void apamRemove() {
	}

}

