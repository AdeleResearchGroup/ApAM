package fr.imag.adele.apam.apformipojo.handlers;

import org.apache.felix.ipojo.PrimitiveHandler;

import fr.imag.adele.apam.apformipojo.ApformIpojoComponent;
import fr.imag.adele.apam.apformipojo.ApformIpojoInstance;

/**
 * The base class for all iPojo handlers manipulating APAM components
 * 
 * @author vega
 * 
 */
public abstract class ApformHandler extends PrimitiveHandler {

	
	@Override
	public ApformIpojoComponent getFactory() {
		return (ApformIpojoComponent)super.getFactory();
	}
	
	@Override
	public ApformIpojoInstance getInstanceManager() {
		return (ApformIpojoInstance) super.getInstanceManager();
	}

}
