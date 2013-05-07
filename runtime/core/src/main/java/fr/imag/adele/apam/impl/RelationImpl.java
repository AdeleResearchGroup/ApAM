package fr.imag.adele.apam.impl;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Relation;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.MissingPolicy;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.RelationInjection;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Util;


public class RelationImpl implements Relation {

	static Logger logger = LoggerFactory.getLogger(ApamResolverImpl.class);

	// Relationship name
	private final String identifier;

	// Target definition (resource or component reference)
	private final ResolvableReference targetDefinition;

	// Source type for this relation (Spec, implem, instance)
	private final ComponentKind sourceType;

	// Target type for this relation (Spec, implem, instance)
	private final ComponentKind targetType;

	//	// Name of an ancestor of the Source for this relation. For ctxt relations
	//	private String sourceName;

	// The actual source of this relation. Set just before a resolution
	private Component linkSource;

	// // The reference to the associated component
	// private final Component component;

	/**
	 * The set of constraints that must be satisfied by the target component
	 * implementation The intrinsic ones, then those added by managers (maybe).
	 * Must be reset before any resolution.
	 */
	private final Set<String> implementationConstraints = new HashSet<String>();
	private final Set<String> mngImplementationConstraints = new HashSet<String>();
	private final Set<ApamFilter> implementationConstraintFilters = new HashSet<ApamFilter>();
	private final Set<ApamFilter> mngImplementationConstraintFilters = new HashSet<ApamFilter>();
	private boolean isStaticImplemConstraintFilters;

	/**
	 * The set of constraints that must be satisfied by the target component
	 * instance
	 */
	private final Set<String> instanceConstraints = new HashSet<String>();
	private final Set<String> mngInstanceConstraints = new HashSet<String>();
	private final Set<ApamFilter> instanceConstraintFilters = new HashSet<ApamFilter>();
	private final Set<ApamFilter> mngInstanceConstraintFilters = new HashSet<ApamFilter>();
	private boolean isStaticInstConstraintFilters;

	/**
	 * The list of preferences to choose among candidate service provider
	 * implementation
	 */
	private final List<String> implementationPreferences = new ArrayList<String>();
	private final List<String> mngImplementationPreferences = new ArrayList<String>();
	private final List<ApamFilter> implementationPreferenceFilters = new ArrayList<ApamFilter>();
	private final List<ApamFilter> mngImplementationPreferenceFilters = new ArrayList<ApamFilter>();
	private boolean isStaticImplemPreferenceFilters = false;

	/**
	 * The list of preferences to choose among candidate service provider
	 * instances
	 */
	private final List<String> instancePreferences = new ArrayList<String>();
	private final List<String> mngInstancePreferences = new ArrayList<String>();
	private final List<ApamFilter> instancePreferenceFilters = new ArrayList<ApamFilter>();
	private final List<ApamFilter> mngInstancePreferenceFilters = new ArrayList<ApamFilter>();
	private boolean isStaticInstPreferenceFilters = false;

	// public static relation voidDep = new relationImpl () ;

	// Whether this relation is declared explicitly as multiple
	private final boolean isMultiple;

	// The policy to handle unresolved dependencies
	private final MissingPolicy missingPolicy;

	// The exception to throw for the exception missing policy
	private final String missingException;

	// Whether a relation matching this policy must be eagerly resolved
	private final boolean isEager;

	// Whether a resolution error must trigger a backtrack in the architecture
	private final boolean mustHide;

	// true if this is a dynamic relation : a field multiple, or a dynamic message
	private final boolean isDynamic;

	// If this is a Wire definion
	private final boolean isWire;

	private boolean isComputed = false;

	// this relation was created but never computed so far. bool isStaticxyz are
	// not set yet
	private boolean isInitialized = false;


	public boolean isRelation() {
		return !identifier.isEmpty();
	}

	public RelationImpl(ResolvableReference target, ComponentKind targetType, Set<String> constraints, List<String> preferences) {
		// The minimum info for a find.
		// this.component = component;
		this.identifier = "";
		this.targetDefinition = target;
		this.isMultiple = false;
		this.sourceType = ComponentKind.INSTANCE;
		this.targetType = (targetType == null) ? ComponentKind.INSTANCE : targetType;

		//		this.linkSource = component;
		//		this.sourceName = component.getName();
		isDynamic = false;
		isWire = false;
		isEager = false;
		mustHide = false;
		missingException = null;
		missingPolicy = null;

		if (constraints != null) {
			if (targetType == ComponentKind.IMPLEMENTATION)
				implementationConstraints.addAll(constraints);
			instanceConstraints.addAll(constraints);
		}
		if (preferences != null) {
			if (targetType == ComponentKind.IMPLEMENTATION)
				implementationPreferences.addAll(constraints);
			instancePreferences.addAll(preferences);
		}
	}


	/*
	 * Component can be null; in that case filters are not substituted.
	 */
	public RelationImpl(RelationDeclaration dep) {
		// Definition
		// this.component = component;
		this.targetDefinition = dep.getTarget();
		this.identifier = dep.getIdentifier();
		this.sourceType = (dep.getSourceType() == null) ? ComponentKind.INSTANCE : dep.getSourceType();
		this.targetType = (dep.getTargetType() == null) ? ComponentKind.INSTANCE : dep.getTargetType();
		// this.sourceName = (dep.getSource() == null) ? component.getName() :
		// dep.getSource().getName();

		// Flags
		this.isMultiple = dep.isMultiple();
		this.isEager = (dep.isEager() == null) ? false : dep.isEager();
		this.mustHide = (dep.isHide() == null) ? false : dep.isHide();
		this.missingPolicy = dep.getMissingPolicy();
		this.missingException = dep.getMissingException();

		// Constraints
		this.implementationConstraints.addAll(dep.getImplementationConstraints());
		this.instanceConstraints.addAll(dep.getInstanceConstraints());
		this.implementationPreferences.addAll(dep.getImplementationPreferences());
		this.instancePreferences.addAll(dep.getInstancePreferences());

		// computing isDynamic
		boolean hasField = false;
		for (RelationInjection injection : dep.getInjections()) {
			if (injection instanceof RelationInjection.Field) {
				hasField = true;
				break;
			}
		}
		isDynamic = (!hasField || dep.isMultiple() || dep.isEffectiveEager());

		isWire = (hasField
				&& dep.getTargetType() == ComponentKind.INSTANCE
				&& dep.getTarget() instanceof ResourceReference
				&& !dep.getTarget().getName().equals("fr.imag.adele.apam.Component")
				&& !dep.getTarget().getName().equals("fr.imag.adele.apam.Specification")
				&& !dep.getTarget().getName().equals("fr.imag.adele.apam.Implementation")
 && !dep.getTarget().getName().equals("fr.imag.adele.apam.Instance"));
	}

	private void initializeRelation(Component component) {
		// Check if there are substitutions, and build filters
		ApamFilter f;
		isStaticImplemConstraintFilters = true;
		isStaticInstConstraintFilters = true;
		isStaticImplemPreferenceFilters = true;
		isStaticInstPreferenceFilters = true;

		for (String c : implementationConstraints) {
			if (ApamFilter.isSubstituteFilter(c, component))
				isStaticImplemConstraintFilters = false;
			f = ApamFilter.newInstanceApam(c, component);
			if (f != null)
				implementationConstraintFilters.add(f);
		}

		for (String c : instanceConstraints) {
			if (ApamFilter.isSubstituteFilter(c, component))
				isStaticInstConstraintFilters = false;
			f = ApamFilter.newInstanceApam(c, component);
			if (f != null)
				instanceConstraintFilters.add(f);
		}

		for (String c : implementationPreferences) {
			if (ApamFilter.isSubstituteFilter(c, component))
				isStaticImplemPreferenceFilters = false;
			f = ApamFilter.newInstanceApam(c, component);
			if (f != null)
				implementationPreferenceFilters.add(f);
		}

		for (String c : instancePreferences) {
			if (ApamFilter.isSubstituteFilter(c, component))
				isStaticInstPreferenceFilters = false;
			f = ApamFilter.newInstanceApam(c, component);
			if (f != null)
				instancePreferenceFilters.add(f);
		}
	}
	/**
	 * Called after the managers have added their constraints, and before to try
	 * to resolve that relation. First it clears the previous filters (except if
	 * immutable), and recompute them from the string contraints Adds in mng the
	 * preferences and constraints filters
	 */
	public void computeFilters(Component linkSource) {

		this.linkSource = linkSource;

		if (!isInitialized) {
			initializeRelation(linkSource);
			isInitialized = true;
		}

		this.linkSource = linkSource;
		ApamFilter f;
		/*
		 * Manager constraints. Can be different for each resolution
		 */
		mngImplementationConstraintFilters.clear();
		for (String c : mngImplementationConstraints) {
			f = ApamFilter.newInstanceApam(c, linkSource);
			if (f != null)
				mngImplementationConstraintFilters.add(f);
		}
		mngImplementationConstraints.clear();

		mngInstanceConstraintFilters.clear();
		for (String c : mngInstanceConstraints) {
			f = ApamFilter.newInstanceApam(c, linkSource);
			if (f != null)
				mngInstanceConstraintFilters.add(f);
		}
		mngInstanceConstraints.clear();

		mngImplementationPreferenceFilters.clear();
		for (String c : mngImplementationPreferences) {
			f = ApamFilter.newInstanceApam(c, linkSource);
			if (f != null)
				mngImplementationPreferenceFilters.add(f);
		}
		mngImplementationPreferences.clear();

		mngInstancePreferenceFilters.clear();
		for (String c : mngInstancePreferences) {
			f = ApamFilter.newInstanceApam(c, linkSource);
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
				f = ApamFilter.newInstanceApam(c, linkSource);
				if (f != null)
					implementationConstraintFilters.add(f);
			}
		}

		if (!isStaticInstConstraintFilters) {
			instanceConstraintFilters.clear();
			for (String c : instanceConstraints) {
				f = ApamFilter.newInstanceApam(c, linkSource);
				if (f != null)
					instanceConstraintFilters.add(f);
			}
		}

		if (!isStaticImplemPreferenceFilters) {
			implementationPreferenceFilters.clear();
			for (String c : implementationPreferences) {
				f = ApamFilter.newInstanceApam(c, linkSource);
				if (f != null)
					implementationPreferenceFilters.add(f);
			}
		}

		if (!isStaticInstPreferenceFilters) {
			instancePreferenceFilters.clear();
			for (String c : instancePreferences) {
				f = ApamFilter.newInstanceApam(c, linkSource);
				if (f != null)
					instancePreferenceFilters.add(f);
			}
		}

		// concatenate preference lists
		mngImplementationPreferenceFilters.addAll(implementationPreferenceFilters);
		mngInstancePreferenceFilters.addAll(instancePreferenceFilters);

		// concatenate constraints lists
		mngImplementationConstraintFilters.addAll(implementationConstraintFilters);
		mngInstanceConstraintFilters.addAll(instanceConstraintFilters);

		isComputed = true;
	}

	protected void resetFilters() {
		isComputed = false;
	}


	/**
	 * return true if the component matches the constraints of that relation.
	 * Preferences are not taken into account
	 * 
	 * @param comp
	 * @return
	 */
	@Override
	public boolean matchRelationConstraints(Component comp) {
		return matchRelationConstraints(comp.getAllProperties());
	}

	@Override
	public boolean matchRelationConstraints(Map<String, Object> properties) {

		if (!isComputed) {
			logger.error("Filters not computed");
			return false;
		}

		if (getSourceKind() == ComponentKind.IMPLEMENTATION) {
			for (ApamFilter f : mngImplementationConstraintFilters) {
				if (!f.match0(properties))
					return false;
			}
			return true;
		}

		if (getSourceKind() == ComponentKind.INSTANCE) {
			for (ApamFilter f : mngInstanceConstraintFilters) {
				if (!f.match0(properties))
					return false;
			}
		}

		// TODO if it is a spec ...
		return true;
	}

	@Override
	public boolean matchRelation(Component target) {
		return (matchRelationConstraints(target) && matchRelationTarget(target));
	}

	@Override
	public boolean matchRelationTarget(Component target) {
		if (target.getKind() != getTargetKind())
			return false;
		if (!linkSource.canSee(target))
			return false;

		if (getTarget() instanceof ComponentReference<?>) {
			return target.getName().equals(getTarget().getName());
		}

		// if (dep.getTarget() instanceof ResourceReference) {
		return target.getDeclaration().getProvidedResources()
				.contains((ResourceReference) getTarget());
	}

	@Override
	public boolean isDynamic() {
		return isDynamic;
	}

	// @Override
	// public boolean isWire () {
	// return isWire ;
	// }

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Relation))
			return false;

		return this == object;
	}

	// @Override
	// public int hashCode() {
	// return reference.hashCode();
	// }


	/**
	 * Get the reference to the required resource
	 */
	public ResolvableReference getTarget() {
		return targetDefinition;
	}

	//	@Override
	//	public String getSource() {
	//		return sourceName;
	//	}

	@Override
	public Component getLinkSource() {
		return linkSource;
	}

	/**
	 * Get the constraints that need to be satisfied by the implementation that
	 * resolves the reference
	 */
	@Override
	public Set<String> getImplementationConstraints() {
		return Collections.unmodifiableSet(implementationConstraints);
	}

	// Get the constraints that need to be satisfied by the instance that
	// resolves the reference
	@Override
	public Set<String> getInstanceConstraints() {
		return Collections.unmodifiableSet(instanceConstraints);
	}

	// Get the resource provider preferences
	@Override
	public List<String> getImplementationPreferences() {
		return Collections.unmodifiableList(implementationPreferences);
	}

	// Get the instance provider preferences
	@Override
	public List<String> getInstancePreferences() {
		return Collections.unmodifiableList(instancePreferences);
	}

	// Modifiable
	@Override
	public Set<String> getMngImplementationConstraints() {
		return mngImplementationConstraints;
	}

	// Get the constraints that need to be satisfied by the instance that
	// resolves the reference
	@Override
	public Set<String> getMngInstanceConstraints() {
		return mngInstanceConstraints;
	}

	// Get the resource provider preferences
	@Override
	public List<String> getMngImplementationPreferences() {
		return mngImplementationPreferences;
	}

	// Get the instance provider preferences
	@Override
	public List<String> getMngInstancePreferences() {
		return mngInstancePreferences;
	}


	// The defining component
	// @Override
	// public Component getComponent() {
	// return component;
	// }

	// Get the id of the relation in the declaring component declaration
	@Override
	public String getIdentifier() {
		return identifier;
	}


	@Override
	public boolean isMultiple() {
		return isMultiple;
	}

	// Get the policy associated with this relation
	@Override
	public MissingPolicy getMissingPolicy() {
		return missingPolicy;
	}

	// Whether dependencies matching this contextual policy must be resolved
	// eagerly
	@Override
	public boolean isEager() {
		return isEager;
	}

	/**
	 * Whether an error resolving a relation matching this policy should trigger
	 * a backtrack in resolution
	 */
	@Override
	public boolean isHide() {
		return mustHide;
	}

	// Get the exception associated with the missing policy
	public String getMissingException() {
		return missingException;
	}

	@Override
	public List<ApamFilter> getImplementationPreferenceFilters() {
		return Collections.unmodifiableList(mngImplementationPreferenceFilters);
	}

	// Both list have been added after filter generation
	@Override
	public List<ApamFilter> getInstancePreferenceFilters() {
		return Collections.unmodifiableList(mngInstancePreferenceFilters);
	}




	@Override
	public ComponentKind getSourceKind() {
		return sourceType;
	}


	@Override
	public ComponentKind getTargetKind() {
		return targetType;
	}


	@Override
	public boolean hasConstraints() {
		return mngImplementationConstraintFilters != null
				|| mngInstanceConstraintFilters != null
				|| implementationConstraintFilters != null
				|| instanceConstraintFilters != null;
	}


	// Supposed to be called after filters have been computed and added
	@Override
	public Set<ApamFilter> getAllImplementationConstraintFilters() {
		return Collections.unmodifiableSet(mngImplementationConstraintFilters);
	}


	// Supposed to be called after filters have been computed and added
	@Override
	public Set<ApamFilter> getAllInstanceConstraintFilters() {
		return Collections.unmodifiableSet(mngInstanceConstraintFilters);
	}

	// ==== ex RelationUtil
	/**
	 * Return the sub-set of candidates that satisfy all the constraints and
	 * preferences. Suppose the candidates are of the right kind ! Visibility is
	 * checked is source is provided
	 * 
	 * @param <T>
	 * @param candidates
	 * @param constraints
	 * @return
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Resolved<?> getResolved(Set<? extends Component> candidates) {
		// if (dep == null) return new Resolved (candidates);
		if (candidates == null || candidates.isEmpty())
			return null;

		if (candidates.iterator().next().getKind() != getTargetKind()) {
			logger.error("Invalid type in getResolved");
			return null;
		}

		Set<Component> ret = new HashSet<Component>();
		for (Component c : candidates) {
			if (getLinkSource().canSee(c) && c.matchRelationConstraints(this)) {
				ret.add(c);
			}
		}

		if (ret.isEmpty())
			return null;

		if (isMultiple())
			return new Resolved(ret);

		// look for preferences
		return new Resolved(getPrefered(ret));
	}

	public Resolved<?> getResolved(Resolved<?> candidates) {
		if (candidates.singletonResolved != null) {
			if (candidates.singletonResolved.matchRelationConstraints(this))
				return candidates;
			return null;
		} else
			return getResolved(candidates.setResolved);
	}

	/**
	 * Return the candidates that best matches the preferences Take the
	 * preferences in orden: m candidates find the n candidates that match the
	 * constraint. if n= 0 ignore the constraint if n=1 return it. iterate with
	 * the n candidates. At the end, if n > 1 returns the default one.
	 * 
	 * @param <T>
	 * @param candidates
	 * @param preferences
	 * @return
	 */
	public <T extends Component> T getPrefered(Set<T> candidates) {
		if (candidates == null || candidates.isEmpty())
			return null;
		if (candidates.size() == 1)
			return candidates.iterator().next();

		if (candidates.iterator().next() instanceof Implementation) {
			return getPreferedFilter(candidates,
					getImplementationPreferenceFilters());
		}
		return getPreferedFilter(candidates, getInstancePreferenceFilters());
	}

	private static <T extends Component> T getPreferedFilter(Set<T> candidates,
			List<ApamFilter> preferences) {
		if (preferences.isEmpty())
			return getDefaultComponent(candidates);

		Set<T> valids = new HashSet<T>();
		for (ApamFilter f : preferences) {
			for (T compo : candidates) {
				if (compo.match(f))
					valids.add(compo);
			}

			// If a single one satisfies, it is the prefered one.
			if (valids.size() == 1)
				return valids.iterator().next();

			// If nobody satisfies the contraints check next constraint with
			// same set of candidates
			if (valids.isEmpty())
				break;

			// continue with those that satisfy the constraint
			candidates = valids;
			valids = new HashSet<T>();
		}

		// More than one candidate are still here: return the default one.
		return getDefaultComponent(candidates);
	}

	/**
	 * Return the "best" component among the candidates. Best depends on the
	 * component nature. For implems, it is those that have sharable instance or
	 * that is instantiable.
	 * 
	 * @param <T>
	 * @param candidates
	 * @return
	 */
	private static <T extends Component> T getDefaultComponent(Set<T> candidates) {
		if (candidates == null || candidates.isEmpty())
			return null;
		if (!(candidates.iterator().next() instanceof Implementation))
			return candidates.iterator().next();

		for (T impl : candidates) {
			if (impl.isInstantiable())
				return impl;
			for (Component inst : impl.getMembers()) {
				if (((Instance) inst).isSharable())
					return impl;
			}
		}
		return candidates.iterator().next();
	}

	/**
	 * Provided a component, compute its effective relations, adding group
	 * constraint and flags. It is supposed to be correct !! No failure expected
	 * 
	 * Does not add those dependencies defined "above" nor the composite ones.
	 * 
	 */
	protected static Map<String, Relation> initializeDependencies(Component client) {
		Map<String, Relation> relations = new HashMap<String, Relation>();
		for (RelationDeclaration relDef : client.getDeclaration().getDependencies()) {
			//relations.add (relation.getIdentifier(),relation)) ;
			Component group = client.getGroup();
			// look for that relation declaration above
			RelationDeclaration groupDep = null;
			while (group != null && (groupDep == null)) {
				groupDep = group.getDeclaration().getRelation(relDef.getIdentifier());
				group = group.getGroup();
			}

			if (groupDep != null) {
				// it is declared above. Merge and check.
				// First merge flags, and then constraints.
				Util.overrideDepFlags(relDef, groupDep, false);
				relDef.getImplementationConstraints().addAll(groupDep.getImplementationConstraints());
				relDef.getInstanceConstraints().addAll(groupDep.getInstanceConstraints());
				relDef.getImplementationPreferences().addAll(groupDep.getImplementationPreferences());
				relDef.getInstancePreferences().addAll(groupDep.getInstancePreferences());

				// It is supposed that the compilation checked that the targets
				// are compatible
				// relation.setTarget(groupDep.getTarget()) ;
			}

			// Add the override relation : flags and constraints. Only for
			// source instances
			if (client instanceof Instance) {
				List<RelationDeclaration> overDeps = ((Instance) client).getComposite().getCompType().getCompoDeclaration().getOverridenDependencies();
				if (overDeps != null && !overDeps.isEmpty()) {
					for (RelationDeclaration overDep : overDeps) {
						if (Util.matchOverrideRelation((Instance) client, overDep, relDef)) {
							Util.overrideDepFlags(relDef, overDep, true);
							// It is assumed that the filters have been checked
							// at compile time (checkObr)
							relDef.getImplementationConstraints().addAll(overDep.getImplementationConstraints());
							relDef.getInstanceConstraints().addAll(overDep.getInstanceConstraints());
							relDef.getImplementationPreferences().addAll(overDep.getImplementationPreferences());
							relDef.getInstancePreferences().addAll(overDep.getInstancePreferences());
						}
					}
				}
			}

			// Build the corresponding relation
			relations.put(relDef.getIdentifier(), new RelationImpl(relDef));
		}
		return relations;
	}

	/**
	 * Provided a client instance, checks if its relation "clientDep", matches
	 * another relation: "compoDep".
	 * 
	 * matches only based on same name (same resource or same component). If
	 * client cardinality is multiple, compo cardinallity must be multiple too.
	 * No provision for the client constraints or characteristics (missing,
	 * eager)
	 * 
	 * @param compoInst
	 *            the composite instance containing the client
	 * @param compoDep
	 *            the relation that matches or not
	 * @param clientDep
	 *            the client relation we are trying to resolve
	 * @return
	 */
	public boolean matchRelation(Instance compoInst, Relation compoDep) {
		if (compoDep == null)
			return false;

		if (compoDep.getTargetKind() != getTargetKind())
			return false;

		if (compoDep.getSourceKind() != getSourceKind())
			return false;

		// Look for same relation: the same specification, the same
		// implementation or same resource name
		// Constraints are not taken into account
		boolean multiple = isMultiple();

		// if same nature (spec, implem, internface ... make a direct
		// comparison.
		if (compoDep.getTarget().getClass().equals(getTarget().getClass())) {
			if (compoDep.getTarget().equals(getTarget())) {
				if (!multiple || compoDep.isMultiple()) {
					return true;
				}
			}
		}

		// Look for a compatible relation.
		// Stop at the first relation matching only based on same name (same
		// resource or same component)
		// No provision for : cardinality, constraints or characteristics
		// (missing, eager)

		// Look if the client requires one of the resources provided by the
		// specification
		if (compoDep.getTarget() instanceof SpecificationReference) {
			Specification spec = CST.apamResolver.findSpecByName(compoInst,
					((SpecificationReference) compoDep.getTarget()).getName());
			if ((spec != null)
					&& spec.getDeclaration().getProvidedResources()
.contains(getTarget())
					&& (!multiple || compoDep.isMultiple())) {
				return true;
			}
		}

		// If the composite has a relation toward an implementation
		// and the client requires a resource provided by that implementation
		else {
			if (compoDep.getTarget() instanceof ImplementationReference) {
				String implName = ((ImplementationReference<?>) compoDep
						.getTarget()).getName();
				Implementation impl = CST.apamResolver.findImplByName(
						compoInst, implName);
				if (impl != null) {
					// The client requires the specification implemented by that
					// implementation
					if (getTarget() instanceof SpecificationReference) {
						String clientReqSpec = ((SpecificationReference) getTarget())
								.getName();
						if (impl.getImplDeclaration().getSpecification()
								.getName().equals(clientReqSpec)
								&& (!multiple || compoDep.isMultiple())) {
							return true;
						}
					} else {
						// The client requires a resource provided by that
						// implementation
						if (impl.getImplDeclaration().getProvidedResources()
								.contains(getTarget())
								&& (!multiple || compoDep.isMultiple())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append("resolving ");

		if (isRelation())
			ret.append("relation " + getIdentifier() + " towards ");

		if (getTarget() instanceof ComponentReference<?>)
			ret.append(getTargetKind() + " providing " + getTarget().getName());
		else
			ret.append(getTarget());

		ret.append(" from " + linkSource);

		if (!implementationConstraintFilters.isEmpty()) {
			ret.append("\n         Implementation Constraints");
			for (ApamFilter inj : implementationConstraintFilters) {
				ret.append("\n            " + inj);
			}
		}
		if (!instanceConstraintFilters.isEmpty()) {
			ret.append("\n         Instance Constraints");
			for (ApamFilter inj : instanceConstraintFilters) {
				ret.append("\n            " + inj);
			}
		}
		if (!implementationPreferenceFilters.isEmpty()) {
			ret.append("\n         Implementation Preferences");
			for (ApamFilter inj : implementationPreferenceFilters) {
				ret.append("\n            " + inj);
			}
		}
		if (!instancePreferenceFilters.isEmpty()) {
			ret.append("\n         Instance Preferences");
			for (ApamFilter inj : instancePreferenceFilters) {
				ret.append("\n            " + inj);
			}
		}
		return ret.toString();
	}

}
