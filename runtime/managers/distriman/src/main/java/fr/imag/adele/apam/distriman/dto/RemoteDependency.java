package fr.imag.adele.apam.distriman.dto;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.impl.RelationImpl;



public class RemoteDependency extends RelationImpl {

	public RemoteDependency(RelationDeclaration dd,Component comp){
		super(dd);
	}

	@Override
	public boolean isMultiple() {
		return false;
	}

}