package fr.imag.adele.apam;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.DependencyInjection;
import fr.imag.adele.apam.declarations.MissingPolicy;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.DependencyDeclaration.Reference;

public class Dependency  {

	//Relationship name
	private final String identifier;

	//Target definition (resource or component reference)
	private final ResolvableReference targetDefinition;
	
	//Source type for this relation (Spec, implem, instance)
	private final String sourceType ;

	//Target type for this relation (Spec, implem, instance)
	private final String targetType ;

	// The reference to the associated component
	private final Component     declaringComponent;

	/**
	 * The set of constraints that must be satisfied by the target component implementation
	 * The intrinsic ones, then those added by managers (maybe). Must be reset before any resolution.
	 */
	private final Set<String> 	implementationConstraints 		= new HashSet<String> ();
	private Set<String> 		mngImplementationConstraints 	= new HashSet<String> ();
	private Set<ApamFilter> 	implementationConstraintFilters = new HashSet<ApamFilter> ();
	private Set<ApamFilter> 	mngImplementationConstraintFilters = new HashSet<ApamFilter> () ;
	private boolean isStaticImplemConstraintFilters = false;

	/**
	 * The set of constraints that must be satisfied by the target component instance
	 */
	private final Set<String> 	instanceConstraints 		= new HashSet<String> ();
	private  Set<String>		mngInstanceConstraints 		= new HashSet<String> ();
	private Set<ApamFilter> 	instanceConstraintFilters 	= new HashSet<ApamFilter> ();
	private Set<ApamFilter> 	mngInstanceConstraintFilters= new HashSet<ApamFilter> ();
	private boolean isStaticInstConstraintFilters = false;

	/**
	 * The list of preferences to choose among candidate service provider implementation
	 */
	private final List<String> 	implementationPreferences 		= new ArrayList <String> ();
	private  List<String> 		mngImplementationPreferences 	= new ArrayList <String> ();
	private  List<ApamFilter> 	implementationPreferenceFilters = new ArrayList<ApamFilter> ();
	private  List<ApamFilter> 	mngImplementationPreferenceFilters = new ArrayList<ApamFilter> ();
	private boolean isStaticImplemPreferenceFilters = false;

	/**
	 * The list of preferences to choose among candidate service provider instances
	 */
	private final List<String> 	instancePreferences 		= new ArrayList <String> ();
	private List<String> 		mngInstancePreferences 		= new ArrayList <String> ();
	private List<ApamFilter> 	instancePreferenceFilters 	= new ArrayList<ApamFilter> ();
	private List<ApamFilter> 	mngInstancePreferenceFilters= new ArrayList<ApamFilter> ();
	private boolean isStaticInstPreferenceFilters = false;

	
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


	public Dependency (DependencyDeclaration dep, Component component) {
		
		this.targetDefinition	= dep.getTarget() ;
		this.identifier			= dep.getIdentifier() ;
		this.declaringComponent = component ;
		this.sourceType			= dep.getSourceType () ;
		this.targetType			= dep.getTargetType () ;

		this.isMultiple 		= dep.isMultiple();
		this.isEager 			= dep.isEager();
		this.mustHide 			= dep.isHide();
		this.missingPolicy 		= dep.getMissingPolicy();
		this.missingException 	= dep.getMissingException();

		this.implementationConstraints	= new HashSet <String> (dep.getImplementationConstraints()) ;
		this.instanceConstraints 		= new HashSet <String> (dep.getInstanceConstraints()) ;
		this.implementationPreferences 	= new ArrayList <String>(dep.getImplementationPreferences());
		this.instancePreferences		= new ArrayList <String>(dep.getInstancePreferences());

		implementationConstraintFilters = new HashSet<ApamFilter> () ;
		ApamFilter f ;
		for (String c : dep.getImplementationConstraints()) {
			f = ApamFilter.newInstanceApam(c, component) ;
			if (f != null) 
				implementationConstraintFilters.add(f);
		}

		instanceConstraintFilters = new HashSet<ApamFilter> () ;
		for (String c : dep.getInstanceConstraints()) {
			f = ApamFilter.newInstanceApam(c, component) ;
			if (f != null) 
				instanceConstraintFilters.add(f);
		}

		implementationPreferenceFilters = new ArrayList<ApamFilter> () ;
		for (String c : dep.getImplementationPreferences()) {
			f = ApamFilter.newInstanceApam(c, component) ;
			if (f != null) 
				implementationPreferenceFilters.add(f);
		}

		instancePreferenceFilters = new ArrayList<ApamFilter> () ;
		for (String c : dep.getInstancePreferences()) {
			f = ApamFilter.newInstanceApam(c, component) ;
			if (f != null) 
				instancePreferenceFilters.add(f);
		}
	}

	//
	private void computeFilters () {
		implementationConstraintFilters = new HashSet<ApamFilter> () ;
		ApamFilter f ;
		for (String c : dep.getImplementationConstraints()) {
			f = ApamFilter.newInstanceApam(c, component) ;
			if (f != null) 
				implementationConstraintFilters.add(f);
		}

		instanceConstraintFilters = new HashSet<ApamFilter> () ;
		for (String c : dep.getInstanceConstraints()) {
			f = ApamFilter.newInstanceApam(c, component) ;
			if (f != null) 
				instanceConstraintFilters.add(f);
		}

		implementationPreferenceFilters = new ArrayList<ApamFilter> () ;
		for (String c : dep.getImplementationPreferences()) {
			f = ApamFilter.newInstanceApam(c, component) ;
			if (f != null) 
				implementationPreferenceFilters.add(f);
		}

		instancePreferenceFilters = new ArrayList<ApamFilter> () ;
		for (String c : dep.getInstancePreferences()) {
			f = ApamFilter.newInstanceApam(c, component) ;
			if (f != null) 
				instancePreferenceFilters.add(f);
		}

	}
	
	@Override
	public boolean equals(Object object) {
		if (! (object instanceof DependencyDeclaration))
			return false;

		Dependency that = (Dependency) object;
		return this.reference.equals(that.reference);
	}

//	@Override
//	public int hashCode() {
//		return reference.hashCode();
//	}

	
    //The identifier of this relationship

	/**
	 * Get the reference to the required resource
	 */
	public ResolvableReference getTarget() {
		return target;
	}

//	/**
//	 * WARNIONG. Should be used only by compiler, to compute the effective group dependency target
//	 */
//	public void setTarget(ResolvableReference resource) {
//		this.target = resource;
//	}

	/**
	 * Get the constraints that need to be satisfied by the implementation that resolves the reference
	 */
	public Set<String> getImplementationConstraints() {
		return implementationConstraints;
	}

	// Get the constraints that need to be satisfied by the instance that resolves the reference
	public Set<String> getInstanceConstraints() {
		return instanceConstraints;
	}

	// Get the resource provider preferences
	public List<String> getImplementationPreferences() {
		return implementationPreferences;
	}

	// Get the instance provider preferences
	public List<String> getInstancePreferences() {
		return instancePreferences;
	}


	// Get the constraints that need to be satisfied by the implementation that resolves the reference
	public Set<ApamFilter> getImplementationConstraintFilters() {
		return implementationConstraintFilters;
	}

	// Get the constraints that need to be satisfied by the instance that resolves the reference
	public Set<ApamFilter> getInstanceConstraintFilters() {
		return instanceConstraintFilters;
	}

	// Get the resource provider preferences
	public List<ApamFilter> getImplementationPreferenceFilters() {
		return implementationPreferenceFilters;
	}

	// Get the instance provider preferences
	public List<ApamFilter> getInstancePreferenceFilters() {
		return instancePreferenceFilters;
	}

	// The defining component
	public Component getComponent() {
		return declaringComponent;
	}

	// Get the id of the dependency in the declaring component declaration
	public String getIdentifier() {
		return identifier;
	}

	// Get the reference to this declaration
//	public Reference getReference() {
//		return reference;
//	}

	public boolean isMultiple() {
		return isMultiple ;
	}

	// Get the policy associated with this dependency
	public MissingPolicy getMissingPolicy() {
		return missingPolicy;
	}

	// Set the missing policy used for this dependency
	public void setMissingPolicy(MissingPolicy missingPolicy) {
		this.missingPolicy = missingPolicy;
	}

	// Whether dependencies matching this contextual policy must be resolved eagerly
	public Boolean isEager() {
		return isEager;
	}

	public boolean isEffectiveEager() {
		return isEager != null ? isEager : false;
	}

	public void setEager(Boolean isEager) {
		this.isEager = isEager;
	}

	/**
	 * Whether an error resolving a dependency matching this policy should trigger a backtrack
	 * in resolution
	 */
	public Boolean isHide() {
		return mustHide;
	}

	public void setHide(Boolean mustHide) {
		this.mustHide = mustHide;
	}

	// Get the exception associated with the missing policy
	public String getMissingException() {
		return missingException;
	}

	public void setMissingException(String missingException) {
		this.missingException = missingException;
	}


	public String toString () {
		StringBuffer ret = new StringBuffer ();
		ret.append (" effective dependency id: " + getIdentifier() + ". toward " + getTarget()) ;

		if (!getImplementationConstraintFilters().isEmpty()) {
			ret.append ("\n         Implementation Constraints");
			for (ApamFilter inj : getImplementationConstraintFilters()) {
				ret.append ("\n            " + inj);
			}
		}
		if (!getInstanceConstraintFilters().isEmpty()) {
			ret.append ("\n         Instance Constraints");
			for (ApamFilter inj : getInstanceConstraintFilters()) {
				ret.append ("\n            " + inj);
			}
		}
		if (!getImplementationPreferenceFilters().isEmpty()) {
			ret.append ("\n         Implementation Preferences");
			for (ApamFilter inj : getImplementationPreferenceFilters()) {
				ret.append ("\n            " + inj);
			}
		}
		if (!getInstancePreferenceFilters().isEmpty()) {
			ret.append ("\n         Instance Preferences");
			for (ApamFilter inj : getInstancePreferenceFilters()) {
				ret.append ("\n            " + inj);
			}
		}
		return ret.toString();

	}
}

/**
 * Set the filters, as a transformation of the string into filters, and substitutions
 */
//	public void setImplementationConstraintFilters(Set<ApamFilter> filters) {
//		implementationConstraintFilters = filters;
//	}
//	public void setInstanceConstraintFilters(Set<ApamFilter> filters) {
//		instanceConstraintFilters  = filters;;
//	}
//	public void setImplementationPreferenceFilters(List<ApamFilter> filters) {
//		implementationPreferenceFilters  = filters;;
//	}
//	public void setInstancePreferenceFilters(List<ApamFilter> filters) {
//		instancePreferenceFilters  = filters;;
//	}
//
//
//}
