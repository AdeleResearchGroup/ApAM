package fr.imag.adele.apam.apform.impl.handlers;

import org.apache.felix.ipojo.PrimitiveHandler;

import fr.imag.adele.apam.apform.impl.ApformComponentImpl;
import fr.imag.adele.apam.apform.impl.ApformInstanceImpl;

/**
 * The base class for all iPojo handlers manipulating APAM components
 * 
 * @author vega
 * 
 */
public abstract class ApformHandler extends PrimitiveHandler {

	
	@Override
	public ApformComponentImpl getFactory() {
		return (ApformComponentImpl)super.getFactory();
	}
	
	@Override
	public ApformInstanceImpl getInstanceManager() {
		return (ApformInstanceImpl) super.getInstanceManager();
	}

}
