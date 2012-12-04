package fr.imag.adele.apam.pax.test.s3.impl;

import fr.imag.adele.apam.pax.test.iface.S3;
import fr.imag.adele.apam.pax.test.iface.device.Eletronic;


public class S3GroupAImpl implements S3
{

	Eletronic element;
	
	S3 c;
	
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

	public S3 getC() {
		return c;
	}

	public void setC(S3 c) {
		this.c = c;
	}

}
