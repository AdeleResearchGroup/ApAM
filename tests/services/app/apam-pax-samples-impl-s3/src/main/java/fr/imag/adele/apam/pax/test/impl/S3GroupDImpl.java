package fr.imag.adele.apam.pax.test.impl;

import fr.imag.adele.apam.pax.test.iface.S3;
import fr.imag.adele.apam.pax.test.iface.device.Eletronic;


public class S3GroupDImpl implements S3
{

	Eletronic element;
	
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
    
}
