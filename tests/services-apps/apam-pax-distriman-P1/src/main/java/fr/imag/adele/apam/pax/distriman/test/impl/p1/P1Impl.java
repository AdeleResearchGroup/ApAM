package fr.imag.adele.apam.pax.distriman.test.impl.p1;

import fr.imag.adele.apam.pax.distriman.test.iface.P1Spec;
import fr.imag.adele.apam.pax.distriman.test.iface.P2Spec;

public class P1Impl extends Thread implements P1Spec {

	P2Spec p2;

	boolean running = false;

	@Override
	public void run() {
		// TODO Auto-generated method stub
		//while (running) {
			System.out.println("Starting P1");

//			if (p2 != null) {
//				System.out.println("---> the P2 value injected was:" + p2.getName());
//			} else {
//				System.out.println("---> the P2 value injected was NULL");
//			}
//
//			if (p2.getListNames() != null) {
//
//				String names="";
//				
//				for (String name : p2.getListNames()) {
//					names+=name + ",";
//				}
//
//				System.out.print("---->listNames:"+names);
//
//			} else {
//				System.out.print("---->listNames:EMPTY");
//			}
//			
//			if (p2.getKeeper() != null) {
//				
//				System.out.print("---->keeper:"+p2.getKeeper().getValue());
//
//			} else {
//				System.out.print("---->keeper:EMPTY");
//			}
//
//			if (p2.getKeeper() != null) {
//				
//				System.out.print("---->keeper:"+p2.getKeeper().getValue());
//
//			} else {
//				System.out.print("---->keeper:EMPTY");
//			}

			if(p2!=null)
				System.out.println("---->P2Spec:"+p2.getName());
			else
				System.out.println("---->P2Spec: not injected");
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//}

	}

	public void instantiate() {
		///running = true;
		//this.start();
		run();
	}

	public void desinstantiate() {
		//running = false;
		//System.out.println("Stopping P1");
	}

}
