package fr.imag.adele.apam.pax.test.iface;

import java.util.List;

public interface P2Spec {

	String getName();
	
	List<String> getListNames();
	
	P2SpecKeeper getKeeper();
	
}
