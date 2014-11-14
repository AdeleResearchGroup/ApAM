package fr.imag.adele.apam.maven.plugin.validation;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.GrantDeclaration;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.OwnedComponentDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.RelationPromotion;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.VisibilityDeclaration;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.resources.InterfaceReference;
import fr.imag.adele.apam.declarations.references.resources.MessageReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.repository.maven.Classpath;
import fr.imag.adele.apam.maven.plugin.validation.property.EnumerationType;
import fr.imag.adele.apam.maven.plugin.validation.property.Type;
import fr.imag.adele.apam.maven.plugin.validation.property.TypeParser;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Util;

public class CompositeValidator extends ComponentValidator<CompositeDeclaration> {

	/**
	 * The parser used to validate all defined property types
	 */
	private final TypeParser 	typeParser;
	
	/**
	 * The start instance validator
	 */
	private final InstanceValidator instanceValidator;
	
	/**
	 * The contextual dependencies validator
	 */
	private final RelationValidator contextuallRelationValidator;
	
	public CompositeValidator(ValidationContext context, Classpath classpath) {
		super(context, classpath);
		
		this.typeParser						= new TypeParser();
		this.instanceValidator				= new InstanceValidator(this);
		this.contextuallRelationValidator	= new RelationValidator(this);
	}

	/**
	 * Parses the specified type
	 */
	protected Type getType(PropertyDefinition property) {
		return typeParser.parse(property.getType());
	}
	
	@Override
	public Void validate(CompositeDeclaration component) {
		Void result = super.validate(component);
		
		validateMain();
		validateContent();
		
		return result;
	}
	
	/**
	 * Validates the main implementation
	 */
	private void validateMain() {
		
		/*
		 *  Abstract composites have no main component, but must not provide any resource
		 */
		if (getComposite().getMainComponent() == null) {
			if (! getComposite().getProvidedResources(ResourceReference.class).isEmpty()) {
				error(" abstract composites can not provide resources");
			}
			return;
		}

		/*
		 *  Concrete composites must have a main component that provides everything provided by the composite
		 */
		ComponentDeclaration main 	= getComponent(getComposite().getMainComponent(),true);
		if (main == null) {
			error(" main component "+getComposite().getMainComponent().getName()+" could not be found");
			return;
		}
		
		if (getComposite().getGroupVersioned() != null && main.getGroupVersioned() != null && !getComposite().getGroupVersioned().equals(main.getGroupVersioned()) ) {
			error("main component "+ main.getName() + " must implement specification " + getComposite().getGroupVersioned().getName());
		}

		validateMainProvides(main,InterfaceReference.class);
		validateMainProvides(main,MessageReference.class);
	}

	/**
	 * Validates the provided resources of the main implementation
	 */
	private void validateMainProvides(ComponentDeclaration main, Class<? extends ResourceReference> kind) {
		Set<? extends ResourceReference> compositeResources	= getComposite().getProvidedResources(kind);
		Set<? extends ResourceReference> mainResources 		= main.getProvidedResources(kind);

		if (!mainResources.containsAll(compositeResources)) {
			error("invalid main implementation, "+ main.getName() + " must provide " + Util.list(compositeResources,true));
		}
	}

	/**
	 * check all the characteristics that can be found in the content management declaration
	 *
	 */
	private void validateContent() {

		validateVisibility();
		validateOwn();
		validatePromotions();

		validateStart();
		validateContextualRelations();
	}


	private void validateVisibility() {
		
		VisibilityDeclaration visibility = getComposite().getVisibility();
		
		validateVisibilityExpression(visibility.getApplicationInstances(), "bad expression in ExportApp visibility");
		validateVisibilityExpression(visibility.getExportImplementations(),"bad expression in Export implementation visibility");
		validateVisibilityExpression(visibility.getExportInstances(),"bad expression in Export instance visibility");
		validateVisibilityExpression(visibility.getImportImplementations(),"bad expression in Imports implementation visibility");
		validateVisibilityExpression(visibility.getImportInstances(),"bad expression in Imports instance visibility");
	}
	
	private void validateVisibilityExpression(String expression, String message) {

		if (expression == null || expression.equals(CST.V_FALSE) || expression.equals(CST.V_TRUE)) {
			return;
		}

		ApamFilter parsedExpression = parseFilter(expression);
		if (parsedExpression == null) {
			error(message+" "+quoted(expression));
		}

	}
	
	/**
	 * Validate own declarations
	 */
	private void validateOwn() {

		/*
		 * A state may optionally be defined to allow grant specification
		 */
		Type stateType = validateState();
		
		/*
		 *  The composite must be a singleton to define owns
		 */
		if (!getComposite().getOwnedComponents().isEmpty() && !getComposite().isSingleton()) {
			error("invalid own expression, a composite must be a singleton to define "+quoted("own")+" clauses");
		}


		/*
		 *  Check that a single own clause is defined for a component and its members
		 */
		Set<ComponentReference<?>> ownedComponents = new HashSet<ComponentReference<?>>();
		
		for (OwnedComponentDeclaration ownDeclaration : getComposite().getOwnedComponents()) {
			
			ComponentDeclaration owned = getComponent(ownDeclaration.getComponent(),true);
			if (owned == null) {
				error("invalid own expression, unknown component " + ownDeclaration.getComponent().getName());
				continue;
			}


			/*
			 * Verify the property used to optionally filter owned components is declared, and is an enumeration
			 * that contains the specified values
			 */
			if (ownDeclaration.getProperty() != null) {

				String propertyName			= ownDeclaration.getProperty().getIdentifier();
				PropertyDefinition property = owned.getPropertyDefinition(propertyName);
				
				if (property == null) {
					error("invalid own expression, undefined property "+ quoted(propertyName) +" in component "+ owned.getName());
					continue;
				}
				
				Type propertyType = getType(property);
				if (propertyType == null || !(propertyType instanceof EnumerationType)) {
					error("invalid own expression, property "+ quoted(propertyName) + " of component " + owned.getName() + " is not an enumeration");
					continue;
				}

				if (ownDeclaration.getValues().isEmpty()) {
					error("invalid own expression, values not specified for property "+ quoted(propertyName) + " of component " + owned.getName());
					continue;
				}

				for (String value : ownDeclaration.getValues()) {
					if (propertyType.value(value) == null) {
						error("invalid own expression, value "+quoted(value)+" is not valid for property "+ quoted(propertyName) + " of component " + owned.getName());
					}
				}
			}

			/*
			 * Check that a single own clause applies for the same component and its members. At execution, it must also
			 * be checked that if there are other grant clauses in other composites for the same component, they must 
			 * specify the same property and different values.
			 */
			if (ownedComponents.contains(owned.getReference())) {
				error("invalid own expression, another own  clause exists for "+ owned.getName()+ " in this composite declaration");
				continue;
			}
			
			ownedComponents.add(owned.getReference());
			if (owned.getGroup() != null) {
				ownedComponents.add(owned.getGroup());
			}

			validateGrant(owned,ownDeclaration,stateType);
		}
	}

	private void validateGrant(ComponentDeclaration owned, OwnedComponentDeclaration ownDeclaration, Type stateType) {

		/*
		 * Verify a state has been defined
		 */
		if (stateType == null && !ownDeclaration.getGrants().isEmpty()) {
			error("invalid grant expression, state is not defined in composite ");
			return;
		}

		Set<String> grantedStates = new HashSet<String>();
		
		for (GrantDeclaration grantDeclaration : ownDeclaration.getGrants()) {

			// Check that grant state values are valid
			for (String grantState : grantDeclaration.getStates()) {
				if (stateType.value(grantState) == null) {
					error("invalid grant expression, value "+quoted(grantState)+" is not valid for state property "+ quoted(getComposite().getStateProperty().getIdentifier()));
				}
				
				if (grantedStates.contains(grantState)) {
					error("invalid grant expression, state value"+quoted(grantState)+" alreday speciifed in another gran clause "+grantDeclaration);
				}
				
				grantedStates.add(grantState);
			}

			// Check that the granted component exists
			ComponentDeclaration granted	= getComponent(grantDeclaration.getRelation().getDeclaringComponent(),true);

			if (granted == null) {
				error("invalid grant expression, unknown component "+grantDeclaration.getRelation().getDeclaringComponent().getName()+ " in grant expression "+grantDeclaration);
				continue;
			}
			
			// Check that the component is a singleton
			if (granted.isDefinedSingleton()  && ! granted.isSingleton()) {
				warning("invalid grant expression, component "+grantDeclaration.getRelation().getDeclaringComponent().getName()+ " is not a singleton "+grantDeclaration);
			}
			
			// Check that the relation exists and has as target the OWN resource
			// OWN is a specification or an implem but the granted relation can be anything
			
			RelationDeclaration  grantedRelation = granted.getRelation(grantDeclaration.getRelation().getIdentifier());

			if (grantedRelation == null) {
				error("invalid grant expression, the relation "+ quoted(grantDeclaration.getRelation().getIdentifier()) +" is not defined in component " + granted.getName());
			}
			else if (!isCandidateTarget(grantedRelation,owned)) {
				error("invalid grant expression, the relation "+ quoted(grantDeclaration.getRelation().getIdentifier()) +" does not refer to the owned component " + owned.getName());
			}
			
		}
	}
	
	/**
	 * Validate the specified state property declaration
	 *
	 */
	private EnumerationType validateState() {
		
		PropertyDefinition.Reference state = getComposite().getStateProperty();
		
		if (state == null) {
			return null;
		}

		ComponentDeclaration declaring = getComponent(state.getDeclaringComponent(),true);
		if (declaring == null) {
			error("invalid state property "+quoted(state.getIdentifier())+", declaring component "+state.getDeclaringComponent().getName()+" is unavailable");
			return null;
		}
		
		if (! declaring.getKind().equals(ComponentKind.IMPLEMENTATION)) {
			error("invalid state property "+quoted(state.getIdentifier())+", declaring component "+state.getDeclaringComponent().getName()+" is not an implementation");
			return null;
		}
		
		// Attribute state must be defined on the implementation.
		PropertyDefinition property = declaring.getPropertyDefinition(state.getIdentifier());
		if (property == null) {
			error("invalid state property "+quoted(state.getIdentifier())+", not declared in component "+state.getDeclaringComponent().getName());
			return null;
		}

		Type type = getType(property);
		if (!(type instanceof EnumerationType)) {
			error("invalid state property "+quoted(state.getIdentifier())+", must be an enumeration and it is actually of type "+type);
			return null;
		}
		
		return (EnumerationType) type; 
	}
	
	/**
	 * Cannot check if the component relation is valid. Only checks that the composite relation is declared,
	 * and that the component is known.
	 *
	 * @param composite
	 */
	private void validatePromotions() {

		for (RelationPromotion promotion : getComposite().getPromotions()) {
			
			ComponentDeclaration source = getComponent(promotion.getContentRelation().getDeclaringComponent(),true);
			if (source == null) {
				error("invalid promotion, the source component "+ promotion.getContentRelation().getDeclaringComponent() +" is unknown");
			}
			
			RelationDeclaration promotedRelation = source != null ? source.getRelation(promotion.getContentRelation().getIdentifier()) : null;
			// Check if the dependencies are compatible
			if (source != null && promotedRelation == null) {
				error("invalid promotion, the promoted relation "+ quoted(promotion.getContentRelation().getIdentifier()) +" is not defined in component "+source.getName());
				continue;
			}

			RelationDeclaration compositeRelation = getComposite().getRelation(promotion.getCompositeRelation());
			if (compositeRelation == null) {
				error("invalid promotion, the composite relation "+ quoted(promotion.getCompositeRelation().getIdentifier()) +" is not defined");
				continue;
			}

			// Both the composite and the component have a relation with the right id. Check if the targets are compatible
			if (promotedRelation != null && compositeRelation != null && !matchPromotion(promotedRelation, compositeRelation)) {
				error("invalid promotion, the promoted relation "+ quoted(promotion.getContentRelation().getIdentifier()) +
					  " does not match the composite relation "	+ quoted(promotion.getCompositeRelation().getIdentifier()));
			}
		}
	}

	// Copy paste of the Util class ! too bad, this one uses ApamCapability
	private boolean matchPromotion(RelationDeclaration promotedRelation, RelationDeclaration compositeRelation) {

		boolean match = false;
		
		ComponentDeclaration compositeTarget = getComponent(compositeRelation.getTarget().as(ComponentReference.class),true);
		if (compositeTarget != null) {
			/*
			 * If the target of the composite relation is a component, it must satisfy the promoted relation
			 */
			match = isCandidateTarget(promotedRelation, compositeTarget);
		}
		else {
			/*
			 * Otherwise, the target resource must match exactly
			 */
			match = promotedRelation.getTarget().as(ResourceReference.class) != null && 
					promotedRelation.getTarget().equals(compositeRelation.getTarget());
		}
			
		return  match && ( !promotedRelation.isMultiple() || compositeRelation.isMultiple());
	}
	
	private void validateStart() {
		for (InstanceDeclaration instance : getComposite().getInstanceDeclarations()) {
			validate(instance,instanceValidator);
		}
	}

	private void validateContextualRelations() {
		
		for (RelationDeclaration contextual : getComposite().getContextualDependencies()) {
			validate(contextual,contextuallRelationValidator);
		}
		
		for (RelationDeclaration override : getComposite().getOverridenDependencies()) {
			validate(override,contextuallRelationValidator);
		}
		
	}

	protected CompositeDeclaration getComposite() {
		return super.getComponent();
	}
	
	@Override
	protected SpecificationDeclaration getGroup() {
		return (SpecificationDeclaration) super.getGroup();
	}

}
