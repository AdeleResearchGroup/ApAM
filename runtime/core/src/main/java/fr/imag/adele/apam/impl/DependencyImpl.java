package fr.imag.adele.apam.impl;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Dependency;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.MissingPolicy;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.util.ApamFilter;


public class DependencyImpl implements Dependency {

	//Relationship name
	private final String identifier;

	//Target definition (resource or component reference)
	private final ResolvableReference targetDefinition;

	//Source type for this relation (Spec, implem, instance)
	private final ComponentKind sourceType ;

	//Target type for this relation (Spec, implem, instance)
	private final ComponentKind targetType ;

	//Name of an ancestor of the Source for this relation. For ctxt relations
	private final String source ;

	// The reference to the associated component
	private final Component     component;

	/**
	 * The set of constraints that must be satisfied by the target component implementation
	 * The intrinsic ones, then those added by managers (maybe). Must be reset before any resolution.
	 */
	private final Set<String> 		implementationConstraints 		= new HashSet<String> ();
	private final Set<String> 		mngImplementationConstraints 	= new HashSet<String> ();
	private final Set<ApamFilter> 	implementationConstraintFilters = new HashSet<ApamFilter> ();
	private final Set<ApamFilter> 	mngImplementationConstraintFilters = new HashSet<ApamFilter> () ;
	private final boolean isStaticImplemConstraintFilters ;

	/**
	 * The set of constraints that must be satisfied by the target component instance
	 */
	private final Set<String> 		instanceConstraints 		= new HashSet<String> ();
	private final Set<String>		mngInstanceConstraints 		= new HashSet<String> ();
	private final Set<ApamFilter> 	instanceConstraintFilters 	= new HashSet<ApamFilter> ();
	private final Set<ApamFilter> 	mngInstanceConstraintFilters= new HashSet<ApamFilter> ();
	private final boolean isStaticInstConstraintFilters;

	/**
	 * The list of preferences to choose among candidate service provider implementation
	 */
	private final List<String> 		implementationPreferences 		= new ArrayList <String> ();
	private final List<String> 		mngImplementationPreferences 	= new ArrayList <String> ();
	private final List<ApamFilter> 	implementationPreferenceFilters = new ArrayList<ApamFilter> ();
	private final List<ApamFilter> 	mngImplementationPreferenceFilters = new ArrayList<ApamFilter> ();
	private boolean isStaticImplemPreferenceFilters = false;

	/**
	 * The list of preferences to choose among candidate service provider instances
	 */
	private final List<String> 		instancePreferences 		= new ArrayList <String> ();
	private final List<String> 		mngInstancePreferences 		= new ArrayList <String> ();
	private final List<ApamFilter> 	instancePreferenceFilters 	= new ArrayList<ApamFilter> ();
	private final List<ApamFilter> 	mngInstancePreferenceFilters= new ArrayList<ApamFilter> ();
	private boolean isStaticInstPreferenceFilters = false;

//	public static Dependency voidDep 		= new DependencyImpl () ;
	
	// Whether this dependency is declared explicitly as multiple
	private final boolean       isMultiple;

	// The policy to handle unresolved dependencies
	private MissingPolicy       missingPolicy;

	// The exception to throw for the exception missing policy
	private String              missingException;

	// Whether a dependency matching this policy must be eagerly resolved
	private Boolean             isEager;

	// Whether a resolution error must trigger a backtrack in the architecture
	private Boolean             mustHide;

//	private DependencyImpl () {
//		
//	}
	
	//The minimum info for a find.
    protected DependencyImpl (Component component,  String id, boolean isMultiple, ResolvableReference resource, ComponentKind sourceType, ComponentKind targetType) {
		this.component 			= component ;
		this.identifier			= id ;
		this.targetDefinition	= resource;
		this.isMultiple 		= isMultiple;
		this.sourceType			= (sourceType == null) ? ComponentKind.INSTANCE :  sourceType ;
		this.targetType			= (targetType == null) ? ComponentKind.INSTANCE :  targetType ;

		this.source				= "this" ;
		isStaticImplemConstraintFilters = false ;
		isStaticInstConstraintFilters = false ;
		isStaticImplemPreferenceFilters = false ;
		isStaticInstPreferenceFilters = false;
    }


	public DependencyImpl (DependencyDeclaration dep, Component component) {
		//Definition
		this.component 			= component ;
		this.targetDefinition	= dep.getTarget() ;
		this.identifier			= dep.getIdentifier() ;
		this.sourceType			= (dep.getSourceType () == null) ? ComponentKind.INSTANCE :  dep.getSourceType () ;
		this.targetType			= (dep.getTargetType () == null) ? ComponentKind.INSTANCE :  dep.getTargetType () ;
		this.source				= (dep.getSource() == null) ? component.getName() : dep.getSource().getName() ;

		// Flags
		this.isMultiple 		= dep.isMultiple();
		this.isEager 			= dep.isEager();
		this.mustHide 			= dep.isHide();
		this.missingPolicy 		= dep.getMissingPolicy();
		this.missingException 	= dep.getMissingException();

		//Constraints
		this.implementationConstraints.addAll(dep.getImplementationConstraints()) ;
		this.instanceConstraints.addAll(dep.getInstanceConstraints()) ;
		this.implementationPreferences.addAll(dep.getImplementationPreferences());
		this.instancePreferences.addAll(dep.getInstancePreferences());

		//Check if there are substitutions, and build filters
		ApamFilter f ;
		boolean subst ;
		subst = false ;
		for (String c0 : implementationConstraints) {
			if (ApamFilter.isSubstituteFilter(c0, component)) {
				subst = true ;
				for (String c : implementationConstraints) {
					f = ApamFilter.newInstanceApam(c, component) ;
					if (f != null) 
						implementationConstraintFilters.add(f) ;
				}
				break ;
			}
		}
		isStaticImplemConstraintFilters = subst ;

		subst = false ;
		for (String c0 : instanceConstraints) {
			if (ApamFilter.isSubstituteFilter(c0, component)) {
				subst = true ;
				for (String c : instanceConstraints) {
					f = ApamFilter.newInstanceApam(c, component) ;
					if (f != null) 
						instanceConstraintFilters.add(f) ;
				}
				break ;
			}
		}
		isStaticInstConstraintFilters = subst ;

		subst = false ;
		for (String c0 : implementationPreferences) {
			if (ApamFilter.isSubstituteFilter(c0, component)) {
				subst = true ;
				for (String c : implementationPreferences) {
					f = ApamFilter.newInstanceApam(c, component) ;
					if (f != null) 
						implementationPreferenceFilters.add(f) ;
				}
				break ;
			}
		}
		isStaticImplemPreferenceFilters = subst ;

		subst = false ;
		for (String c0 : instancePreferences) {
			if (ApamFilter.isSubstituteFilter(c0, component)) {
				subst = true ;
				for (String c : instancePreferences) {
					f = ApamFilter.newInstanceApam(c, component) ;
					if (f != null) 
						instancePreferenceFilters.add(f) ;
				}
				break ;
			}
		}
		isStaticInstPreferenceFilters = subst ;
	}


	/**
	 * Called after the managers have added their constraints, and before to try to resolve that dependency.
	 * First it clears the previous filters (except if immutable), and recompute them from the string contraints
	 * Adds in mng the preferences and constraints filters 
	 */
	public void computeFilters () {
		ApamFilter f ;
		/*
		 * Manager constraints. Can be different for each resolution
		 */
		mngImplementationConstraintFilters.clear();
		for (String c : mngImplementationConstraints) {
			f = ApamFilter.newInstanceApam(c, component) ;
			if (f != null) 
				mngImplementationConstraintFilters.add(f);
		}
		mngImplementationConstraints.clear();

		mngInstanceConstraintFilters.clear() ;
		for (String c : mngInstanceConstraints) {
			f = ApamFilter.newInstanceApam(c, component) ;
			if (f != null) 
				mngInstanceConstraintFilters.add(f);
		}
		mngInstanceConstraints.clear();

		mngImplementationPreferenceFilters.clear();
		for (String c : mngImplementationPreferences) {
			f = ApamFilter.newInstanceApam(c, component) ;
			if (f != null) 
				mngImplementationPreferenceFilters.add(f);
		}
		mngImplementationPreferences.clear();

		mngInstancePreferenceFilters.clear() ;
		for (String c : mngInstancePreferences) {
			f = ApamFilter.newInstanceApam(c, component) ;
			if (f != null) 
				mngInstancePreferenceFilters.add(f);
		}
		mngInstancePreferences.clear();


		/*
		 * intrinsic constraints. To recompute only if they have substitutions.
		 */
		if (!isStaticImplemConstraintFilters) {
			implementationConstraintFilters.clear();
			for (String c : implementationConstraints) {
				f = ApamFilter.newInstanceApam(c, component) ;
				if (f != null) 
					implementationConstraintFilters.add(f);
			}
		}

		if (!isStaticInstConstraintFilters) {
			instanceConstraintFilters.clear() ;
			for (String c : instanceConstraints) {
				f = ApamFilter.newInstanceApam(c, component) ;
				if (f != null) 
					instanceConstraintFilters.add(f);
			}
		}

		if (!isStaticImplemPreferenceFilters) {
			implementationPreferenceFilters.clear();
			for (String c : implementationPreferences) {
				f = ApamFilter.newInstanceApam(c, component) ;
				if (f != null) 
					implementationPreferenceFilters.add(f);
			}
		}

		if (!isStaticInstPreferenceFilters) {
			instancePreferenceFilters.clear() ;
			for (String c : instancePreferences) {
				f = ApamFilter.newInstanceApam(c, component) ;
				if (f != null) 
					instancePreferenceFilters.add(f);
			}
		}

		// concatenate preference lists
		mngImplementationPreferenceFilters.addAll(implementationPreferenceFilters) ;
		mngInstancePreferenceFilters.addAll(instancePreferenceFilters) ;

		// concatenate constraints lists
		mngImplementationConstraintFilters.addAll(implementationConstraintFilters) ;
		mngInstanceConstraintFilters.addAll(instanceConstraintFilters) ;
}


	/**
	 * return true if the component matches the constraints of that dependency.
	 * Preferences are not taken into account
	 * @param comp
	 * @return
	 */
//	@Override
//	public boolean matchDep (Component comp) {
//		return matchDep (comp.getAllProperties()) ;
//	}

	@Override
	public boolean matchDep (Map <String, Object> properties) {
		if (getSourceType()== ComponentKind.IMPLEMENTATION) {
			for (ApamFilter f : mngImplementationConstraintFilters) {
				if (! f.match0(properties)) return false ;
			}
			return true ;
		}

		if (getSourceType()== ComponentKind.INSTANCE) {
			for (ApamFilter f : mngInstanceConstraintFilters) {
				if (! f.match0(properties)) return false ;
			}
		}

		//TODO if it is a spec ...
		return true ;	
	}


	@Override
	public boolean equals(Object object) {
		if (! (object instanceof Dependency))
			return false;

		return this == object;
	}

	//	@Override
	//	public int hashCode() {
	//		return reference.hashCode();
	//	}


	/**
	 * Get the reference to the required resource
	 */
	public ResolvableReference getTarget() {
		return targetDefinition;
	}

	public String getSource () {
		return source ;
	}
	/**
	 * Get the constraints that need to be satisfied by the implementation that resolves the reference
	 */
	public Set<String> getImplementationConstraints() {
		return Collections.unmodifiableSet(implementationConstraints);
	}

	// Get the constraints that need to be satisfied by the instance that resolves the reference
	public Set<String> getInstanceConstraints() {
		return Collections.unmodifiableSet(instanceConstraints);
	}

	// Get the resource provider preferences
	public List<String> getImplementationPreferences() {
		return Collections.unmodifiableList(implementationPreferences);
	}

	// Get the instance provider preferences
	public List<String> getInstancePreferences() {
		return Collections.unmodifiableList(instancePreferences);
	}

	//Modifiable
	public Set<String> getMngImplementationConstraints() {
		return mngImplementationConstraints;
	}

	// Get the constraints that need to be satisfied by the instance that resolves the reference
	public Set<String> getMngInstanceConstraints() {
		return mngInstanceConstraints;
	}

	// Get the resource provider preferences
	public List<String> getMngImplementationPreferences() {
		return mngImplementationPreferences;
	}

	// Get the instance provider preferences
	public List<String> getMngInstancePreferences() {
		return mngInstancePreferences;
	}


	// The defining component
	public Component getComponent() {
		return component;
	}

	// Get the id of the dependency in the declaring component declaration
	public String getIdentifier() {
		return identifier;
	}


	public boolean isMultiple() {
		return isMultiple ;
	}

	// Get the policy associated with this dependency
	public MissingPolicy getMissingPolicy() {
		return missingPolicy;
	}

	// Set the missing policy used for this dependency
	protected void setMissingPolicy(MissingPolicy missingPolicy) {
		this.missingPolicy = missingPolicy;
	}

	// Whether dependencies matching this contextual policy must be resolved eagerly
	public Boolean isEager() {
		return isEager;
	}

	public boolean isEffectiveEager() {
		return isEager != null ? isEager : false;
	}

	protected void setEager(Boolean isEager) {
		this.isEager = isEager;
	}

	/**
	 * Whether an error resolving a dependency matching this policy should trigger a backtrack
	 * in resolution
	 */
	public Boolean isHide() {
		return mustHide;
	}

	protected void setHide(Boolean mustHide) {
		this.mustHide = mustHide;
	}

	// Get the exception associated with the missing policy
	public String getMissingException() {
		return missingException;
	}

	protected void setMissingException(String missingException) {
		this.missingException = missingException;
	}

	@Override
	public List<ApamFilter> getImplementationPreferenceFilters (){
		return Collections.unmodifiableList(mngImplementationPreferenceFilters) ;
	}

	//Both list have been added after filter generation
	@Override
	public List<ApamFilter> getInstancePreferenceFilters () {
		return Collections.unmodifiableList(mngInstancePreferenceFilters) ;		
	}




	public String toString () {
		StringBuffer ret = new StringBuffer ();
		ret.append (" effective dependency id: " + getIdentifier() + ". toward " + getTarget()) ;

		if (!implementationConstraintFilters.isEmpty()) {
			ret.append ("\n         Implementation Constraints");
			for (ApamFilter inj : implementationConstraintFilters) {
				ret.append ("\n            " + inj);
			}
		}
		if (!instanceConstraintFilters.isEmpty()) {
			ret.append ("\n         Instance Constraints");
			for (ApamFilter inj : instanceConstraintFilters) {
				ret.append ("\n            " + inj);
			}
		}
		if (!implementationPreferenceFilters.isEmpty()) {
			ret.append ("\n         Implementation Preferences");
			for (ApamFilter inj : implementationPreferenceFilters) {
				ret.append ("\n            " + inj);
			}
		}
		if (!instancePreferenceFilters.isEmpty()) {
			ret.append ("\n         Instance Preferences");
			for (ApamFilter inj : instancePreferenceFilters) {
				ret.append ("\n            " + inj);
			}
		}
		return ret.toString();
	}


	@Override
	public ComponentKind getSourceType() {
		return sourceType;
	}


	@Override
	public ComponentKind getTargetType() {
		return targetType;
	}


	@Override
	public boolean hasConstraints() {
		return mngImplementationConstraintFilters != null ||
				mngInstanceConstraintFilters != null ||
				implementationConstraintFilters != null ||
				instanceConstraintFilters != null ;
	}


	//Supposed to be called after filters have been computed and added
	@Override
	public Set<ApamFilter> getAllImplementationConstraintFilters() {
		return Collections.unmodifiableSet(mngImplementationConstraintFilters) ;
	}


	//Supposed to be called after filters have been computed and added
	@Override
	public Set<ApamFilter> getAllInstanceConstraintFilters() {
		return Collections.unmodifiableSet(mngInstanceConstraintFilters) ;
	}
	
//	//return the (modifiable !!) list of preferences, first intrinic, then mng.
//	@Override	
//	public List<ApamFilter> getImplementationPreferenceFilters () {
//		return mngImplementationPreferenceFilters ;
//	}
//
//	@Override	
//	public List<ApamFilter> getInstancePreferenceFilters () {
//		return mngInstancePreferenceFilters ;
//	}
//
	//return the (non modifiable) list of constraints,  intrinic and mng.
//	@Override
//	public List<ApamFilter> getImplementationConstraintsFilters () ;

}
