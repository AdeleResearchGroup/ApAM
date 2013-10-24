package fr.imag.adele.apam.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.RelationDefinition;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.CreationPolicy;
import fr.imag.adele.apam.declarations.MissingPolicy;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.ResolvePolicy;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.util.ApamFilter;


public class RelToResolveImpl implements RelToResolve {

	static Logger logger = LoggerFactory.getLogger(ApamResolverImpl.class);

	// The actual source of this relation. Set just before a resolution
	private final Component linkSource;

	//The associated relation definition.
	private final RelationDefinition relationDefinition ;

	// this relation was created but never computed so far. bool isStaticxyz are not set yet
	private boolean isInitialized = false;


	/**
	 * The set of constraints that must be satisfied by the target component implementation
	 * The intrinsic ones, then those added by managers (maybe).
	 */
	private  Set<String> mngImplementationConstraints = new HashSet<String>();
	private  Set<ApamFilter> allImplementationConstraintFilters = new HashSet<ApamFilter>();

	/**
	 * The set of constraints that must be satisfied by the target component instance
	 */
	private  Set<String> mngInstanceConstraints = new HashSet<String>();
	private  Set<ApamFilter> allInstanceConstraintFilters = new HashSet<ApamFilter>();

	/**
	 * The list of preferences to choose among candidate service provider implementation
	 */
	private  List<String> mngImplementationPreferences = new ArrayList<String>();
	private  List<ApamFilter> allImplementationPreferenceFilters = new ArrayList<ApamFilter>();

	/**
	 * The list of preferences to choose among candidate service provider instances
	 */
	private  List<String> mngInstancePreferences = new ArrayList<String>();
	private  List<ApamFilter> allInstancePreferenceFilters = new ArrayList<ApamFilter>();


	public RelToResolveImpl(Component source, ResolvableReference target, ComponentKind sourceKind, ComponentKind targetKind, Set<String> constraints, List<String> preferences) {
		// The minimum info for a find.
		relationDefinition = new RelationDefinitionImpl (target, sourceKind, targetKind, constraints, preferences) ;
		linkSource = source ;
	}


	/*
	 * Component can be null; in that case filters are not substituted.
	 */
	public RelToResolveImpl(Component source, RelationDeclaration declaration) {
		relationDefinition = new RelationDefinitionImpl(declaration) ; 
		linkSource = source ;
	}

	public RelToResolveImpl(Component source, RelationDefinition relation) {
		relationDefinition = relation ;
		linkSource = source ;
	}



	public boolean isRelation() {
		return !relationDefinition.getName().isEmpty();
	}


	public RelationDeclaration getDeclaration() {
		return ((RelationDefinitionImpl)relationDefinition).getDeclaration();
	}

	/**
	 * Get the effective result of refining this relation by the specified partial declaration
	 */
	public RelationDeclaration refinedBy(RelationDeclaration refinement) {
		return getDeclaration() != null ? getDeclaration().refinedBy(refinement) : refinement;
	}


	/**
	 * Called after the managers have added their constraints, and before to try
	 * to resolve that relation. First it clears the previous filters (except if
	 * immutable), and recompute them from the string contraints Adds in mng the
	 * preferences and constraints filters
	 */
	public void computeFilters() {

		isInitialized = true;
		ApamFilter f;

		for (String c : mngImplementationConstraints) {
			f = ApamFilter.newInstanceApam(c, linkSource);
			if (f != null)
				allImplementationConstraintFilters.add(f);
		}
		//mngImplementationConstraints = null ;

		for (String c : mngInstanceConstraints) {
			f = ApamFilter.newInstanceApam(c, linkSource);
			if (f != null)
				allInstanceConstraintFilters.add(f);
		}
		//mngInstanceConstraints.clear();

		for (String c : mngImplementationPreferences) {
			f = ApamFilter.newInstanceApam(c, linkSource);
			if (f != null)
				allImplementationPreferenceFilters.add(f);
		}
		//mngImplementationPreferences.clear();

		for (String c : mngInstancePreferences) {
			f = ApamFilter.newInstanceApam(c, linkSource);
			if (f != null)
				allInstancePreferenceFilters.add(f);
		}
		//mngInstancePreferences.clear();


		/*
		 * intrinsic constraints. To recompute only if they have substitutions.
		 */
		if (!getImplementationConstraints().isEmpty()) {
			if (!((RelationDefinitionImpl)relationDefinition).isStaticImplemConstraints()) {
				for (String c : relationDefinition.getImplementationConstraints()) {
					f = ApamFilter.newInstanceApam(c, linkSource);
					if (f != null)
						allImplementationConstraintFilters.add(f);
				}
			} 
			else allImplementationConstraintFilters.addAll(((RelationDefinitionImpl)relationDefinition).getImplementationConstraintFilters());
		}

		if (!getInstanceConstraints().isEmpty()) {
			if (!((RelationDefinitionImpl)relationDefinition).isStaticInstConstraints()) {
				for (String c : relationDefinition.getInstanceConstraints()) {
					f = ApamFilter.newInstanceApam(c, linkSource);
					if (f != null)
						allInstanceConstraintFilters.add(f);
				}
			}
			else allInstanceConstraintFilters.addAll(((RelationDefinitionImpl)relationDefinition).getInstanceConstraintFilters());
		}

		if (!getImplementationPreferences().isEmpty()) {
			if (!((RelationDefinitionImpl)relationDefinition).isStaticImplemPreferences()) {
				for (String c : relationDefinition.getImplementationPreferences()) {
					f = ApamFilter.newInstanceApam(c, linkSource);
					if (f != null)
						allImplementationPreferenceFilters.add(f);
				}
			}
			else allImplementationPreferenceFilters.addAll(((RelationDefinitionImpl)relationDefinition).getImplementationpreferencfeFilters());
		}

		if (!getInstancePreferences().isEmpty()) {
			if (!((RelationDefinitionImpl)relationDefinition).isStaticInstPreferences()) {
				for (String c : relationDefinition.getInstancePreferences()) {
					f = ApamFilter.newInstanceApam(c, linkSource);
					if (f != null)
						allInstancePreferenceFilters.add(f);
				}
			}
			else allInstancePreferenceFilters.addAll(((RelationDefinitionImpl)relationDefinition).getInstancePreferenceFilters());
		}

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
		return matchRelationConstraints(comp.getKind(), comp.getAllProperties());
	}

	@Override
	public boolean matchRelationConstraints(ComponentKind candidateKind, Map<String, Object> properties) {

		if (!isInitialized) {
			computeFilters() ;
		}

		//Instance must match both implementation and instance constraints ???
		switch (candidateKind) {
		case INSTANCE:
			for (ApamFilter f : allInstanceConstraintFilters) {
				if (!f.match(properties))
					return false;
			}
		case IMPLEMENTATION:
			for (ApamFilter f : allImplementationConstraintFilters) {
				if (!f.match(properties))
					return false;
			}
		case SPECIFICATION:
		case COMPONENT:
		}

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

		/*
		 * target definition is a specification, an implementation or instance by name
		 */
		if (getTarget() instanceof ComponentReference<?>) {
			Component group = target ;
			while (group != null) {
				if (group.getName().equals(getTarget().getName()))
					return true ;
				group = group.getGroup() ;
			}
			return false;
		}

		// if (dep.getTarget() instanceof ResourceReference) {
		return target.getDeclaration().getProvidedResources()
				.contains((ResourceReference) getTarget());
	}

	@Override
	public boolean isDynamic() {
		return relationDefinition.isDynamic() ;
	}

	@Override
	public boolean isWire() {
		return relationDefinition.isWire();
	}

	@Override
	public boolean isInjected() {
		return relationDefinition.isInjected();
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof RelToResolve))
			return false;

		return this == object;
	}

	/**
	 * Get the reference to the required resource
	 */
	public ResolvableReference getTarget() {
		return relationDefinition.getTarget();
	}

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
		return relationDefinition.getImplementationConstraints();
	}

	// Get the constraints that need to be satisfied by the instance that
	// resolves the reference
	@Override
	public Set<String> getInstanceConstraints() {
		return relationDefinition.getInstanceConstraints();
	}

	// Get the resource provider preferences
	@Override
	public List<String> getImplementationPreferences() {
		return relationDefinition.getImplementationPreferences();
	}

	// Get the instance provider preferences
	@Override
	public List<String> getInstancePreferences() {
		return relationDefinition.getInstancePreferences();
	}

	/*
	 *  Modifiable
	 * @see fr.imag.adele.apam.Relation#getMngImplementationConstraints()
	 */
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


	// Get the id of the relation in the declaring component declaration
	@Override
	public String getName() {
		return relationDefinition.getName();
	}


	@Override
	public boolean isMultiple() {
		return relationDefinition.isMultiple();
	}

	// Get the policy associated with this relation
	@Override
	public MissingPolicy getMissingPolicy() {
		return relationDefinition.getMissingPolicy();
	}

	@Override
	public CreationPolicy getCreation() {
		return relationDefinition.getCreation();
	}

	@Override
	public ResolvePolicy getResolve() {
		return relationDefinition.getResolve();
	}

	/**
	 * Whether an error resolving a relation matching this policy should trigger
	 * a backtrack in resolution
	 */
	@Override
	public boolean isHide() {
		return relationDefinition.isHide() ;
	}

	// Get the exception associated with the missing policy
	public String getMissingException() {
		return relationDefinition.getMissingException();
	}

	@Override
	public List<ApamFilter> getImplementationPreferenceFilters() {
		return Collections.unmodifiableList(allImplementationPreferenceFilters);
	}

	// Both list have been added after filter generation
	@Override
	public List<ApamFilter> getInstancePreferenceFilters() {
		return Collections.unmodifiableList(allInstancePreferenceFilters);
	}


	@Override
	public ComponentKind getSourceKind() {
		return relationDefinition.getSourceKind();
	}


	@Override
	public ComponentKind getTargetKind() {
		return relationDefinition.getTargetKind();
	}


	@Override
	public boolean hasConstraints() {
		return !allImplementationConstraintFilters.isEmpty()
				|| !allInstanceConstraintFilters.isEmpty() ;
	}


	// Supposed to be called after filters have been computed and added
	@Override
	public Set<ApamFilter> getAllImplementationConstraintFilters() {
		return Collections.unmodifiableSet(allImplementationConstraintFilters);
	}


	// Supposed to be called after filters have been computed and added
	@Override
	public Set<ApamFilter> getAllInstanceConstraintFilters() {
		return Collections.unmodifiableSet(allInstanceConstraintFilters);
	}


	/*
	 * return the component corresponding to the sourceKind.
	 */
	@Override
	public Component getRelSource (Component base) {
		Component source = base ;
		while (source != null) {
			if (source.getKind() == getSourceKind()) {
				return source ;
			}
			source = source.getGroup() ;
		}
		return null ;
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
	public Resolved<?> getResolved(Set<? extends Component> candidates, boolean isPromotion) {

		if (candidates == null || candidates.isEmpty())
			return null;

		if (candidates.iterator().next().getKind() != getTargetKind()) {
			logger.error("Invalid type in getResolved");
			return null;
		}

		Set<Component> ret = new HashSet<Component>();
		if (isPromotion) { 
			//In case of promotion we do no check the visibility (source is inside the composite, candidate outside; only the constraints
			for (Component c : candidates) {
				if (c.matchRelationConstraints(this)) {
					ret.add(c);
				}
			}
		} else {
			for (Component c : candidates) {
				if (getLinkSource().canSee(c) && c.matchRelationConstraints(this)) {
					ret.add(c);
				}
			}
		}

		if (ret.isEmpty())
			return null;

		if (isMultiple())
			return new Resolved(ret);

		// look for preferences
		return new Resolved(getPrefered(ret));
	}

	public Resolved<?> getResolved(Resolved<?> candidates, boolean isPromotion) {
		if (candidates.singletonResolved != null) {
			if (candidates.singletonResolved.matchRelationConstraints(this))
				return candidates;
			return null;
		} else
			return getResolved(candidates.setResolved, isPromotion);
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



	public String toString() {
		StringBuffer ret = new StringBuffer();
		//ret.append("resolving ");

		if (isRelation())
			ret.append("relation " + getName() + " towards ");

		if (relationDefinition.isMultiple())
			ret.append("multiple ");

		ret.append(getTargetKind()) ;

		if (getTarget() instanceof ComponentReference<?>)
			ret.append( " of" + getTarget());
		else
			ret.append(" providing " + getTarget());

		ret.append(" from " + linkSource);
		ret.append(" (creation = "+ relationDefinition.getCreation() +", resolve = "+ relationDefinition.getResolve() +", missing policy = "+ relationDefinition.getMissingPolicy() +")");

		if (!allImplementationConstraintFilters.isEmpty()) {
			ret.append("\n         Implementation Constraints");
			for (ApamFilter inj : allImplementationConstraintFilters) {
				ret.append("\n            " + inj);
				//				if (!implementationConstraintFilters.contains(inj))
				//					ret.append("[added by Manager]");
			}
		}
		if (!allInstanceConstraintFilters.isEmpty()) {
			ret.append("\n         Instance Constraints");
			for (ApamFilter inj : allInstanceConstraintFilters) {
				ret.append("\n            " + inj);
				//				if (!instanceConstraintFilters.contains(inj))
				//					ret.append("[added by Manager]");
			}
		}
		if (!allImplementationPreferenceFilters.isEmpty()) {
			ret.append("\n         Implementation Preferences");
			for (ApamFilter inj : allImplementationPreferenceFilters) {
				ret.append("\n            " + inj);
				//				if (!implementationPreferenceFilters.contains(inj))
				//					ret.append("[added by Manager]");
			}
		}
		if (!allInstancePreferenceFilters.isEmpty()) {
			ret.append("\n         Instance Preferences");
			for (ApamFilter inj : allInstancePreferenceFilters) {
				ret.append("\n            " + inj);
				//				if (!instancePreferenceFilters.contains(inj))
				//					ret.append("[added by Manager]");
			}
		}
		return ret.toString();
	}

	@Override
	public boolean hasPreferences () {
		return !allImplementationPreferenceFilters.isEmpty() 
				|| !allInstancePreferenceFilters.isEmpty() ; 
	}

	@Override
	public RelationDefinition getRelationDefinition() {
		return relationDefinition;
	}
}
