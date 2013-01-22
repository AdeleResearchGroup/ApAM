package fr.imag.adele.apam.pax.test.impl.p1;

import fr.imag.adele.apam.pax.test.iface.P1Spec;
import fr.imag.adele.apam.pax.test.iface.P2Spec;

public class P1Impl extends Thread implements P1Spec{

	P2Spec p2;

	boolean running=false;
	
	public P2Spec getP2() {
		return p2;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(running){
			System.out.println("Starting P1");
			
			if(p2!=null){
				System.out.println("the P2 value injected was:"+p2.getName());
			}else {
				System.out.println("the P2 value injected was NULL");
			}
			
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
				
	}

	public void instantiate(){
		running=true;
		this.start();
	}
	
	public void desinstantiate(){
		running=false;
		System.out.println("Stopping P1");
	}
	
}
