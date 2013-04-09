package fr.imag.adele.apam.distriman.dto;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Dependency;
import fr.imag.adele.apam.declarations.DependencyDeclaration;

public class RemoteDependency extends Dependency {

	public RemoteDependency(DependencyDeclaration dd,Component comp){
		super(dd,comp);
	}
	
}
