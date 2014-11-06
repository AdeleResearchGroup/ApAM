package fr.imag.adele.apam.maven.plugin.validation;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration.CodeReflection;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.references.ResolvableReference;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;
import fr.imag.adele.apam.maven.plugin.validation.property.CollectionType;
import fr.imag.adele.apam.maven.plugin.validation.property.PrimitiveType;
import fr.imag.adele.apam.maven.plugin.validation.property.Type;
import fr.imag.adele.apam.maven.plugin.validation.property.TypeParser;
import fr.imag.adele.apam.util.Attribute;
import fr.imag.adele.apam.util.Substitute;
import fr.imag.adele.apam.util.Substitute.SplitSub;

/**
 * This class performs validation of contextual expressions that can include substitution of expressions
 * with navigation of relations and properties in the model
 *  
 * @author vega
 *
 */
public class ContextualExpressionValidator extends AbstractValidator<String,Type> {

	/**
	 * The parser used to get the type of referenced properties
	 */
	private final TypeParser typeParser;
	
	public ContextualExpressionValidator(AbstractValidator<?,?> parent, TypeParser typeParser) {
		super(parent);
		this.typeParser = typeParser;
	}

	/**
	 * Verifies if the value is a substitution expression
	 */
	public boolean isContextualExpression(String value) {
		return value.startsWith("@") || value.startsWith("$");
		
	}
	
	
	/**
	 * Validates that the value is valid, and tries to infer its Type.
	 * 
	 * Return null if there are errors in the substitution expressions, and may return {@link #RUNTIME_TYPE}
	 * if the type can only be determined at runtime
	 * 
	 */
	public Type validate(String value) {

		initializeState(value);
		
		if (value.startsWith("@")) {
			return validateFunctionInvocation();
		}
		else if (value.startsWith("$")) {
			return validateNavigationExpresion();
		}
		else
			return null;
	}
	
	/**
	 * Validates that the specified navigation expression is valid in the context of the component where the value is specified 
	 */
	private Type validateNavigationExpresion() {

		/*
		 * TODO We should convert class Substitute into a parser with better error reporting
		 */
		SplitSub expression = Substitute.split(value);
		if (expression == null) {
			error("Invalid substitute expression, syntax is incorrect "+value);
			return null;
		}

		/*
		 * Get the source of the relations' navigation
		 */
		String sourceName						= expression.sourceName;
		ComponentReference<?> sourceReference	= sourceName.equals("this") ? getComponent().getReference() : new ComponentReference<ComponentDeclaration>(sourceName);
		ComponentDeclaration source				= getComponent(sourceReference, true);

		if (source == null) {
			error("Invalid substitute expression "+value+ ", component not found "+sourceReference.getName());
			return null;
		}
		
		/*
		 * iteratively get the target and the multiplicity of the relations' navigation
		 */
		
		boolean hasMultipleNavigation	= false;
		ResolvableReference target		= source.getReference();

		navigation:
		for (String relationName : expression.depIds != null ? expression.depIds : Collections.<String> emptyList()) {


			/*
			 * Otherwise try to best approximate the target of the relation with the build-time information 
			 */
			if (CST.isFinalRelation(relationName)) {
				
				if (relationName.equals(CST.REL_COMPOSITE) || relationName.equals(CST.REL_COMPOTYPE) || relationName.equals(CST.REL_CONTAINS)) {
					/*
					 * For relations that navigate the nested hierarchy of composites, we cannot know the actual types
					 * at build-time, as this hierarchy is completely build at runtime. We simply stop navigation
					 */
					target = new UnknownReference(new ResourceReference("<RUNTIME>"));
				}				
				else {
					/*
					 * For relations that navigate the abstraction levels, we can navigate to more abstract levels, and
					 * for more concrete levels we continue validating in the same context
					 */
					ComponentKind targetLevel = source.getKind();

					if (relationName.equals(CST.REL_SPEC)) {
						targetLevel = ComponentKind.SPECIFICATION;
					}
					else if (relationName.equals(CST.REL_IMPL) || relationName.equals(CST.REL_IMPLS)) {
						targetLevel = ComponentKind.IMPLEMENTATION;
					}
					else if (relationName.equals(CST.REL_INST) || relationName.equals(CST.REL_INSTS)) {
						targetLevel = ComponentKind.INSTANCE;
					}
					
					ComponentDeclaration sourceAtDifferentLevel =  changeAbstractionLevel(source,targetLevel);
					if (sourceAtDifferentLevel == null) {
						error("Invalid substitute expression "+value+ " , relation "+quoted(relationName)+" not defined for component "+source.getName());
						return null;
					}
					
					source					= sourceAtDifferentLevel;
					target					= source.getReference();
					hasMultipleNavigation	= hasMultipleNavigation || relationName.equals(CST.REL_IMPLS) || relationName.equals(CST.REL_INSTS);
				}
			}
			else {

				/*
				 * For normal relations, we get the relation declaration and follow the specified target
				 */
				RelationDeclaration relation = source.getRelation(relationName);
				
				if (relation == null) {
					error("Invalid substitute expression "+value+ " , relation "+quoted(relationName)+" not defined in component "+source.getName());
					return null;
				}
				
				target					= relation.getTarget();
				hasMultipleNavigation	= hasMultipleNavigation || relation.isMultiple();
				
				/*
				 * If the target is a component, we can move the source one step further
				 */
				if (relation.getTarget().as(ComponentReference.class) != null) {
					source = getComponent(relation.getTarget().as(ComponentReference.class),true);
					if (source == null) {
						error("Invalid substitute expression "+value+ " , relation "+quoted(relationName)+" has unknown target "+relation.getTarget().getName());
						return null;
					}
					
				}
				
			}
			
			/*
			 * When we get to an interface target, we cannot validate any further
			 */
			if (target.as(ResourceReference.class) != null)
				break navigation;
			
		}

		
		/*
		 * Get the type of the referenced property
		 */
		Type propertyType = null;

		if (! Attribute.isFinalAttribute(expression.attr)) {
			

			/*
			 * If the last target cannot be determined at build time, simply give up 
			 */
			if (target.as(ComponentReference.class) == null) {
				warning("Substitute expression "+value+ ", cannot be completely validated at build-time");
				return Type.NONE;
			}
			
			/*
			 * The last computed source of the navigation is used to look for the final property
			 */
			PropertyDefinition property = source.getPropertyDefinition(expression.attr);
			if (property == null) {
				error("Invalid substitute expression "+value+ ", invalid property "+quoted(expression.attr)+" for component "+target.getName());
				return null;
			}

			/*
			 * Infer the type of the expression from the type of the property, and the multiplicity of the navigation
			 */
			
			propertyType = typeParser.parse(property.getType()); 
			if (propertyType == null) {
				error("Invalid substitute expression "+value+ " , invalid type "+property.getType()+" for property "+property.getName());
				return null;
			}
			
		}
		
		
		/*
		 * If there are prefixes or suffixes, or the referenced property is predefined, the expression is a string
		 */
		if (Attribute.isFinalAttribute(expression.attr) || expression.prefix != null || expression.suffix != null)
			propertyType = PrimitiveType.STRING; 
		
		/*
		 * If at least one of the navigates relations is multiple, the result type is a collection, even if the property is
		 * not a collection. 
		 * 
		 * However, if the property is already a collection the result is the concatenation of all sets
		 */
		return hasMultipleNavigation && !(propertyType instanceof CollectionType) ? new CollectionType(propertyType,true) :  propertyType;
	}
	

	
	/**
	 * Given a component, return a parent of this component at the specified abstraction level.
	 * 
	 * NOTE Notice that we can only return a more abstract component, by navigating the group hierarchy. 
	 * 
	 * If a more concrete level is requested, we simply return the given component. This is enough for 
	 * validating substitution expression, as the mentioned relations and attributes must be defined in
	 * the abstract component.
	 */
	private ComponentDeclaration changeAbstractionLevel(ComponentDeclaration component, ComponentKind level) {
		
		ComponentDeclaration result = component;
		while (level.isMoreAbstractThan(result.getKind())) {
			
			/*
			 * try to go up the abstraction levels
			 */
			ComponentDeclaration parent = getComponent(result.getGroupVersioned(),true);
			
			/*
			 * There is no ancestor defined, we have to stop
			 */
			if (parent == null) {
				return null;
			}
			
			result = parent;
		}
		
		return result;
	}
	
	/**
	 * Validates that the specified function invocation is valid in the context of the component where the value is specified 
	 */
	private Type validateFunctionInvocation() {
		

		/*
		 *	A function is only valid for instrumented concrete components 
		 */
		
		ComponentDeclaration context 	= getComponent();
		String method 					= value.substring(1).trim();
		
		if (!(context instanceof AtomicImplementationDeclaration)) {
			error("Invalid substitute value, function substitution is only valid for atomic components "+method);
			return null;
		}
		
		/*
		 *  Validate the specified method is valid
		 */
		CodeReflection reflection = ((AtomicImplementationDeclaration) context).getReflection();

		try {
			/*
			 * Verify the method has a single parameter of type Instance
			 */
			String parameterType  = reflection.getMethodParameterType(method,true);
			
			if (parameterType == null) {
				error("Invalid substitute value, method "+method+" with a single parameter is not defined in class "+reflection.getClassName());
				return null;
			}
			
			if (!ComponentKind.INSTANCE.isAssignableTo(parameterType)) {
				error("Invalid substitute value, method "+method+" with a single Instance parameter is not defined in class "+reflection.getClassName());
				return null;
			}

			/*
			 * Verify the method return type is one of the supported property primitive types
			 */
			String  returnType = reflection.getMethodReturnType(method, null,true);
			if (returnType == null) {
				error("Invalid substitute value, method "+method+"does not return a value");
				return null;
			}
			
			for (Type primitive : PrimitiveType.values()) {
				if (primitive.isAssignableFrom(returnType))
					return primitive;
			}
			
			/*
			 * As a last resort, we also consider collections of strings 
			 * 
			 *  TODO modify CodeReflection to perform this validation and get the type of the collection if possible
			 *  to be able to accept collections of primitive types
			 */
			if ( returnType.equals(Collection.class.getCanonicalName()) || returnType.equals(Set.class.getCanonicalName())) {
				return new CollectionType(PrimitiveType.STRING,true);
			}
			
			error("Invalid substitute value, method "+method+"does not return a value of a valid type "+returnType);
			return null;
			
		} catch (NoSuchMethodException exc) {
			error("Invalid substitute value, method "+method+" with a single parameter is not defined in class "+reflection.getClassName());
			return null;
		}
		
	}
	
	/**
	 * The value that is being validated
	 */
	private String value;

	/**
	 * Initializes the internal state of the validator
	 */
	protected void initializeState(String value) {
		this.value		= value.trim();
	}	
	
	/**
	 * Frees all references to the state of validation
	 */
	@Override
	public void resetState() {
		
		this.value 		= null;
		super.resetState();
	}
	
}
