package fr.imag.adele.apam.core;

import java.util.Set;

public class GrantDeclaration {

	public final DependencyDeclaration.Reference dependency;
	
	public final Set<String> states;
	
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
}
