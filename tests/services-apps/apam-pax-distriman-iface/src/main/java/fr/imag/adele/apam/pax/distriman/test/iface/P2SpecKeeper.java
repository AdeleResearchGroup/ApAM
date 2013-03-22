package fr.imag.adele.apam.pax.distriman.test.iface;

import java.io.Serializable;

public class P2SpecKeeper implements Serializable {
	
	private String value;
	
	public P2SpecKeeper(){
		
	}
	
	public P2SpecKeeper(String value) {
		this.value=value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
}
