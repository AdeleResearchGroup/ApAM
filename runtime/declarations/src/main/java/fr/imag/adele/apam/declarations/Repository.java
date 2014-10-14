package fr.imag.adele.apam.declarations;


/**
 * This class represents a repository of component declarations.
 * 
 * There can be many different kinds of repository that can be used at build or runtime. The main usage of 
 * a repository is to verify consistency of component declarations, so by now the only supported operations
 * are lookup by reference and whole iteration. 
 * 
 * TODO Add support for some form of querying using filters and selection of candidate versions
 * 
 * @author vega
 *
 */
public interface Repository extends Iterable<ComponentReference<?>> {

	
	/**
	 * Get the component declaration associated to the specified reference. If there are several versions of 
	 * the component, selects an arbitrary one.
	 */
	public <C extends ComponentDeclaration> C getComponent(ComponentReference<C> reference);
	
	/**
	 * Get the component declaration associated to the specified reference. If there are several versions of 
	 * the component, selects an arbitrary one among the specified range.
	 */
	public <C extends ComponentDeclaration> C getComponent(ComponentReference<C>.Versioned reference);
	
}
