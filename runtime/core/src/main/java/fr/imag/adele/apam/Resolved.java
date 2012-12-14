package fr.imag.adele.apam;

import java.util.Set;

public class Resolved {

	public Set<Implementation> implementations ;
	public Set<Instance> instances ;
	
	public Resolved (Set<Implementation> implementations, Set<Instance> instances ) {
		if (implementations == null || implementations.isEmpty())
			this.implementations = null ;
		else this.implementations = implementations ;
		
		if (instances == null || instances.isEmpty())
			this.instances = null ;
		else this.instances = instances ;
	}
}
