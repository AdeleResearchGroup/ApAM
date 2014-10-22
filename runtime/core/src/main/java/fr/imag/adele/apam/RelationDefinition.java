package fr.imag.adele.apam;

import java.util.List;
import java.util.Set;

import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.CreationPolicy;
import fr.imag.adele.apam.declarations.MissingPolicy;
import fr.imag.adele.apam.declarations.ResolvePolicy;
import fr.imag.adele.apam.declarations.references.ResolvableReference;

//import fr.imag.adele.apam.util.ApamFilter;

public interface RelationDefinition {

	public CreationPolicy getCreation();

	// The source for composite relation definition
	public String getCtxtSourceName();

	// Get the constraints that need to be satisfied by the implementation that
	// resolves the reference
	public Set<String> getImplementationConstraints();

	// Get the resource provider preferences
	public List<String> getImplementationPreferences();

	// Get the constraints that need to be satisfied by the instance that
	// resolves the reference
	public Set<String> getInstanceConstraints();

	// Get the instance provider preferences
	public List<String> getInstancePreferences();

	// //Modifiable
	// //return the (modifiable !!) list of preferences, first intrinic, then
	// mng.
	// public List<ApamFilter> getImplementationPreferenceFilters () ;
	//
	// //return the (non modifiable) list of constraints, intrinic and mng.
	// public List<ApamFilter> getInstancePreferenceFilters () ;
	//
	// public Set<ApamFilter> getAllImplementationConstraintFilters ();
	// public Set<ApamFilter> getAllInstanceConstraintFilters ();

	// public Set<String> getMngImplementationConstraints();
	//
	// // Get the constraints that need to be satisfied by the instance that
	// resolves the reference
	// public Set<String> getMngInstanceConstraints() ;
	//
	// // Get the resource provider preferences
	// public List<String> getMngImplementationPreferences() ;
	//
	// // Get the instance provider preferences
	// public List<String> getMngInstancePreferences();

	// // The defining component
	// public Component getComponent() ;

	// Get the exception associated with the missing policy
	public String getMissingException();

	// //Get the source (ancestor) for ctxt relation
	// public Component getLinkSource () ;

	// Get the policy associated with this relation
	public MissingPolicy getMissingPolicy();

	// Get the id of the relation in the declaring component declaration
	public String getName();

	// return the ancestor or base (including base) corresponding to the
	// sourceKind
	public Component getRelSource(Component base);

	public ResolvePolicy getResolve();

	// Type of source
	public ComponentKind getSourceKind();

	/**
	 * return true if the component matches the constraints of that relation.
	 * Preferences are not taken into account Same as matchDep (Component comp)
	 * since component extends Map<String, Object>
	 */
	// public boolean matchRelation (Component target) ;
	// public boolean matchRelationConstraints (ComponentKind candidateKind,
	// Map<String, Object> properties) ;
	// public boolean matchRelationConstraints (Component target) ;
	// public boolean matchRelationTarget (Component target) ;

	// Get the reference to the required resource
	public ResolvableReference getTarget();

	// Type of target
	public ComponentKind getTargetKind();

	// True if the relation has constraints (preferences ignored)
	public boolean hasConstraints();

	// true if this is a dynamic wire, or a dynamic message ...
	// TODO to remove
	public boolean isDynamic();

	// Whether an error resolving a relation matching this policy should trigger
	// a backtrack
	public boolean isHide();

	// true if this is there is an associated field
	public boolean isInjected();

	// True if relation cardinality is multiple
	public boolean isMultiple();

	public boolean isRelation();

	// true if this is a Wire or a Link
	public boolean isWire();

	// //Ex in Util
	// public Resolved<?> getResolved(Set<? extends Component> candidates,
	// boolean isPromotion) ;
	//
	// public Resolved<?> getResolved(Resolved<?> candidates, boolean
	// isPromotion) ;
	//
	// public <T extends Component> T getPrefered (Set<T> candidates) ;
	//
	public boolean matchRelation(Instance compoInst, RelationDefinition compoDep);
}
