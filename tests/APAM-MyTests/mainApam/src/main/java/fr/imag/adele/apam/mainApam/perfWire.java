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

		//		//overhead
		//		deb = System.nanoTime();
		//		for (int i = 0; i < nb; i++) {
		//			Link l = thisInstance.getLink("testSimple") ;
		//			//			l.reevaluate(true, true) ;
		//		}
		//		fin = System.nanoTime();
		//		overHead = (fin - deb) ;
		//		System.out.println(" : duree de " + nb + " appels. Overhead : " + overHead + " milli secondes");
		//
		//		//heating the system
		//		for (int i = 0; i < nb; i++) {
		//			Link l = thisInstance.getLink("testSimple") ;
		//			l.reevaluate(true, true) ;
		//		}
		//
		//		//Preference
		//		deb = System.nanoTime();
		//		for (int i = 0; i < nb; i++) {
		//			Link l = thisInstance.getLink("testPerfPrefere") ;
		//			l.reevaluate(true, true) ;
		//		}
		//		fin = System.nanoTime();
		//		duree = (fin - deb - overHead) ;
		//		System.out.println(" : duree de " + nb + " appels avec changement de dep, contrainte et Preference : " + duree/1000000 + " milli secondes");		

		overHead = 0 ;
		for (int j = 0; j < 10; j++) {
			//simple
			deb = System.nanoTime();
			for (int i = 0; i < nb; i++) {
				Link l = thisInstance.getLink("testSimple") ;
				l.reevaluate(true, true) ;
			}
			fin = System.nanoTime();
			duree = (fin - deb - overHead) ;
			System.out.println("2 instances : duree de " + nb + " appels sans contrainte : " + duree/1000000 + " milli secondes");		

			//Contrainte
			deb = System.nanoTime();
			for (int i = 0; i < nb; i++) {
				Link l = thisInstance.getLink("testPerf") ;
				l.reevaluate(true, true) ;
			}
			fin = System.nanoTime();
			duree = (fin - deb - overHead) ;
			System.out.println("2 instances : duree de " + nb + " appels avec contrainte : " + duree/1000000 + " milli secondes");		

			//preference
			deb = System.nanoTime();
			for (int i = 0; i < nb; i++) {
				Link l = thisInstance.getLink("testPerfPrefere") ;
				l.reevaluate(true, true) ;
			}
			fin = System.nanoTime();
			duree = (fin - deb - overHead) ;
			System.out.println("2 instances : duree de " + nb + " appels avec preference : " + duree/1000000 + " milli secondes");		

		}

		//		=========== start testReaction test Simple
		//				creating instance
		//				connected to S1ImplEmpty-0
		//				2 instances : duree de 1000 appels sans contrainte : 268 milli secondes
		//				2 instances : duree de 1000 appels avec contrainte : 145 milli secondes
		//		2 instances : duree de 1000 appels sans contrainte : 19 milli secondes
		//		2 instances : duree de 1000 appels avec contrainte : 10 milli secondes
		//		2 instances : duree de 1000 appels avec preference : 20 milli secondes
		//		2 instances : duree de 1000 appels sans contrainte : 10 milli secondes
		//		2 instances : duree de 1000 appels avec contrainte : 20 milli secondes
		//		2 instances : duree de 1000 appels avec preference : 20 milli secondes
		//		2 instances : duree de 1000 appels sans contrainte : 10 milli secondes
		//		2 instances : duree de 1000 appels avec contrainte : 20 milli secondes
		//		2 instances : duree de 1000 appels avec preference : 10 milli secondes
		//		2 instances : duree de 1000 appels sans contrainte : 20 milli secondes
		//		2 instances : duree de 1000 appels avec contrainte : 21 milli secondes
		//		2 instances : duree de 1000 appels avec preference : 20 milli secondes

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

		//			=========== start testReaction test
		//					creating 2 instances
		//					connected to S1ImplEmpty-0
		//					connected to S1ImplEmpty-1
	}

	public void testPerfLink () {
		System.out.println("=========== start testPerfLink test");
		Implementation impl= CST.apamResolver.findImplByName(null,"S2Impl");

		long overHead = 0 ;
		long fin ;
		long duree ;
		long deb ;
		int nb = 1000;
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
			testPerf.getName() ;
			test.setProperty("debit", 10) ;
		}
		fin = System.nanoTime();
		overHead = (fin - deb) ;
		System.out.println(nbInst + " : duree de " + nb + " appels sans changement : " + overHead + " milli secondes");

		for (int j = 0; j < 10; j++) {
			deb = System.nanoTime();
			for (int i = 0; i < nb; i++) {
				test = CST.componentBroker.getInstService(testPerf) ;
				test.setProperty("debit", 2) ;
				testPerf.getName() ;
				//			System.out.println(testPerf.getName());
				test.setProperty("debit", 10) ;
			}
			fin = System.nanoTime();
			duree = (fin - deb);
			System.out.println("Nombre d'instances " + nbInst +  " : duree de " + nb + " appels avec changement de dependance : " + duree/1000000 + " milli secondes");
		}

		//		for (int i = 0; i < 100; i++) {
		//			test = implS1.createInstance(null, null);
		//			nbInst++ ;
		//		}
		//		test.setProperty("debit",  2000) ;

		String s ;
		for (int k = 0; k < 10; k++) {
			System.out.println("creating 100 instances");
			for (int i = 0; i < 100; i++) {
				test = implS1.createInstance(null, null);
				nbInst++ ;
			}
			test.setProperty("debit",  2000) ;
			for (int j = 0; j < 10; j++) {

				deb = System.nanoTime();
				//			System.out.println(testSimple.getName());
				for (int i = 0; i < nb; i++) {
					test = CST.componentBroker.getInstService(testSimple) ;
					test.setProperty("debit", 10) ;
					testSimple.getName() ;
					//				System.out.println(testSimple.getName());
				}
				fin = System.nanoTime();
				overHead = (fin - deb) ;
				System.out.println("Nombre d'instances " + nbInst + " : duree de " + nb + " appels sans changement : " + overHead/1000000 + " milli secondes");

				deb = System.nanoTime();
				//			System.out.println(testPerf.getName());
				for (int i = 0; i < nb; i++) {
					test = CST.componentBroker.getInstService(testPerf) ;
					test.setProperty("debit", 2) ;
					s= testPerf.getName() ;
					test.setProperty("debit", 10) ;
					//System.out.println(s);
				}
				fin = System.nanoTime();
				duree = (fin - deb) ;
				System.out.println("Nombre d'instances " + nbInst +  " : duree de " + nb + " appels avec contrainte et changement de dep : " + duree/1000000 + " milli secondes");

				deb = System.nanoTime();
				//			System.out.println(testPerfPrefere.getName());
				for (int i = 0; i < nb; i++) {
					test = CST.componentBroker.getInstService(testPerfPrefere) ;
					//				System.out.println("debit = " +test.getProperty("debit"));
					test.setProperty("debit", 2) ;
					Link l = thisInstance.getLink("testPerfPrefere") ;
					l.reevaluate(true, true) ;

					testPerfPrefere.getName() ;
					//				System.out.println(testPerfPrefere.getName());
				}
				fin = System.nanoTime();
				duree = (fin - deb) ;
				System.out.println("Nombre d'instances " + nbInst +  " : duree de " + nb + " changement de dep et preference : " + duree/1000000 + " milli secondes");

			}
		}

		//=========== start testPerfLink test
		//		Nombre d'instances 2 : duree de 1000 appels avec changement de dependance : 221 milli secondes
		//		creating 100 instances
		//		Nombre d'instances 102 : duree de 1000 appels sans changement : 10 milli secondes
		//		Nombre d'instances 102 : duree de 1000 appels avec contrainte et changement de dep : 20 milli secondes
		//		Nombre d'instances 102 : duree de 1000 changement de dep et preference : 121 milli secondes
		//		creating 100 instances
		//		Nombre d'instances 202 : duree de 1000 appels sans changement : 7 milli secondes
		//		Nombre d'instances 202 : duree de 1000 appels avec contrainte et changement de dep : 56 milli secondes
		//		Nombre d'instances 202 : duree de 1000 changement de dep et preference : 207 milli secondes
		//		creating 100 instances
		//		Nombre d'instances 302 : duree de 1000 appels sans changement : 6 milli secondes
		//		Nombre d'instances 302 : duree de 1000 appels avec contrainte et changement de dep : 64 milli secondes
		//		Nombre d'instances 302 : duree de 1000 changement de dep et preference : 265 milli secondes
		//		creating 100 instances
		//		Nombre d'instances 402 : duree de 1000 appels sans changement : 9 milli secondes
		//		Nombre d'instances 402 : duree de 1000 appels avec contrainte et changement de dep : 30 milli secondes
		//		Nombre d'instances 402 : duree de 1000 changement de dep et preference : 351 milli secondes
		//		creating 100 instances
		//		Nombre d'instances 502 : duree de 1000 appels sans changement : 8 milli secondes
		//		Nombre d'instances 502 : duree de 1000 appels avec contrainte et changement de dep : 34 milli secondes
		//		Nombre d'instances 502 : duree de 1000 changement de dep et preference : 474 milli secondes
		//		creating 100 instances
		//		Nombre d'instances 602 : duree de 1000 appels sans changement : 8 milli secondes
		//		Nombre d'instances 602 : duree de 1000 appels avec contrainte et changement de dep : 18 milli secondes
		//		Nombre d'instances 602 : duree de 1000 changement de dep et preference : 519 milli secondes
		//		creating 100 instances
		//		Nombre d'instances 702 : duree de 1000 appels sans changement : 10 milli secondes
		//		Nombre d'instances 702 : duree de 1000 appels avec contrainte et changement de dep : 26 milli secondes
		//		Nombre d'instances 702 : duree de 1000 changement de dep et preference : 658 milli secondes
		//		creating 100 instances
		//		Nombre d'instances 802 : duree de 1000 appels sans changement : 10 milli secondes
		//		Nombre d'instances 802 : duree de 1000 appels avec contrainte et changement de dep : 15 milli secondes
		//		Nombre d'instances 802 : duree de 1000 changement de dep et preference : 978 milli secondes
		//		creating 100 instances
		//		Nombre d'instances 902 : duree de 1000 appels sans changement : 11 milli secondes
		//		Nombre d'instances 902 : duree de 1000 appels avec contrainte et changement de dep : 26 milli secondes
		//		Nombre d'instances 902 : duree de 1000 changement de dep et preference : 976 milli secondes
		//		creating 100 instances
		//		Nombre d'instances 1002 : duree de 1000 appels sans changement : 12 milli secondes
		//		Nombre d'instances 1002 : duree de 1000 appels avec contrainte et changement de dep : 16 milli secondes
		//		Nombre d'instances 1002 : duree de 1000 changement de dep et preference : 1189 milli secondes


	}


	@Override
	public void run() {
		System.out.println("Starting test perf Link");
		testReactionSimple () ;
		testReaction () ;
		testPerfLink  () ;
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

