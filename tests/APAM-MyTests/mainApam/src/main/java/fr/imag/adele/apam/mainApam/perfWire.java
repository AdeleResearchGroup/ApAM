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

		//overhead
		deb = System.currentTimeMillis();
		for (int i = 0; i < nb; i++) {
			Link l = thisInstance.getLink("testSimple") ;
//			l.reevaluate(true, true) ;
		}
		fin = System.currentTimeMillis();
		overHead = (fin - deb) ;
		System.out.println(" : duree de " + nb + " appels. Overhead : " + overHead + " milli secondes");
		
		//heating the system
		for (int i = 0; i < nb; i++) {
			Link l = thisInstance.getLink("testSimple") ;
			l.reevaluate(true, true) ;
		}

		//Preference
		deb = System.currentTimeMillis();
		for (int i = 0; i < nb; i++) {
			Link l = thisInstance.getLink("testPerfPrefere") ;
			l.reevaluate(true, true) ;
		}
		fin = System.currentTimeMillis();
		duree = (fin - deb - overHead) ;
		System.out.println(" : duree de " + nb + " appels avec changement de dep, contrainte et Preference : " + duree + " milli secondes");		

		
		//simple
		deb = System.currentTimeMillis();
		for (int i = 0; i < nb; i++) {
			Link l = thisInstance.getLink("testSimple") ;
			l.reevaluate(true, true) ;
		}
		fin = System.currentTimeMillis();
		duree = (fin - deb - overHead) ;
		System.out.println(" : duree de " + nb + " appels avec changement de dep : " + duree + " milli secondes");		

		//Contrainte
		deb = System.currentTimeMillis();
		for (int i = 0; i < nb; i++) {
			Link l = thisInstance.getLink("testPerf") ;
			l.reevaluate(true, true) ;
		}
		fin = System.currentTimeMillis();
		duree = (fin - deb - overHead) ;
		System.out.println(" : duree de " + nb + " appels avec changement de dep et contrainte : " + duree + " milli secondes");		


//		=========== start testReaction test Simple
//				creating instance
//				connected to S1ImplEmpty-0
//				 : duree de 1000 appels. Overhead : 16 milli secondes
//				 : duree de 1000 appels avec changement de dep, contrainte et Preference : 262 milli secondes
//				 : duree de 1000 appels avec changement de dep : 202 milli secondes
//				 : duree de 1000 appels avec changement de dep et contrainte : 202 milli secondes

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
		int nb = 10;
		int nbInst = 0 ;

		System.out.println("creating 2 instances");
		Implementation implS1 = CST.apamResolver.findImplByName(null,"S1ImplEmpty");
		implS1.createInstance(null, null);
		nbInst++ ;
		implS1.createInstance(null, null);
		nbInst++ ;

		Instance test = null ;
		deb = System.currentTimeMillis();
		for (int i = 0; i < nb; i++) {
			test = CST.componentBroker.getInstService(testPerf) ;
			test.setProperty("debit", 10) ;
			testPerf.getName() ;
			test.setProperty("debit", 10) ;
		}
		fin = System.currentTimeMillis();
		overHead = (fin - deb) ;
		System.out.println(nbInst + " : duree de " + nb + " appels sans changement : " + overHead + " milli secondes");

		deb = System.currentTimeMillis();
		for (int i = 0; i < nb; i++) {
			test = CST.componentBroker.getInstService(testPerf) ;
			test.setProperty("debit", 2) ;
			testPerf.getName() ;
			test.setProperty("debit", 10) ;
		}
		fin = System.currentTimeMillis();
		duree = (fin - deb - overHead);
		System.out.println("Nombre d'instances " + nbInst +  " : duree de " + nb + " appels avec changement de dependance : " + duree + " milli secondes");


		for (int j = 0; j < 10; j++) {
			System.out.println("creating 100 instances");
			for (int i = 0; i < 100; i++) {
				implS1.createInstance(null, null);
				nbInst++ ;
			}

			deb = System.currentTimeMillis();
			for (int i = 0; i < nb; i++) {
				test = CST.componentBroker.getInstService(testPerf) ;
				test.setProperty("debit", 10) ;
				testPerf.getName() ;
				test.setProperty("debit", 10) ;
			}
			fin = System.currentTimeMillis();
			overHead = (fin - deb) ;
			System.out.println("Nombre d'instances " + nbInst + " : duree de " + nb + " appels sans changement : " + overHead + " milli secondes");

			deb = System.currentTimeMillis();
			nb = 100 ;
			for (int i = 0; i < nb; i++) {
				test = CST.componentBroker.getInstService(testPerf) ;
				test.setProperty("debit", 2) ;
				testPerf.getName() ;
				test.setProperty("debit", 10) ;
			}
			fin = System.currentTimeMillis();
			duree = (fin - deb - overHead) ;
			System.out.println("Nombre d'instances " + nbInst +  "duree de " + nb + " appels avec changement de dep : " + duree + " milli secondes");

			deb = System.currentTimeMillis();
			nb = 100 ;
			for (int i = 0; i < nb; i++) {
				test = CST.componentBroker.getInstService(testPerf) ;
				test.setProperty("debit", 2) ;
				testPerf.getName() ;
				test.setProperty("debit", 10) ;
			}
			fin = System.currentTimeMillis();
			duree = (fin - deb - overHead) ;
			System.out.println("Nombre d'instances " + nbInst +  "duree de " + nb + " changement de dep et preference : " + duree + " milli secondes");
		
		}


//=========== start testPerfLink test
//creating 2 instances
//2 : duree de 10 appels sans changement : 1 milli secondes
//Nombre d'instances 2 : duree de 10 appels avec changement de dependance : 3 milli secondes
//creating 100 instances
//Nombre d'instances 102 : duree de 10 appels sans changement : 1 milli secondes
//Nombre d'instances 102duree de 100 appels avec changement de dep : 33 milli secondes
//Nombre d'instances 102duree de 100 changement de dep et preference : 45 milli secondes
//creating 100 instances
//Nombre d'instances 202 : duree de 100 appels sans changement : 8 milli secondes
//Nombre d'instances 202duree de 100 appels avec changement de dep : 21 milli secondes
//Nombre d'instances 202duree de 100 changement de dep et preference : 23 milli secondes
//creating 100 instances
//Nombre d'instances 302 : duree de 100 appels sans changement : 13 milli secondes
//Nombre d'instances 302duree de 100 appels avec changement de dep : 21 milli secondes
//Nombre d'instances 302duree de 100 changement de dep et preference : 17 milli secondes
//creating 100 instances
//Nombre d'instances 402 : duree de 100 appels sans changement : 10 milli secondes
//Nombre d'instances 402duree de 100 appels avec changement de dep : 26 milli secondes
//Nombre d'instances 402duree de 100 changement de dep et preference : 50 milli secondes
//creating 100 instances
//Nombre d'instances 502 : duree de 100 appels sans changement : 7 milli secondes
//Nombre d'instances 502duree de 100 appels avec changement de dep : 15 milli secondes
//Nombre d'instances 502duree de 100 changement de dep et preference : 15 milli secondes
//creating 100 instances
//Nombre d'instances 602 : duree de 100 appels sans changement : 7 milli secondes
//Nombre d'instances 602duree de 100 appels avec changement de dep : 17 milli secondes
//Nombre d'instances 602duree de 100 changement de dep et preference : 46 milli secondes
//creating 100 instances
//Nombre d'instances 702 : duree de 100 appels sans changement : 6 milli secondes
//Nombre d'instances 702duree de 100 appels avec changement de dep : 17 milli secondes
//Nombre d'instances 702duree de 100 changement de dep et preference : 13 milli secondes
//creating 100 instances
//Nombre d'instances 802 : duree de 100 appels sans changement : 6 milli secondes
//Nombre d'instances 802duree de 100 appels avec changement de dep : 15 milli secondes
//Nombre d'instances 802duree de 100 changement de dep et preference : 14 milli secondes
//creating 100 instances
//Nombre d'instances 902 : duree de 100 appels sans changement : 6 milli secondes
//Nombre d'instances 902duree de 100 appels avec changement de dep : 17 milli secondes
//Nombre d'instances 902duree de 100 changement de dep et preference : 13 milli secondes
//creating 100 instances
//Nombre d'instances 1002 : duree de 100 appels sans changement : 19 milli secondes
//Nombre d'instances 1002duree de 100 appels avec changement de dep : 25 milli secondes
//Nombre d'instances 1002duree de 100 changement de dep et preference : 60 milli secondes


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

