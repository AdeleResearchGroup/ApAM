package fr.imag.adele.apam;

import java.util.Set;

public class Resolved {

	public Set<Implementation> implementations ;
	public Set<Instance> instances ;
	
	public Resolved (Set<Implementation> implementations, Set<Instance> instances ) {
		this.implementations = implementations ;
		this.instances = instances ;
	}

}
