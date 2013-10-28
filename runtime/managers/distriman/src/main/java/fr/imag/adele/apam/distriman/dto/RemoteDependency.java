package fr.imag.adele.apam.distriman.dto;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.impl.RelToResolveImpl;



public class RemoteDependency extends RelToResolveImpl {

	public RemoteDependency(RelationDeclaration dd,Component comp){
		super(comp, dd);
	}

}