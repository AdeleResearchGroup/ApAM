package fr.imag.adele.apam.apform;

import org.osgi.framework.Bundle;

import fr.imag.adele.apam.core.ComponentDeclaration;

public interface ApformComponent {

	public ComponentDeclaration getDeclaration () ;

	public void setProperty(String attr, String value);
	
	public abstract Bundle getBundle();
}
