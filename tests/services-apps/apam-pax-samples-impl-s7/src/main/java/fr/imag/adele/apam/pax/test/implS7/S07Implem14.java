package fr.imag.adele.apam.pax.test.implS7;

import fr.imag.adele.apam.pax.test.specS7ext.S07Dependency03;



public class S07Implem14 implements S07Interface14 {


	@Override
	public String whoami() {
		return "S07Interface14";
	}

	public S07Dependency02 injected02;

	public S07Dependency02 getInjected02() throws Exception {
		return injected02;
	}
	
	private S07Dependency03 injected03;
	public S07Dependency03 getInjected03() throws Exception {
		return injected03;
	}	

}
