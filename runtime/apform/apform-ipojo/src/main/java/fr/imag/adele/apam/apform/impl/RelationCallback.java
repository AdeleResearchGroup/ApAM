package fr.imag.adele.apam.apform.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.util.Callback;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.CallbackDeclaration;
import fr.imag.adele.apam.declarations.RelationDeclaration;

/**
 * This is the callback associated to the APAM relation lifecycle
 * 
 * @author vega
 */
public class RelationCallback extends Callback {

	private final ApamInstanceManager 		instance;

    private final RelationDeclaration 		relation;
    private final RelationDeclaration.Event trigger;
    private final CallbackDeclaration 		callback;

    private boolean needsArgument;
    

	public RelationCallback(ApamInstanceManager instance, RelationDeclaration relation, RelationDeclaration.Event trigger, CallbackDeclaration callback) throws ConfigurationException {
		super(callback.getMethodName(),(String[])null,false,instance);
		
		this.instance		= instance;
		this.relation		= relation;
		this.trigger		= trigger;
		this.callback		= callback;
		
		/*
		 * We force reflection meta-data calculation in the constructor to signal errors as soon as
		 * possible. This however has a cost in terms of early class loading.  
		 */
		try {
			searchMethod();
		} catch (NoSuchMethodException e) {
			throw new ConfigurationException("invalid method declaration in callback "+callback.getMethodName()+ " for relation "+relation.getIdentifier());
		}
	}
	
	public boolean isTriggeredBy(String relationName, RelationDeclaration.Event trigger) {
		return this.relation.getIdentifier().equals(relationName) && this.trigger.equals(trigger);
	}
	
	public Object invoke(Component target) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (m_methodObj == null) {
            searchMethod();
        }

		return call(needsArgument ? new Object[] {target} : new Object[0]);
	}

	@Override
	protected void searchMethod() throws NoSuchMethodException {
        
		/*
		 * Try to find the declared method with the specified name that best matches the callback signature
		 * 
		 * NOTE Notice that we force class loading of the implementation class (and its super classes) by using
		 * reflection to search for methods.
		 */
		
        Method[] methods = instance.getClazz().getMethods();
        
        Method candidate 			= null;
        Class<?> candidateParameter	= null;
        
        for (Method method : methods) {
			
        	if (! method.getName().equals(callback.getMethodName()))
				continue;

        	/*
        	 * We are looking for a method with an optional, single parameter of kind
        	 * APAM component (depending on the target of the relation)
        	 */
        	Class<?> targetKind = Component.class;
        	switch (relation.getTargetKind()) {
        		case SPECIFICATION:
        			targetKind = Specification.class;
        			break;
        		case IMPLEMENTATION:
        			targetKind = Implementation.class;
        			break;
        		case INSTANCE:
        			targetKind = Instance.class;
        			break;
        	}
        	
        	Class<?>[] parameters = method.getParameterTypes();
        	if (parameters.length > 1)
        		continue;

           	Class<?> parameter = parameters.length == 1 ? parameters[0] : null;
           	if (parameter != null && ! parameter.isAssignableFrom(targetKind))
        		continue;
        	
        	if (candidate == null) {
        		candidate 			= method;
        		candidateParameter	= parameter;
        		continue;
        	}
        	
        	if (candidateParameter == null && parameter != null) {
        		candidate 			= method;
        		candidateParameter	= parameter;
        		continue;
        	}
        	
        	if (candidateParameter != null && parameter != null && candidateParameter.isAssignableFrom(parameter)) {
        		candidate 			= method;
        		candidateParameter	= parameter;
        		continue;
        	}
        	
        	
		}

        m_methodObj 	= candidate;
        needsArgument	= (candidateParameter != null);
        
		if (m_methodObj == null) {
			throw new NoSuchMethodException(callback.getMethodName());
        } else {
            if (! m_methodObj.isAccessible()) { 
                m_methodObj.setAccessible(true);
            }
        }
	}
	
}
