package fr.imag.adele.apam.impl;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.RelationDefinition;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.CreationPolicy;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.MissingPolicy;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.RequirerInstrumentation;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.ResolvePolicy;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.util.ApamFilter;


public class RelationDefinitionImpl implements RelationDefinition {

	static Logger logger = LoggerFactory.getLogger(ApamResolverImpl.class);

	/**
	 * The effective declaration used to build this relation, 
	 */
	private final RelationDeclaration declaration;

	// Relationship name
	private final String identifier;

	// Target definition (resource or component reference)
	private final ResolvableReference targetDefinition;

	// Source type for this relation (Spec, implem, instance)
	private final ComponentKind sourceKind;

	// Target type for this relation (Spec, implem, instance)
	private final ComponentKind targetKind;

	//The name of an ancestor of the link source. Contextual relation definition only. Null otherwise.
	private final String ctxtSourceName ;


	// TODO // The reference to the associated component ??
	// private final Component component;

	/**
	 * The set of constraints that must be satisfied by the target component implementation.
	 * Filters are set only if static (i.e. there is no substitutions into filters)
	 */
	private final Set<String> implementationConstraints = new HashSet<String>();
	private  Set<ApamFilter> implementationConstraintFilters ; //= new HashSet<ApamFilter>();
	private boolean isStaticImplemConstraints;

	/**
	 * The set of constraints that must be satisfied by the target component instance
	 */
	private final Set<String> instanceConstraints = new HashSet<String>();
	private  Set<ApamFilter> instanceConstraintFilters ; //= new HashSet<ApamFilter>();
	private boolean isStaticInstConstraints;

	/**
	 * The list of preferences to choose among candidate service provider implementation
	 */
	private final List<String> implementationPreferences = new ArrayList<String>();
	private  List<ApamFilter> implementationPreferenceFilters ; //= new ArrayList<ApamFilter>();
	private boolean isStaticImplemPreferences = false;

	/**
	 * The list of preferences to choose among candidate service provider instances
	 */
	private final List<String> instancePreferences = new ArrayList<String>();
	private  List<ApamFilter> instancePreferenceFilters ; //= new ArrayList<ApamFilter>();
	private boolean isStaticInstPreferences = false;



	// Whether this relation is declared explicitly as multiple
	private final boolean isMultiple;

	private final CreationPolicy create;

	private final ResolvePolicy resolve;

	// The policy to handle unresolved dependencies
	private final MissingPolicy missingPolicy;

	// The exception to throw for the exception missing policy
	private final String missingException;

	// Whether a resolution error must trigger a backtrack in the architecture
	private final boolean mustHide;

	// true if this is a dynamic relation : a field multiple, or a dynamic message
	private final boolean isDynamic;

	// If this is a Wire definition. Can be overloaded. Null if unknown.
	private boolean isWire;

	//Injected. Can be overloaded. Null if unknown.
	private boolean isInjected;


	public boolean isRelation() {
		return !identifier.isEmpty();
	}

	public RelationDefinitionImpl(ResolvableReference target, ComponentKind sourceKind, ComponentKind targetKind, Set<String> constraints, List<String> preferences) {
		// The minimum info for a find.
		this.declaration = null;
		this.identifier = "";
		this.targetDefinition = target;
		this.isMultiple = false;
		this.create	= CreationPolicy.LAZY;
		this.resolve = ResolvePolicy.EXTERNAL;
		this.sourceKind = (sourceKind == null) ? ComponentKind.INSTANCE : sourceKind ;
		this.targetKind = (targetKind == null) ? ComponentKind.INSTANCE : targetKind;
		this.ctxtSourceName = null;

		isDynamic = false;
		isWire = false;
		isInjected = false;
		mustHide = false;
		missingException = null;
		missingPolicy = null;

		if (constraints != null) {
			if (targetKind == ComponentKind.IMPLEMENTATION)
				implementationConstraints.addAll(constraints);
			instanceConstraints.addAll(constraints);
		}
		if (preferences != null) {
			if (targetKind == ComponentKind.IMPLEMENTATION)
				implementationPreferences.addAll(constraints);
			instancePreferences.addAll(preferences);
		}

		//Check constraint syntax and compute filter and booleans isStaticxxxx
		initializeRelation() ;
	}


	/*
	 * Component can be null; in that case filters are not substituted.
	 */
	public RelationDefinitionImpl(RelationDeclaration declaration) {

		this.declaration		= declaration;
		this.identifier 		= declaration.getIdentifier();
		this.sourceKind 		= (declaration.getSourceKind() == null) ? ComponentKind.INSTANCE : declaration.getSourceKind();
		this.targetKind 		= (declaration.getTargetKind() == null) ? ComponentKind.INSTANCE : declaration.getTargetKind();
		this.targetDefinition	= declaration.getTarget();
		this.ctxtSourceName		= declaration.getSourceName() ;

		// computing isDynamic, isWire, hasField.
		// NOTE the relation declaration is already refined and overridden so
		// we have access to all the information from all levels above
		this.isWire = false;
		this.isInjected = false;

		boolean hasCallbacks = false;

		for (RequirerInstrumentation injection : declaration.getInstrumentations()) {

			if (injection instanceof RequirerInstrumentation.MessageConsumerCallback)
				hasCallbacks = true;

			if (injection instanceof RequirerInstrumentation.RequiredServiceField) {
				this.isInjected = true;
				if (((RequirerInstrumentation.RequiredServiceField) injection).isWire())
					this.isWire = true;
			}
		}

		// Flags
		this.isMultiple 		= declaration.isMultiple();
		if (declaration.getCreationPolicy() == null) 
			this.create = hasCallbacks ? CreationPolicy.EAGER : CreationPolicy.LAZY;
		else
			this.create = declaration.getCreationPolicy();

		this.resolve			= declaration.getResolvePolicy() == null ? ResolvePolicy.EXTERNAL : declaration.getResolvePolicy();
		this.missingPolicy 		= declaration.getMissingPolicy();
		this.missingException 	= declaration.getMissingException();

		this.mustHide 	= (declaration.isHide() == null) ? false : declaration.isHide();
		this.isDynamic	= declaration.isMultiple() || (this.create == CreationPolicy.EAGER);

		// Constraints
		this.implementationConstraints.addAll(declaration.getImplementationConstraints());
		this.instanceConstraints.addAll(declaration.getInstanceConstraints());
		this.implementationPreferences.addAll(declaration.getImplementationPreferences());
		this.instancePreferences.addAll(declaration.getInstancePreferences());

		//Check constraint syntax and compute booleans isStaticxxxx
		initializeRelation() ;
	}

	public RelationDeclaration getDeclaration() {
		return declaration;
	}

	/**
	 * Get the effective result of refining this relation by the specified partial declaration
	 */
	public RelationDeclaration refinedBy(RelationDeclaration refinement) {
		return this.declaration != null ? this.declaration.refinedBy(refinement) : refinement;
	}

	/**
	 * To be called before any use of this relation.
	 * 
	 * @param component the component source on which is defined this relation
	 */
	private void initializeRelation() {
		// Check if there are substitutions, and build filters
		ApamFilter f;
		isStaticImplemConstraints = true ;
		isStaticInstConstraints = true;
		isStaticImplemPreferences = true;
		isStaticInstPreferences = true;

		for (String c : implementationConstraints) {
			implementationConstraintFilters = new HashSet<ApamFilter> () ;
			if (ApamFilter.isSubstituteFilter(c, null)) {
				isStaticImplemConstraints = false ;
				implementationConstraintFilters = null ;
				break ;
			}
			f = ApamFilter.newInstanceApam(c, null);
			if (f != null)
				implementationConstraintFilters.add(f) ;
		}

		for (String c : instanceConstraints) {
			instanceConstraintFilters = new HashSet<ApamFilter> () ;
			if (ApamFilter.isSubstituteFilter(c, null)) {
				isStaticInstConstraints = false ;
				instanceConstraintFilters = null ;
				break ;
			}
			f = ApamFilter.newInstanceApam(c, null);
			if (f != null)
				instanceConstraintFilters.add(f);
		}

		for (String c : implementationPreferences) {
			implementationPreferenceFilters =  new ArrayList<ApamFilter> () ;
			if (ApamFilter.isSubstituteFilter(c, null)) {
				isStaticImplemPreferences = false ;
				implementationPreferenceFilters = null ;
				break ;
			}
			f = ApamFilter.newInstanceApam(c, null);
			if (f != null)
				implementationPreferenceFilters.add(f);
		}

		for (String c : instancePreferences) {
			instancePreferenceFilters = new ArrayList<ApamFilter> () ;
			if (ApamFilter.isSubstituteFilter(c, null)) {
				isStaticInstPreferences = false ;
				instancePreferenceFilters = null ;
				break ;
			}
			f = ApamFilter.newInstanceApam(c, null);
			if (f != null)
				instancePreferenceFilters.add(f);
		}
	}

	public Set<ApamFilter> getImplementationConstraintFilters () {
		return Collections.unmodifiableSet(implementationConstraintFilters) ;
	}

	public Set<ApamFilter> getInstanceConstraintFilters () {
		return Collections.unmodifiableSet(instanceConstraintFilters) ;
	}
	public List<ApamFilter> getImplementationpreferencfeFilters () {
		return Collections.unmodifiableList(implementationPreferenceFilters) ;
	}
	public List<ApamFilter> getInstancePreferenceFilters () {
		return Collections.unmodifiableList(instancePreferenceFilters) ;
	}

	public boolean isStaticImplemConstraints () {
		return isStaticImplemConstraints ;
	}
	public boolean isStaticInstConstraints () {
		return isStaticInstConstraints ;
	}
	public boolean isStaticImplemPreferences () {
		return isStaticImplemPreferences ;
	}
	public boolean isStaticInstPreferences () {
		return isStaticInstPreferences ;
	}


	@Override
	public boolean isDynamic() {
		return isDynamic;
	}

	@Override
	public boolean isWire() {
		return isWire;
	}

	@Override
	public boolean isInjected() {
		return isInjected;
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof RelationDefinition))
			return false;

		return this == object;
	}


	/**
	 * Get the reference to the required resource
	 */
	public ResolvableReference getTarget() {
		return targetDefinition;
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


	// Get the id of the relation in the declaring component declaration
	@Override
	public String getName() {
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

	@Override
	public CreationPolicy getCreation() {
		return create;
	}

	@Override
	public ResolvePolicy getResolve() {
		return resolve;
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
	public ComponentKind getSourceKind() {
		return sourceKind;
	}


	@Override
	public ComponentKind getTargetKind() {
		return targetKind;
	}


	@Override
	public boolean hasConstraints() {
		return 	!implementationConstraints.isEmpty()
				|| !instanceConstraints.isEmpty();
	}

	//			return !mngImplementationConstraintFilters.isEmpty()
	//					|| !mngInstanceConstraintFilters.isEmpty()
	//					|| !implementationConstraintFilters.isEmpty()
	//					|| !instanceConstraintFilters.isEmpty();



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
	public boolean matchRelation(Instance compoInst, RelationDefinition compoDep) {
		//		RelToResolve rel = new RelToResolveImpl (inst, relDef) ;
		//		return rel.matchRelation(inst) ;
		//	}
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

	//	public static LinkImpl createLink ()

	public String toString() {
		StringBuffer ret = new StringBuffer();
		//ret.append("resolving ");

		if (isRelation())
			ret.append("relation " + getName() + " towards ");

		if (isMultiple)
			ret.append("multiple ");

		ret.append(getTargetKind()) ;

		if (getTarget() instanceof ComponentReference<?>)
			ret.append( " of" + getTarget());
		else
			ret.append(" providing " + getTarget());

		//ret.append(" from " + linkSource);
		ret.append(" (creation = "+create+", resolve = "+resolve+", missing policy = "+this.missingPolicy+")");

		return ret.toString();
	}

	@Override
	public String getCtxtSourceName() {

		return ctxtSourceName ;
	}

}
