package fr.imag.adele.apam;

import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.MissingPolicy;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.util.ApamFilter;

public interface Dependency  {


	/**
	 * return true if the component matches the constraints of that dependency.
	 * Preferences are not taken into account
	 * Same as matchDep (Component comp) since component extends Map<String, Object>
	*/
	public boolean matchDep (Map <String, Object> properties) ;

	// Get the reference to the required resource
	public ResolvableReference getTarget() ;
	
	//True if the dependency has constraints (preferences ignored)
	public boolean hasConstraints () ;

	// Get the constraints that need to be satisfied by the implementation that resolves the reference
	public Set<String> getImplementationConstraints() ;

	// Get the constraints that need to be satisfied by the instance that resolves the reference
	public Set<String> getInstanceConstraints() ;
	
	//return the (modifiable !!) list of preferences, first intrinic, then mng.
	public List<ApamFilter> getImplementationPreferenceFilters () ;

	//return the (non modifiable) list of constraints,  intrinic and mng.
	public List<ApamFilter> getInstancePreferenceFilters () ;

	public Set<ApamFilter> getAllImplementationConstraintFilters ();
	public Set<ApamFilter> getAllInstanceConstraintFilters ();

	// Get the resource provider preferences
	public List<String> getImplementationPreferences() ;

	// Get the instance provider preferences
	public List<String> getInstancePreferences() ;

	//Modifiable
	public Set<String> getMngImplementationConstraints();

	// Get the constraints that need to be satisfied by the instance that resolves the reference
	public Set<String> getMngInstanceConstraints() ;

	// Get the resource provider preferences
	public List<String> getMngImplementationPreferences() ;

	// Get the instance provider preferences
	public List<String> getMngInstancePreferences();

	// The defining component
	public Component getComponent() ;

	// Get the id of the dependency in the declaring component declaration
	public String getIdentifier() ;
	
	//Get the source (ancestor) for ctxt relation
	public String getSource () ;
	
	//Type of source
	public ComponentKind getSourceType () ;
	
	//Type of target
	public ComponentKind getTargetType () ;

	//True if relation cardinality is multiple
	public boolean isMultiple();

	// Get the policy associated with this dependency
	public MissingPolicy getMissingPolicy();

	// Whether dependencies matching this contextual policy must be resolved eagerly
	public boolean isEager() ;
	
	//true if this is a dynamic wire, or a dynamic message ...
	public boolean isDynamic () ;

	//true if this is a Wire definition
	public boolean isWire () ;

	//
	public boolean isEffectiveEager();

	//Whether an error resolving a dependency matching this policy should trigger a backtrack
	public boolean isHide() ;

	// Get the exception associated with the missing policy
	public String getMissingException() ;

}


