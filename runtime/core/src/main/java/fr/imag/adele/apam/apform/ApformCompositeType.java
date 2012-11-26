package fr.imag.adele.apam.apform;

import java.util.Set;

import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.declarations.CompositeDeclaration;

public interface ApformCompositeType extends ApformImplementation {

	/**
	 * Get the development model associated with the the implementation
	 */
	public CompositeDeclaration getDeclaration();
	
    /**
     * returns the models associated with this component
     * 
     */
    public Set<ManagerModel> getModels();

}
