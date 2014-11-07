package fr.imag.adele.apam.apform.impl;

import org.apache.felix.ipojo.ConfigurationException;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.instrumentation.CallbackDeclaration;

/**
 * This is the callback associated to the APAM component lifecycle
 * 
 * @author vega
 */
public class LifecycleCallback extends InstanceCallback<Instance> {

    private final AtomicImplementationDeclaration.Event trigger;

	public LifecycleCallback(ApamInstanceManager instance, AtomicImplementationDeclaration.Event trigger, CallbackDeclaration declaration) throws ConfigurationException {
		super(instance,declaration.getMethodName());
		
		this.trigger		= trigger;
		
		/*
		 * We force reflection meta-data calculation in the constructor to signal errors as soon as
		 * possible. This however has a cost in terms of early class loading.  
		 */
		try {
			searchMethod();
		} catch (NoSuchMethodException e) {
			throw new ConfigurationException("invalid method declaration in callback "+getMethod());
		}
	}
	
	
	public boolean isTriggeredBy(AtomicImplementationDeclaration.Event trigger) {
		return this.trigger.equals(trigger);
	}
	
	
	@Override
	protected boolean isExpectedParameter(Class<?> parameterType) {
		return parameterType.isAssignableFrom(Instance.class);
	}
}
