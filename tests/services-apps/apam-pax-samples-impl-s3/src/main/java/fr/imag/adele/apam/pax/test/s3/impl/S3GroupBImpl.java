package fr.imag.adele.apam.pax.test.s3.impl;

import fr.imag.adele.apam.pax.test.iface.S3;
import fr.imag.adele.apam.pax.test.iface.device.Eletronic;


public class S3GroupBImpl implements S3
{

	Eletronic element;
	
	S3 d;
	
	S3 e;
	
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

	public S3 getD() {
		return d;
	}

	public void setD(S3 d) {
		this.d = d;
	}

	public S3 getE() {
		return e;
	}

	public void setE(S3 e) {
		this.e = e;
	}
    
}
