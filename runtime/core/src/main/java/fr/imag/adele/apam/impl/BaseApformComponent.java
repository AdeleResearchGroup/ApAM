package fr.imag.adele.apam.impl;

import org.osgi.framework.Bundle;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;

/**
 * This class is the basic implementation of an apform component. It implements
 * the basic contract for all underlying platforms.
 * 
 * It can be used by inheritence or delegation.
 * 
 * The type parameters are used to type-safely reference the specific class of
 * APAM component and declaration associated to this apform.
 * 
 * @author vega
 * 
 */
public abstract class BaseApformComponent<C extends Component, D extends ComponentDeclaration> implements ApformComponent {

	/**
	 * The Associated declaration
	 */
	protected final D declaration;

	/**
	 * The associated APAM component
	 */
	protected C apamComponent;

	protected BaseApformComponent(D declaration) {
		this.declaration = declaration;
	}

	@Override
	public C getApamComponent() {
		return apamComponent;
	}

	@Override
	public Bundle getBundle() {
		return null;
	}

	@Override
	public D getDeclaration() {
		return declaration;
	}

	@Override
	public boolean remLink(Component destInst, String depName) {

		/*
		 * Propagate down the group hierarchy.
		 * 
		 * TODO Should we stop in case of a veto from one of the members?
		 */
		for (Component member : apamComponent.getMembers()) {
			member.getApformComponent().remLink(destInst, depName);
		}

		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setApamComponent(Component apamComponent) throws InvalidConfiguration {
		this.apamComponent = (C) apamComponent;
	}

	@Override
	public boolean setLink(Component destInst, String depName) {

		/*
		 * Propagate down the group hierarchy.
		 * 
		 * TODO Should we stop in case of a veto from one of the members?
		 */
		for (Component member : apamComponent.getMembers()) {
			member.getApformComponent().setLink(destInst, depName);
		}

		return true;
	}

	@Override
	public boolean checkLink(Component destInst, String depName) {
		/*
		 * Does nothing
		 */
		return true;
	}

	@Override
	public void setProperty(String attr, String value) {
	}

}
