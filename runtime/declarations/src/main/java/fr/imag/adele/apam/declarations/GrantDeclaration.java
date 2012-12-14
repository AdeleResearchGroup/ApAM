package fr.imag.adele.apam.declarations;

import java.util.Set;

public class GrantDeclaration {

	private final DependencyDeclaration.Reference dependency;
	
	private final Set<String> states;
	
	public GrantDeclaration(DependencyDeclaration.Reference dependency, Set<String> states) {
		this.dependency = dependency;
		this.states = states;
	}
	
	public DependencyDeclaration.Reference getDependency() {
		return dependency;
	}
	
	public Set<String> getStates() {
		return states;
	}
	
	@Override
	public String toString() {
		return "<grant when " + states + " " + dependency.getDeclaringComponent() + " dependency=\"" 
				+ dependency.getIdentifier() + "\"/>" ;
	}
}
