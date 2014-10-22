package fr.imag.adele.apam.declarations.repository;

import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.Versioned;


/**
 * This class represents a repository of component declarations.
 * 
 * There can be many different kinds of repository that can be used at build or runtime. The main usage of 
 * a repository is to verify consistency of component declarations, so by now the only supported operation
 * is lookup by reference. 
 * 
 * TODO Add support for some form of querying using filters and selection of candidate versions, eventually
 * this should converge with property-based querying as used by dependency resolvers
 * 
 * @author vega
 *
 */
public interface Repository {

	
	/**
	 * Get the component declaration associated to the specified reference. If there are several versions of 
	 * the component, selects an arbitrary one.
	 */
	public <C extends ComponentDeclaration> C getComponent(ComponentReference<C> reference);
	
	/**
	 * Get the component declaration associated to the specified reference. If there are several versions of 
	 * the component, selects an arbitrary one among the specified range.
	 */
	public <C extends ComponentDeclaration> C getComponent(Versioned<C> reference);
	
}
