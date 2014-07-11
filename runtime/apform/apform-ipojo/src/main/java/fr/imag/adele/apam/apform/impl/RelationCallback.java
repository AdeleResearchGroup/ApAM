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
import fr.imag.adele.apam.declarations.ComponentKind;
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

    private Class<?> argumentType;

	public RelationCallback(ApamInstanceManager instance, RelationDeclaration relation, RelationDeclaration.Event trigger, CallbackDeclaration callback) throws ConfigurationException {
		super(callback.getMethodName(),(String[])null,false,instance);
		
		this.instance		= instance;
		this.relation		= relation;
		this.trigger		= trigger;
		this.callback		= callback;
		
		/*
		 * We force reflection meta-data calculation in the constructor to signal errors as soon as
		 * possible. This however has a cost in terms of early class loading.
		 * 
		 * For partially declared relations, we need to wait until the apform has been fully reified
		 * in APAM in order to have access to the complete relation declaration.
		 */
		if (relation.getTargetKind() != null) {
			try {
				searchMethod();
			} catch (NoSuchMethodException e) {
				throw new ConfigurationException("invalid method declaration in callback "+callback.getMethodName()+ " for relation "+relation.getIdentifier());
			}
		}

	}
	
	public boolean isTriggeredBy(String relationName, RelationDeclaration.Event trigger) {
		return this.relation.getIdentifier().equals(relationName) && this.trigger.equals(trigger);
	}
	
	public Object invoke(Component target) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (m_methodObj == null) {
            searchMethod();
        }

		return call(argumentType != null ? new Object[] {argumentType.isInstance(target) ? target : ((Instance)target).getServiceObject()} : new Object[0]);
	}

	@Override
	protected void searchMethod() throws NoSuchMethodException {
        

    	ComponentKind targetKind = relation.getTargetKind();
    	
    	/*
    	 * If target kind is not directly specified in the declaration, wee ask APAM to perform
    	 * the calculation.
    	 */
    	if (targetKind == null && instance.getApamComponent() != null) {
    		targetKind = instance.getApamComponent().getRelation(relation.getIdentifier()).getTargetKind();
    	}

    	if (targetKind == null)
    		throw new NoSuchMethodException(callback.getMethodName());

    	/*
    	 * Calculate the expected parameter type depending on the target kind
    	 * 
    	 * TODO for instance targets we allow the parameter to be a service object, but we do not validate
    	 * the parameter type
    	 */
    	
    	boolean validateParameter = true;
    	
    	Class<?> parameterType = Component.class;
    	switch (targetKind) {
    		case SPECIFICATION:
    			parameterType = Specification.class;
    			break;
    		case IMPLEMENTATION:
    			parameterType = Implementation.class;
    			break;
    		case INSTANCE:
    			parameterType = Instance.class;
    			validateParameter = false;
    			break;
			case COMPONENT:
				parameterType = Component.class;
				break;
			default:
				break;
    	}
    	
		/*
		 * Try to find the declared method with the specified name that best matches the callback signature
		 * 
		 * NOTE Notice that we force class loading of the implementation class (and its super classes) by using
		 * reflection to search for methods.
		 */
		
        Method[] methods 			= instance.getClazz().getMethods();
        
        Method candidate 			= null;
        Class<?> candidateParameter	= null;
    	
        for (Method method : methods) {
			
        	if (! method.getName().equals(callback.getMethodName()))
				continue;

        	/*
        	 * We are looking for a method with an optional, single parameter of type
        	 * APAM component (depending on the target of the relation if specified)
        	 */

        	Class<?>[] parameters = method.getParameterTypes();
        	if (parameters.length > 1)
        		continue;

           	Class<?> parameter = parameters.length == 1 ? parameters[0] : null;
           	if (parameter != null && validateParameter && ! parameter.isAssignableFrom(parameterType))
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
        argumentType	= candidateParameter;
        
		if (m_methodObj == null) {
			throw new NoSuchMethodException(callback.getMethodName());
        } else {
            if (! m_methodObj.isAccessible()) { 
                m_methodObj.setAccessible(true);
            }
        }
	}
	
}
