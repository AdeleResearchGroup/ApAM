package fr.imag.adele.apam.apamMavenPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.ComponentReference;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.ConstrainedReference;
import fr.imag.adele.apam.core.ContextualResolutionPolicy;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.DependencyInjection;
import fr.imag.adele.apam.core.DependencyPromotion;
import fr.imag.adele.apam.core.GrantDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.MessageReference;
import fr.imag.adele.apam.core.OwnedComponentDeclaration;
import fr.imag.adele.apam.core.PropertyDefinition;
import fr.imag.adele.apam.core.Reference;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.core.VisibilityDeclaration;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Util;

public class CheckObr {

	private static Logger logger = LoggerFactory.getLogger(CheckObr.class);

	private static final Set<String>             allFields         = new HashSet<String>();
	private static final Set<String>             allOwns           = new HashSet<String>();
	private static final Set<String>             allGrants         = new HashSet<String>();

	private static boolean                       failedChecking = false;

	/**
	 * An string value that will be used to represent mandatory attributes not specified. From CoreParser.
	 */
	public final static String                   UNDEFINED      = new String("<undefined value>");


	public static boolean getFailedChecking() {
		return CheckObr.failedChecking;
	}

	public static void error(String msg) {
		CheckObr.failedChecking = true;
		logger.error("ERROR: " + msg);
	}

	public static void warning(String msg) {
		logger.error("Warning: " + msg);
	}

	public static void setFailedParsing(boolean failed) {
		CheckObr.failedChecking = failed;
	}

	/**
	 * Checks if the constraints set on this dependency are syntacticaly valid.
	 * Only for specification dependencies.
	 * Checks if the attributes mentioned in the constraints can be set on an implementation of that specification.
	 * @param dep a dependency
	 */
	private static void checkConstraint(DependencyDeclaration dep) {
		if ((dep == null) || !(dep.getTarget() instanceof ComponentReference))
			return;

		//get the spec or impl definition
		ApamCapability cap = ApamCapability.get(dep.getTarget().as(ComponentReference.class));
		if (cap == null)
			return;

		//computes the attributes that can be associated with this spec or implementations members
		Map<String, String> validAttrs = cap.getValidAttrNames();

		CheckObr.checkFilters(dep.getImplementationConstraints(), dep.getImplementationPreferences(), validAttrs, dep.getTarget().getName());
		CheckObr.checkFilters(dep.getInstanceConstraints(), dep.getInstancePreferences(), validAttrs, dep.getTarget().getName());
	}


	private static void checkFilters(Set<String> filters, List<String> listFilters, Map<String, String> validAttr, String comp) {
		if (filters != null) {
			for (String f : filters) {
				ApamFilter parsedFilter = ApamFilter.newInstance(f);
				parsedFilter.validateAttr(validAttr, f, comp);
			}
		}
		if (listFilters != null) {
			for (String f : listFilters) {
				ApamFilter parsedFilter = ApamFilter.newInstance(f);
				parsedFilter.validateAttr(validAttr, f, comp);
			}
		}
	}


	/**
	 * Checks the attributes *defined* in the component; 
	 * if valid, they are returned.
	 * Then the attributes pertaining to the entity above are added.
	 * Then the final attributes
	 * @param component the component to check
	 */
	public static Map<String, String> getValidProperties(ComponentDeclaration component) {
		//the attributes to return
		Map<String, String> ret = new HashMap <String, String> ();
		//Properties of this component
		Map<String, String> properties = component.getProperties();

		ApamCapability entCap = ApamCapability.get(component.getReference()) ;
		if (entCap == null) return ret ; //should never happen.

		//return the valid attributes
		for (String attr : properties.keySet()) {
			if (validDefObr (entCap, attr, properties.get(attr))) {
				ret.put(attr, properties.get(attr)) ;
			}
		}

		//add the attribute coming from "above" if not already instantiated and heritable
		ApamCapability group = entCap.getGroup() ;
		if (group != null && group.getProperties()!= null) {
			for (String prop : group.getProperties().keySet()) {
				if (ret.get(prop) == null && Util.isInheritedAttribute(prop)) {
					ret.put(prop, group.getProperties().get(prop)) ;
				}			
			}
		}	

		/*
		 * Add the default values specified in the group for properties not
		 * explicitly initialized
		 */
		if (group != null) {
			for (String prop : group.getValidAttrNames().keySet()) {
				if (! Util.isInheritedAttribute(prop)) 
					continue;
				if ( ret.get(prop) != null )
					continue;
				if (group.getAttrDefault(prop) == null)
					continue;

				ret.put(prop, group.getAttrDefault(prop)) ;
			}
		} 

		/*
		 * Add the component characteristics as final attributes, only if explicitly defined.
		 * Needed to compile members. 
		 */
//		ret.put(CST.INSTANTIABLE, Boolean.toString(component.isInstantiable())) ;
//		ret.put(CST.SINGLETON, Boolean.toString(component.isSingleton())) ;
//		ret.put(CST.SHARED, Boolean.toString(component.isShared())) ;

		if (component.isDefinedInstantiable()) {
			ret.put(CST.INSTANTIABLE, Boolean.toString(component.isInstantiable())) ;
		}
		if (component.isDefinedSingleton()) {
			ret.put(CST.SINGLETON, Boolean.toString(component.isSingleton())) ;
		}
		if (component.isDefinedShared()) {
			ret.put(CST.SHARED, Boolean.toString(component.isShared())) ;
		}
		
		
		return ret ;
	}

	/**
	 * Checks if the attribute / values pair is valid for the component ent.
	 * If a final attribute, it is ignored but returns false. (cannot be set).
	 * 
	 * @param entName
	 * @param attr
	 * @param value
	 * @param groupProps
	 * @param superGroupProps
	 * @return
	 */
	private static boolean validDefObr (ApamCapability ent, String attr, String value) {
		if (Util.isFinalAttribute(attr))return false ;
		if (!Util.validAttr(ent.getName(), attr)) return false  ;

		if (ent.getGroup()!= null && ent.getGroup().getProperties().get(attr) != null)  {
			warning("Cannot redefine attribute \"" + attr + "\"");
			return false ;
		}

		String defAttr = ent.getAttrDefinition(attr) ;

		if (defAttr == null) {
			warning("In " + ent.getName() + ", attribute \"" + attr + "\" used but not defined.");
			return false ;
		}

		return Util.checkAttrType(attr, value, defAttr) ;		
	}


	/**
	 * An implementation has the following provide; check if consistent with the list of provides found in "cap".
	 * 
	 * @param cap. Can be null.
	 * @param interfaces = "{I1, I2, I3}" or I1 or null
	 * @param messages= "{M1, M2, M3}" or M1 or null
	 * @return
	 */
	public static boolean checkImplProvide(ComponentDeclaration impl, String spec, Set<InterfaceReference> interfaces,
			Set<MessageReference> messages) {
		if (!(impl instanceof AtomicImplementationDeclaration)) return true ;

		if (spec == null)
			return true;
		ApamCapability cap = ApamCapability.get(new SpecificationReference(spec));
		if (cap == null) {
			return true;
		}

		Set<MessageReference> specMessages = cap.getProvideMessages();
		Set<InterfaceReference> specInterfaces = cap.getProvideInterfaces();

		if (!(messages.containsAll(specMessages)))
			CheckObr.error("Implementation " + impl.getName() + " must produce messages "
					+ Util.toStringSetReference(specMessages)) ;

		if (!(interfaces.containsAll(specInterfaces)))
			CheckObr.error("Implementation " + impl.getName() + " must implement interfaces "
					+ Util.toStringSetReference(specInterfaces)) ;

		return true;
	}


	public static void checkCompoMain(CompositeDeclaration composite) {
		String name = composite.getName();
		String implName = composite.getMainComponent().getName();
		ApamCapability cap = ApamCapability.get(composite.getMainComponent());
		if (cap == null) {
			return;
		}
		if (composite.getSpecification() != null) {
			String spec = composite.getSpecification().getName();
			String capSpec = cap.getProperty(CST.PROVIDE_SPECIFICATION);
			if ((capSpec != null) && !spec.equals(capSpec)) {
				CheckObr.error("In " + name + " Invalid main implementation. " + implName
						+ " must implement specification " + spec);
			}
		}

		Set<MessageReference> mainMessages = cap.getProvideMessages();
		Set<MessageReference> compositeMessages = composite.getProvidedResources(MessageReference.class);
		if (!mainMessages.containsAll(compositeMessages))
			CheckObr.error("In " + name + " Invalid main implementation. " + implName
					+ " produces messages " + mainMessages
					+ " instead of " + compositeMessages);

		Set<InterfaceReference> mainInterfaces = cap.getProvideInterfaces() ;
		Set<InterfaceReference> compositeInterfaces = composite.getProvidedResources(InterfaceReference.class);
		if (!mainInterfaces.containsAll(compositeInterfaces))
			CheckObr.error("In " + name + " Invalid main implementation. " + implName
					+ " implements " + mainInterfaces
					+ " instead of " + compositeInterfaces);
	}

	/**
	 * For all kinds of components checks the dependencies : fields (for implems), and constraints.
	 * 
	 * @param component
	 */
	public static void checkDependencies(Set<DependencyDeclaration> deps) {
		//Set<DependencyDeclaration> deps = component.getDependencies();
		if (deps == null)
			return;
		CheckObr.allFields.clear();
		Set<String> depIds = new HashSet<String>();
		for (DependencyDeclaration dep : deps) {
			if (depIds.contains(dep.getIdentifier())) {
				CheckObr.error("Dependency " + dep.getIdentifier() + " allready defined.");
			} else
				depIds.add(dep.getIdentifier());
			// validating dependency constraints and preferences..
			CheckObr.checkConstraint(dep);
			// Checking fields and complex dependencies
			CheckObr.checkFieldTypeDep(dep);
		}
	}

//	public static void checkRequire(Set<DependencyDeclaration> deps) {
//		Set<DependencyDeclaration> deps = component.getDependencies();


	/**
	 * Provided a dependency "dep" (simple or complex) checks if the field type and attribute multiple are compatible.
	 * For complex dependency, for each field, checks if the target specification implements the field resource.
	 * 
	 * @param dep : a dependency
	 * @param component : the component currently analyzed
	 */
	private static void checkFieldTypeDep(DependencyDeclaration dep) {
//		if (!(component instanceof AtomicImplementationDeclaration)) return ;

		// All field must have same multiplicity, and must refer to interfaces and messages provided by the specification.

		Set<ResourceReference> specResources = new HashSet<ResourceReference>();

		if (dep.getTarget() instanceof ComponentReference<?>) {
			ApamCapability cap = ApamCapability.get((ComponentReference)dep.getTarget()) ;
			if (cap == null) return ;
			specResources = cap.getProvideResources() ;
		}
		else {
			specResources.add(dep.getTarget().as(ResourceReference.class));
		}

		for (DependencyInjection innerDep : dep.getInjections()) {

			String type = innerDep.getResource().getJavaType();

			if ((innerDep.getResource() != ResourceReference.UNDEFINED) && !(specResources.contains(innerDep.getResource()))) {
				CheckObr.error("Field "
						+ innerDep.getName()
						+ " is of type " + type
						+ " which is not implemented by specification or implementation " + dep.getIdentifier());
			}
		}
	}

	/**
	 * Provided an atomic dependency, returns if it is multiple or not.
	 * Checks if the same field is declared twice.
	 * 
	 * @param dep
	 * @param component
	 * @return
	 */
	public static boolean isFieldMultiple(DependencyInjection dep, ComponentDeclaration component) {
		if (CheckObr.allFields.contains(dep.getName()) && !dep.getName().equals(CheckObr.UNDEFINED)) {
			CheckObr.error("In " + component.getName() + " field/method " + dep.getName()
					+ " allready declared");
		}
		else {
			CheckObr.allFields.add(dep.getName());
		}

		return dep.isCollection();
	}

	/**
	 * Checks if the component characteristics : shared, exclusive, instantiable, singleton, 
	 * when explicitly defined, are not in contradiction with the group definition.
	 * 
	 * @param component
	 */
	public static void checkComponentHeader (ComponentDeclaration component) {
		ApamCapability cap = ApamCapability.get(component.getReference()) ;
		if (cap == null) return ;
		ApamCapability group = cap.getGroup() ;

		while (group != null) {
			if (cap.shared() != null && group.shared() != null && (cap.shared() != group.shared()) ) {
				error ("The \"shared\" property is incompatible with the value declared in " + group.getName()) ;
			}
			if (cap.instantiable() != null && group.instantiable() != null && (cap.instantiable() != group.instantiable()) ) {
				error ("The \"Instantiable\" property is incompatible with the value declared in " + group.getName()) ;
			}
			if (cap.singleton() != null && group.singleton() != null && (cap.singleton() != group.singleton()) ) {
				error ("The \"Singleton\" property is incompatible with the value declared in " + group.getName()) ;
			}
			group = group.getGroup() ;
		}
	}
	

	
	/**
	 * check all the characteristics that can be found in the <contentMngt> of a composite
	 * @param component
	 */
	public static void checkCompositeContent (CompositeDeclaration component) {

		checkStart ((CompositeDeclaration) component) ;
		checkState ((CompositeDeclaration) component) ;
		checkOwn ((CompositeDeclaration) component) ;
		checkVisibility ((CompositeDeclaration) component) ;
		checkContextualDependencies ((CompositeDeclaration) component) ;
		checkPromote ((CompositeDeclaration) component) ;
	}

	/**
	 * Check the start characteristic.
	 * It is very similar to an instance declaration, plus a trigger.
	 * @param component
	 */
	private static void checkStart (CompositeDeclaration component) {
		for (InstanceDeclaration start : component.getInstanceDeclarations()) {
			ImplementationReference implRef = start.getImplementation() ;
			if (implRef == null) {
				error ("Implementation name cannot be null") ;
				continue ;
			}
			ApamCapability cap = ApamCapability.get(implRef) ;
			if (cap == null) {
				continue ;
			}
			for (String attr : start.getProperties().keySet()) {
				validDefObr(cap, attr, start.getProperties().get(attr)) ;
			}
			checkDependencies(start.getDependencies()) ;
			
			checkTrigger (start) ;
		}
	}

	private static void checkTrigger (InstanceDeclaration start) {
		Set<ConstrainedReference> trig = start.getTriggers() ;
		for (ConstrainedReference ref : trig) {
			ResolvableReference target = ref.getTarget() ;
			ComponentReference compoRef = target.as(ComponentReference.class) ;
			if (compoRef == null) {
				error ("Start trigger not related to a valid component") ;
				continue ;
			}
			ApamCapability cap = ApamCapability.get(compoRef) ;
			if (cap == null) {
				//error ("Unknown component " + target.getName()) ;
				continue ;
			}
			
			Map<String, String> validAttrs = cap.getValidAttrNames();

			CheckObr.checkFilters(ref.getImplementationConstraints(), ref.getImplementationPreferences(), validAttrs, ref.getTarget().getName());
			CheckObr.checkFilters(ref.getInstanceConstraints(), ref.getInstancePreferences(), validAttrs, ref.getTarget().getName());
		}
	}

	/**
	 * Check the state characteristic.
	 * <own specification="Door" property=”location” value=”{entrance, exit}”>
	 * @param component
	 */
	private static Set<String> checkState (CompositeDeclaration component) {
		PropertyDefinition.Reference ref = component.getStateProperty() ;
		if (ref == null) {
			return null;
		}

		ComponentReference compo = ref.getDeclaringComponent() ;
		if (! (compo instanceof ImplementationReference)) {
			error ("A state must be associated with an implementation.") ;
			return null;
		}
		ApamCapability implCap = ApamCapability.get(compo) ;
		if (implCap == null) {
			error ("Implementation for state unavailable: " + compo.getName()) ;
			return null;
		}
		String type = implCap.getAttrDefinition(ref.getIdentifier()) ;
		if (type == null) {
			error ("The state attribute " + ref.getIdentifier() + " on implementation " 
					+ compo.getName() + " is undefined.") ;
			return null ;
		}

		Set<String> values = Util.splitSet(type);
		if (values.isEmpty()) {
			error ("State attribute " + ref.getIdentifier() +  " is not an enumeration. Invalid state attribute") ;
			return null ;	
		}
		return values ;
	}

	private static boolean visibilityExpression (String expr) {
		if (expr == null) return true ;

		if (expr.equals(CST.V_FALSE) || expr.equals(CST.V_TRUE)) 
			return true ;

		try {
			ApamFilter f = ApamFilter.newInstance(expr) ;
		} catch (Exception e) {
			error ("Bad filter in visibility expression " + expr) ;
			return false ;
		}
		return true ;
	}

	private static void checkVisibility (CompositeDeclaration component) {
		VisibilityDeclaration visiDcl = component.getVisibility() ;
		if (! visibilityExpression(visiDcl.getApplicationInstances())) 
			error ("bad expression in Application visibility: " + visiDcl.getApplicationInstances()) ;
		if (! visibilityExpression(visiDcl.getFriendImplementations())) 
			error ("bad expression in Friend implementation visibility: " + visiDcl.getFriendImplementations()) ;
		if (! visibilityExpression(visiDcl.getFriendInstances())) 
			error ("bad expression in Friend instance visibility: " + visiDcl.getFriendInstances()) ;
		if (! visibilityExpression(visiDcl.getLocalImplementations())) 
			error ("bad expression in Local implementation visibility: " + visiDcl.getLocalImplementations()) ;
		if (! visibilityExpression(visiDcl.getLocalInstances())) 
			error ("bad expression in Local instance visibility: " + visiDcl.getLocalInstances()) ;
		if (! visibilityExpression(visiDcl.getBorrowImplementations())) 
			error ("bad expression in Borrow implementation visibility: " + visiDcl.getBorrowImplementations()) ;
		if (! visibilityExpression(visiDcl.getBorrowInstances())) 
			error ("bad expression in Borrow instance visibility: " + visiDcl.getBorrowInstances()) ;
	}

	/**
	 * 
	 * @param component
	 */
	private static void checkOwn (CompositeDeclaration component) {
		Set<OwnedComponentDeclaration> owned = component.getOwnedComponents() ;
		
		//check that a single own clause is defined for a component and its members
		Set <String> compRef = new HashSet <String> () ;
		for (OwnedComponentDeclaration own : owned) {
			ApamCapability ownCap = ApamCapability.get(own.getComponent()) ;
			if (ownCap == null) {
				error ("Unknown component in own expression : " + own.getComponent().getName()) ;
				continue ;
			}
			
			//computes the attributes that can be associated with this spec or implementations members
			if (own.getProperty() == null) {
				error ("Need a property for an own clause") ;
				continue ;
			}
			String prop = own.getProperty().getIdentifier() ;
			String type = ownCap.getAttrDefinition(prop) ;
			if (type == null) {
				error ("Undefined attribute " + own.getProperty().getIdentifier() + " for component " 
						+ own.getComponent().getName() + " in own expression") ;
				continue ;
			}
			Set<String> values = Util.splitSet(type);
			if (values.isEmpty()) {
				error ("Attribute " + own.getProperty().getIdentifier() + " for component " 
						+ own.getComponent().getName() + " is not an enumeration. Invalid in own expression") ;
				continue ;	
			}
			
			if (!values.containsAll(own.getValues()))  {
				error ("In own clause, invalid values for attribute " + prop + "=" + Util.toStringResources(own.getValues()) 
						+  " \n    for component " + own.getComponent().getName() + ". Expected " + Util.toStringResources(values)) ;
			}
			
			/**
			 * Check that a single own clause applies for the same component, and members
			 * At execution must also be checked that if other grant clauses in other composites 
			 * for that component or its members:
			 * 		-It must be the same property
			 * 		-It must be different values
			 */
			if (compRef.contains(own.getComponent().getName())) {
				error ("Another Own clause exist for " + own.getComponent().getName() + " in this composite declaration") ;
				continue ;
			}
			compRef.add(own.getComponent().getName()) ;
			if (ownCap.getGroup() != null) {
				compRef.add(ownCap.getGroup().getName()) ;
			}

			checkGrant(component, own);
		}
	}

	private static void checkGrant (CompositeDeclaration component, OwnedComponentDeclaration own) {
		//Get state definition
		Set<String> stateDefinition = checkState(component) ;
		if (stateDefinition.isEmpty()) { //No valid state declaration. No valid grants.
			return ;
		}
		
		//List<GrantDeclaration> grants = own.getGrants();
		for (GrantDeclaration grant : own.getGrants()) {
			DependencyDeclaration.Reference dep = grant.getDependency() ;
			ComponentReference compo = dep.getDeclaringComponent() ;
			//TODO cannot check if the dependency has really the component as target.
			ApamCapability cap = ApamCapability.get(compo) ;
			if (cap == null) {
				error ("Unknown component in own expression : " + compo.getName()) ;
				continue ;
			}

			//Check that grant state values are valid
			Set<String> grantStates = grant.getStates() ;
			if (!stateDefinition.containsAll(grant.getStates()))  {
				error ("In Grant expression, invalid values " + Util.toStringResources(grant.getStates()) 
						+ " for state=" + Util.toStringResources(stateDefinition)) ;
			}
			
			//Check that a single grant for a given state.
			for (String def : grantStates) {
				if (allGrants.contains(def)) {
					error ("Component " + own.getComponent().getName() + " already granted when state is " + def);
					break ;
				}
				allGrants.add(def) ;
			}
		}
	}

	/**
	 * Cannot check almost nothing !
	 * Because of wild cards, components are not known, and their attribute and dependencies cannot be checked.
	 * Only the syntax of filters can be checked. 
	 * 
	 * @param component
	 */
	private static void checkContextualDependencies (CompositeDeclaration component) {
		 //List<ContextualResolutionPolicy> resol = component.getContextualResolutionPolicies() ;
	}


	/**
	 * Cannot check if the component dependency is valid.
	 * Only checks that the composite dependency is declared, and that the component is known.
	 * 
	 * @param component
	 */
	private static void checkPromote (CompositeDeclaration component) {
		for (DependencyPromotion promo : component.getPromotions()) {
			if (ApamCapability.get(promo.getContentDependency().getDeclaringComponent()) == null) {
				error ("Invalid promotion: unknown component " + promo.getContentDependency().getDeclaringComponent().getName());
			}
			Reference compoDep = promo.getCompositeDependency();
			for (DependencyDeclaration dep : component.getDependencies()) {
				if (dep.getIdentifier().equals(promo.getCompositeDependency().getIdentifier())) {
					break ;
				}
				error ("Undefined composite dependency: " + promo.getCompositeDependency().getIdentifier()) ;
			}
		}
	}

	
}
