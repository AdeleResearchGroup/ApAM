package fr.imag.adele.apam.declarations.repository;

import java.util.ArrayList;
import java.util.List;

import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.VersionedReference;

/**
 * This class implements a repository that aggregates results from other repositories, by using
 * delegation.
 * 
 * Repositories are looked-up in order until a component is found.
 *  
 * @author vega
 *
 */
public class RepositoryChain implements Repository {
	
	private final List<Repository> delegates;
	
	public RepositoryChain(Repository... repositories) {
		
		this.delegates	= new ArrayList<Repository>();
		
		if (repositories != null) {
			for (Repository repository : repositories) {
				addRepository(repository);
			}
		}
	}
	
	public void addRepository(Repository repository) {
		delegates.add(repository);
	}
	
	@Override
	public <C extends ComponentDeclaration> C getComponent(ComponentReference<C> reference) {
		for (Repository delegate : delegates) {
			C component = delegate.getComponent(reference);
			if (component != null)
				return component;
		}
		
		return null;
	}

	@Override
	public <C extends ComponentDeclaration> C getComponent(VersionedReference<C> reference) {
		for (Repository delegate : delegates) {
			C component = delegate.getComponent(reference);
			if (component != null)
				return component;
		}
		
		return null;
	}

}
