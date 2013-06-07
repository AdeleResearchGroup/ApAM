package fr.imag.adele.apam.distriman.dto;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Relation;
import fr.imag.adele.apam.declarations.RelationDeclaration;



public class RemoteDependency extends Relation {

	public RemoteDependency(RelationDeclaration dd,Component comp){
		super(dd,comp);
	}

}