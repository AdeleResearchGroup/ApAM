package fr.imag.adele.apam.apform;

import java.util.Map;

import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.impl.ComponentImpl;

public interface ApformImplementation extends ApformComponent {
	
	/**
	 * Get the development model associated with the the implementation
	 */
	public ImplementationDeclaration getDeclaration();
	
    /**
     * Creates an instance of that implementation, and initialize its properties with the set of provided properties.
     * <p>
     * 
     * @param initialproperties the initial properties
     * @return the platform instance
     */
    public ApformInstance createInstance(Map<String, String> initialproperties) throws ComponentImpl.InvalidConfiguration;

    /**
     * If a specification exists in the platform, returns the associated spec.
     * 
     * @return
     */
    public ApformSpecification getSpecification(); // If existing. In general returns null !!

}
