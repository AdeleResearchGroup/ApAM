package fr.imag.adele.apam.apform;

import fr.imag.adele.apam.core.ComponentDeclaration;

public interface ApformComponent {

	public ComponentDeclaration getDeclaration () ;

	public void setProperty(String attr, String value);
}
