package fr.imag.adele.apam.application.security;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.PropertyManager;

public class SecurityStateManager implements PropertyManager {

	
	
	/**
	 * This is the APAM internal property used to notify state changes
	 */
	@SuppressWarnings("unused")
	private String stateProperty;

	public SecurityStateManager() {
		stateProperty = "normal";
	}
	
	@SuppressWarnings("unused")
	private void start(Instance instance) {
		ApamManagers.addPropertyManager(this);
	}

	@Override
	public void attributeAdded(Component component, String attr, String newValue) {
		attributeChanged(component, attr, newValue, null);
	}

	@Override
	public void attributeChanged(Component component, String attr, String newValue, String oldValue) {
		if (component.getName().equals("fireManagementDomain") && attr.equals("emergency")) {
			if (newValue.equals("emergency"))
				stateProperty = "fire";
			if (newValue.equals("normal"))
				stateProperty = "normal";
		} 
	}

	@Override
	public String getName() {
		return "SecurityStateManager";
	}

	@Override
	public int getPriority() {
		return 0;
	}



	@Override
	public void newComposite(ManagerModel model, CompositeType composite) {
	}

	@Override
	public void attributeRemoved(Component component, String attr,	String oldValue) {
	}

	
	
	
}
