package fr.imag.adele.apam.apform.impl;

import org.apache.felix.ipojo.ConfigurationException;

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
public class RelationCallback extends InstanceCallback<Component> {


    private final RelationDeclaration 		relation;
    private final RelationDeclaration.Event trigger;

	public RelationCallback(ApamInstanceManager instance, RelationDeclaration relation, RelationDeclaration.Event trigger, CallbackDeclaration callback) throws ConfigurationException {
		super(instance,callback.getMethodName());
		
		this.relation		= relation;
		this.trigger		= trigger;
		
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
				throw new ConfigurationException("invalid method declaration in callback "+getMethod()+ " for relation "+relation.getIdentifier());
			}
		}

	}
	
	public boolean isTriggeredBy(String relationName, RelationDeclaration.Event trigger) {
		return this.relation.getIdentifier().equals(relationName) && this.trigger.equals(trigger);
	}

	/**
	 * Relation callback allows injection of the service object, instead of the target component.
	 * 
	 * NOTE notice that we do not actually validate that the service object matches the parameter
	 * type. If declaration are not correct this will throw a cast exception.
	 */
	@Override
	protected Object cast(Component argument) {
		return ((Instance)argument).getServiceObject();
	}
	
	@Override
	protected boolean isExpectedParameter(Class<?> parameterType) {
    	ComponentKind targetKind = relation.getTargetKind();
    	
    	/*
    	 * If target kind is not directly specified in the declaration, wee ask APAM to perform
    	 * the calculation.
    	 */
    	if (targetKind == null && instance.getApamComponent() != null) {
    		targetKind = instance.getApamComponent().getRelation(relation.getIdentifier()).getTargetKind();
    	}

    	if (targetKind == null)
    		return false;

    	/*
    	 * Calculate the expected parameter type depending on the target kind
    	 * 
    	 * TODO for instance targets we allow the parameter to be a service object, but we do not validate
    	 * the parameter type
    	 */
    	
    	boolean isServiceParameter = false;
    	
    	Class<?> expectedType = Component.class;
    	switch (targetKind) {
    		case SPECIFICATION:
    			expectedType = Specification.class;
    			break;
    		case IMPLEMENTATION:
    			expectedType = Implementation.class;
    			break;
    		case INSTANCE:
    			expectedType = Instance.class;
    			isServiceParameter = true;
    			break;
			case COMPONENT:
				expectedType = Component.class;
				break;
			default:
				break;
    	}

    	return parameterType.isAssignableFrom(expectedType) || isServiceParameter;
	}

	
}
