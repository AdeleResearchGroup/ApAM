package fr.imag.adele.apam.pax.test.impl;

import fr.imag.adele.apam.pax.test.iface.S3;
import fr.imag.adele.apam.pax.test.iface.device.Eletronic;


public class S3GroupEImpl implements S3
{

	Eletronic element;
	
	S3 f;
	
    public String whoami()
    {
        return this.getClass().getName();
    }

	public Eletronic getElement() {
		return element;
	}

	public void setElement(Eletronic element) {
		this.element = element;
	}

	public S3 getF() {
		return f;
	}

	public void setF(S3 f) {
		this.f = f;
	}
    
}
