/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.declarations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.declarations.instrumentation.CallbackDeclaration;
import fr.imag.adele.apam.declarations.references.ResolvableReference;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.resources.InterfaceReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;

/**
 * This class represents the declaration of a required resources needed by a
 * component, that will be resolved at runtime by APAM.
 * 
 * @author vega
 * 
 */
public class RelationDeclaration extends ConstrainedReference {

	/**
	 * The events associated to the runtime life-cycle of the relation
	 */
	public enum Event {
		BIND, UNBIND
	}

	/**
	 * A reference to a relation declaration. Notice that relation identifiers
	 * must be only unique in the context of their defining component
	 * declaration.
	 */
	public static class Reference extends FeatureReference {

		public Reference(ComponentReference<?> definingComponent, String identifier) {
			super(definingComponent, identifier);
		}

	}

	/**
	 * The reference to this declaration
	 */
	private final Reference reference;

	/**
	 * The level of abstraction where this relation can be instantiated
	 */
	private final ComponentKind sourceKind;

	/**
	 * The level of abstraction of the target of the relation
	 */
	private final ComponentKind targetKind;

	/**
	 * Whether this relation is declared explicitly as multiple
	 */
	private final boolean isMultiple;

	/**
	 * The policy to handle unresolved dependencies
	 */

	private final MissingPolicy missingPolicy;

	/**
	 * The exception to throw for the exception missing policy
	 * 
	 */
	private final String missingException;

	/**
	 * The list of instrumentation that need to be performed in the source
	 * primitive component to implement the semantics of this relation at
	 * runtime.
	 */
	protected final List<RequirerInstrumentation> instrumentations;

	/**
	 * The map of list of call back methods associated to the relation lifecycle
	 */
	protected final Map<Event, Set<CallbackDeclaration>> callbacks;


	/**
	 * The resolution space to consider when resolving this relation
	 */
	private final ResolvePolicy resolvePolicy;
	
	/**
	 * The time at which resolution must be done
	 */
	private final CreationPolicy creationPolicy;

	/**
	 * Whether a resolution error must trigger a backtrack in the architecture
	 */

	private final Boolean mustHide;
	
	/**
	 * Whether this is a contextual relation
	 */
	private final boolean isContextual;
	
	/**
	 * The source component for this declaration in the case of contextual
	 * dependencies
	 */
	private final String matchSource;

	/**
	 * Whether this contextual relation is declared explicitly as an override
	 */
	private final boolean isOverride;

	/**
	 * Declares a new relation with the default policies 
	 */
	public RelationDeclaration(ComponentReference<?> component, String id, ResolvableReference target, boolean isMultiple) {
		
		this(component, id, ComponentKind.INSTANCE, target, ComponentKind.INSTANCE, isMultiple,
				CreationPolicy.MANUAL, ResolvePolicy.EXIST, 
				MissingPolicy.OPTIONAL, null, false,
				false, null, false);
	}

	/**
	 * Declares a new relation with the specified policies
	 */
	public RelationDeclaration(ComponentReference<?> component, String id,
			ComponentKind sourceKind, ResolvableReference target, ComponentKind targetKind, boolean isMultiple,
			CreationPolicy creationPolicy, ResolvePolicy resolvePolicy,
			MissingPolicy missingPolicy, String missingException, Boolean mustHide) {

		this(component, id, sourceKind, target, targetKind, isMultiple,
				creationPolicy, resolvePolicy, 
				missingPolicy, missingException, mustHide,
				false, null, false);
	}
	
	/**
	 * Declares a new contextual relation with the specified policies
	 * 
	 * TODO we should think about having a another class to represent contextual relations 
	 */
	public RelationDeclaration(ComponentReference<?> component, String id,
			ComponentKind sourceKind, ResolvableReference target, ComponentKind targetKind, boolean isMultiple,
			CreationPolicy creationPolicy, ResolvePolicy resolvePolicy,
			MissingPolicy missingPolicy, String missingException, Boolean mustHide, 
			boolean isContextual, String matchSource, boolean isOverride) {
	
		super(target);

		assert component != null && getTarget() != null;

		id 						= (id == null) ? getTarget().as(fr.imag.adele.apam.declarations.references.Reference.class).getIdentifier() : id;
		this.reference 			= new Reference(component, id);

		this.sourceKind 		= sourceKind;
		this.targetKind 		= targetKind;
		
		this.missingPolicy 		= missingPolicy;
		this.missingException	= missingException;

		this.creationPolicy 	= creationPolicy;
		this.resolvePolicy 		= resolvePolicy;
		this.isMultiple 		= isMultiple;

		this.mustHide 			= mustHide;

		this.instrumentations	= new ArrayList<RequirerInstrumentation>();
		this.callbacks 			= new HashMap<Event, Set<CallbackDeclaration>>();

		this.isContextual		= isContextual;
		this.isOverride			= isOverride;
		this.matchSource 		= matchSource;
	}

	
	/**
	 * Clone this declaration
	 */
	public RelationDeclaration(RelationDeclaration original) {

		super(original);

		this.reference = original.reference;

		this.sourceKind = original.sourceKind;
		this.targetKind = original.targetKind;
		this.isMultiple	= original.isMultiple;

		this.creationPolicy	= original.creationPolicy;
		this.resolvePolicy	= original.resolvePolicy;
		
		this.missingPolicy 		= original.missingPolicy;
		this.missingException	= original.missingException;
		this.mustHide			= original.mustHide;

		this.instrumentations	= new ArrayList<RequirerInstrumentation>(original.instrumentations);
		this.callbacks 			= new HashMap<Event, Set<CallbackDeclaration>>();
		for (Map.Entry<Event,Set<CallbackDeclaration>>callbackEntry : original.callbacks.entrySet()) {
			this.callbacks.put(callbackEntry.getKey(), new HashSet<CallbackDeclaration>(callbackEntry.getValue()));
		}

		this.isContextual	= original.isContextual;
		this.isOverride		= original.isOverride;
		this.matchSource 	= original.matchSource;
	}

	/**
	 * Creates a new declaration that is the result of merging the original declaration with
	 * the specified refinement.
	 * 
	 * See {@link #refinedBy(RelationDeclaration)} and {@link #overriddenBy(RelationDeclaration)}
	 * for a description of the merging rules
	 */
	private RelationDeclaration(RelationDeclaration original, RelationDeclaration refinement) {

		super(target(original, refinement),original,refinement);

		this.reference 			= original.reference;

		this.sourceKind 		= original.sourceKind;
		this.targetKind 		= original.targetKind;
		this.isMultiple			= original.isMultiple;

		this.creationPolicy		= original.creationPolicy == null ? refinement.creationPolicy : 
								  refinement.isOverride() && refinement.creationPolicy != null ? refinement.creationPolicy :
								  original.creationPolicy;
										

		this.resolvePolicy		= original.resolvePolicy == null ? refinement.resolvePolicy : 
			  					  refinement.isOverride() && refinement.resolvePolicy != null ? refinement.resolvePolicy :
			  					  original.resolvePolicy;

		
		this.missingPolicy 		= refinement.missingPolicy != null ? refinement.missingPolicy : original.missingPolicy;
		this.missingException	= refinement.missingException != null ? refinement.missingException : original.missingException;
		this.mustHide			= refinement.mustHide;

		
		this.instrumentations	= new ArrayList<RequirerInstrumentation>();
		this.instrumentations.addAll(original.instrumentations);
		this.instrumentations.addAll(refinement.instrumentations);
		
		this.callbacks 			= new HashMap<Event, Set<CallbackDeclaration>>();
		for (Map.Entry<Event,Set<CallbackDeclaration>>callbackEntry : original.callbacks.entrySet()) {
			this.callbacks.put(callbackEntry.getKey(), new HashSet<CallbackDeclaration>(callbackEntry.getValue()));
		}
		for (Map.Entry<Event,Set<CallbackDeclaration>>callbackEntry : refinement.callbacks.entrySet()) {
			this.callbacks.put(callbackEntry.getKey(), new HashSet<CallbackDeclaration>(callbackEntry.getValue()));
		}

		this.isContextual	= original.isContextual;
		this.isOverride		= original.isOverride;
		this.matchSource 	= original.matchSource;
	}

	/**
	 * Determines the effective target of a a refinement.
	 *
	 * We keep the most concrete target between the original and the refinement. 
	 * 
	 * NOTE If the target of the original and the refinement relation are not related by a abstraction
	 * relationship this method may give wrong results. However, we can not validate this, as this requires
	 * the full definition of the target resources and components. We assume that this is validated at
	 * build-time, to ensure safe execution.
	 * 
	 * NOTE For overriding it is not possible to refine the target, because the target of the override is
	 * used as a matching regular expressions.
	 */
	private static ResolvableReference target(RelationDeclaration original, RelationDeclaration refinement) {
		
		if (refinement.isOverride())
			return original.getTarget();
		
		if (refinement.getTarget() instanceof UnknownReference)
			return original.getTarget();

		if (original.getTarget() instanceof UnknownReference)
			return refinement.getTarget();
		
		if (original.getTarget() instanceof ResourceReference && refinement.getTarget() instanceof ComponentReference)
			return refinement.getTarget();

		if (original.getTarget() instanceof ComponentReference && refinement.getTarget() instanceof ResourceReference)
			return original.getTarget();

		if (original.getTarget() instanceof ComponentReference && refinement.getTarget() instanceof ComponentReference) {
			
			ComponentKind originalTargetKind 	= original.getTarget().as(ComponentReference.class).getKind();
			ComponentKind refinementTargetKind 	= refinement.getTarget().as(ComponentReference.class).getKind();
			
			return originalTargetKind.isMoreAbstractThan(refinementTargetKind) ? refinement.getTarget() : original.getTarget();
		}
		
		return original.getTarget();
	} 
	
	@Override
	public boolean equals(Object object) {

		if (this == object)
			return true;

		if (object == null)
			return false;

		if (!(object instanceof RelationDeclaration)) {
			return false;
		}

		RelationDeclaration that = (RelationDeclaration) object;

		/*
		 * overrides and definitions are conceptually in different namespaces
		 */
		if (this.isOverride != that.isOverride)
			return false;

		/*
		 * for overrides we need to consider source and target equality, and not
		 * only identifier
		 */
		if (this.isOverride) {

			if (this.sourceKind != that.sourceKind)
				return false;

			if (this.targetKind != that.targetKind)
				return false;

			if (this.matchSource == null && that.matchSource != null)
				return false;

			if (this.matchSource != null && that.matchSource == null)
				return false;

			if (this.matchSource != null	&& !this.matchSource.equals(that.matchSource))
				return false;

			if (!this.getTarget().equals(that.getTarget()))
				return false;

		}

		/*
		 * Equality is based on identifier of the dependency
		 */
		return this.reference.equals(that.reference);
	}

	@Override
	public int hashCode() {
		return reference.hashCode();
	}
	
	/**
	 * The defining component
	 */
	public ComponentReference<?> getComponent() {
		return reference.getDeclaringComponent();
	}

	/**
	 * Get the reference to this declaration
	 */
	public Reference getReference() {
		return reference;
	}
	
	/**
	 * Get the id of the relation in the declaring component declaration
	 */
	public String getIdentifier() {
		return reference.getIdentifier();
	}

	public ComponentKind getSourceKind() {
		return sourceKind;
	}


	public ComponentKind getTargetKind() {
		return targetKind;
	}

	/**
	 * The multiplicity of a relation.
	 * 
	 * If this is an abstract declaration in specifications or composites, it
	 * must be explicitly defined.
	 * 
	 * Otherwise it is inferred from the needs of the declared instrumentation.
	 */
	public boolean isMultiple() {

		if (getInstrumentations().isEmpty()) {
			return isMultiple;
		}

		/*
		 * If there is at least one instrumentation that can handle multiple
		 * providers the relation is considered is multiple.
		 * 
		 * TODO currently the way messages are handled, they always support
		 * multiple providers, and consequently this forces the relation to be
		 * multi-valued. This is not very intuitive so we added a special case
		 * to ignore messages.
		 * 
		 * Perhaps we should consider the more systematic alternative of
		 * declaring a relation multiple if all the instrumentation can handle
		 * it. But allow an explicit override.
		 */

		boolean oneRequiredService = false;
		boolean supportMultiple = false;

		for (RequirerInstrumentation injection : getInstrumentations()) {

			boolean isService = injection.getRequiredResource().as(InterfaceReference.class) != null;
			oneRequiredService = isService || oneRequiredService;

			if (isService && injection.acceptMultipleProviders()) {
				supportMultiple = true;
			}
		}

		return oneRequiredService ? supportMultiple : isMultiple;
	}

	public ResolvePolicy getResolvePolicy() {
		return resolvePolicy;
	}

	public CreationPolicy getCreationPolicy() {
		return creationPolicy;
	}
	
	/**
	 * Get the policy associated with this relation
	 */
	public MissingPolicy getMissingPolicy() {
		return missingPolicy;
	}

	/**
	 * Get the exception associated with the missing policy
	 */
	public String getMissingException() {
		return missingException;
	}

	/**
	 * Whether an error resolving a relation matching this policy should trigger
	 * a backtrack in resolution
	 */
	public Boolean isHide() {
		return mustHide;
	}
	
	/**
	 * Get the callbacks associated to this relation
	 */
	public Set<CallbackDeclaration> getCallback(Event trigger) {
		return callbacks.get(trigger);
	}

	public void addCallback(Event trigger, CallbackDeclaration callback) {
		if (callbacks.get(trigger) == null) {
			callbacks.put(trigger, new HashSet<CallbackDeclaration>());
		}
		callbacks.get(trigger).add(callback);

	}
	
	/**
	 * Get the instrumentation metadata associated to this relation declaration
	 */
	public List<RequirerInstrumentation> getInstrumentations() {
		return instrumentations;
	}

	/**
	 * Computes the effective declaration that is the result of applying the specified refinement
	 * to this declaration.
	 * 
	 * Refinements can be declared in members of a component or in contextual dependencies in a
	 * composite.
	 * 
	 * The refinement algorithm is the following :
	 * 
	 * 1) The source kind and target kind must  be defined in the this declaration
	 * 2) The target can be refined to a member of the target specified by this declaration 
	 * 3) Constraints are concatenated (this is equivalent to a conjunction of the constraints)
	 * 4) Preferences are concatenated
	 * 5) Policies can be refined only if not explicitly defined in this declaration
	 * 6) Missing policy (resolution failure) can be completely overridden in the refinement
	 * 7) Instrumentation and callbacks are concatenated
	 * 
	 */
	public RelationDeclaration refinedBy(RelationDeclaration refinement) {
		return new RelationDeclaration(this,refinement);
	}

	/**
	 * Whether this declaration is is a contextual declaration
	 */
	public boolean isContextual() {
		return isContextual;
	}

	/**
	 * Checks if this declaration can refine the specified declaration for refinement or override.
	 * Matching can be based on the name of the declaring component or one of its ancestor groups
	 * 
	 * For refinement the name of the declaring component's group and the name of the relation must
	 * match exactly.
	 * 
	 * For overrides, the contextual relation can use regular expression patterns to match the source,
	 * the target or the name of the relation. 
	 * 
	 */
	public boolean refines(ComponentReference<?>group, RelationDeclaration relation) {

		/*
		 * for contextual refinement match source and identifier exactly
		 */
		if (this.isContextual() && ! this.isOverride()) {
			return 	relation.getIdentifier().equals(this.getIdentifier()) &&
					group.getName().equals(this.matchSource);
		}
		
		/*
		 * for contextual override use regular expressions to match, if there is no
		 * matching expression specified it matches every source
		 */
		if (this.isContextual() && this.isOverride()) {
			
			boolean sourceMatched 		= this.matchSource == null || 
									  	  group.getName().matches(this.matchSource);
			
			boolean targetClassMatched	= relation.getTarget().getClass().equals(this.getTarget().getClass());
			boolean targetMatched		= this.getTarget() instanceof UnknownReference ||
									      (targetClassMatched && relation.getTarget().getName().matches(this.getTarget().getName()));
			
			return 	relation.getIdentifier().matches(this.getIdentifier()) &&
					sourceMatched &&
					targetMatched;
		}
		

		/*
		 * The usual match for refinement of abstract components is based on relation
		 * identifiers
		 */
		return relation.getIdentifier().equals(this.getIdentifier());

	}
	
	/**
	 * Checks if this contextual relation definition applies to the specified source
	 */
	public boolean  appliesTo(ComponentReference<?> source) {
		return	this.isContextual() && 
				this.matchSource.equals(source.getName()) && 
				this.getSourceKind() == source.getKind();
	}
	
	/**
	 * Whether this declaration is an override
	 */
	public boolean isOverride() {
		return isOverride;
	}

	
	/**
	 * Computes the effective declaration that is the result of applying the specified override
	 * to this declaration.
	 * 
	 * Overrides can be declared in contextual dependencies in a composite.
	 * 
	 * The override algorithm is the following :
	 * 
	 * 1) The source kind and target kind must  be defined in the this declaration
	 * 2) The target can be refined to a member of the target specified by this declaration 
	 * 3) Constraints are concatenated (this is equivalent to a conjunction of the constraints)
	 * 4) Preferences are concatenated
	 * 5) Policies can be completely overridden
	 * 6) Instrumentation and callbacks are concatenated
	 * 
	 */
	public RelationDeclaration overriddenBy(RelationDeclaration override) {
		assert override.isOverride();
		return new RelationDeclaration(this,override);
	}

	
	public String printRelationDeclaration(String indent) {
		StringBuffer ret = new StringBuffer();
		ret.append(indent + " relation " + getIdentifier() + " towards "
				+ getTarget().getName() + " (creation =" + creationPolicy
				+ ", resolve=" + resolvePolicy + ")");

		if (!instrumentations.isEmpty()) {
			// ret += "\n         Injected dependencies";
			for (RequirerInstrumentation inj : instrumentations) {
				ret.append("   " + inj);
			}
		}

		if (getCallback(Event.BIND) != null
				&& !getCallback(Event.BIND).isEmpty()) {
			ret.append("\n         added");
			for (CallbackDeclaration inj : getCallback(Event.BIND)) {
				ret.append("\n            " + inj.getMethodName());
			}
		}

		if (getCallback(Event.UNBIND) != null
				&& !getCallback(Event.UNBIND).isEmpty()) {
			ret.append("\n         removed");
			for (CallbackDeclaration inj : getCallback(Event.UNBIND)) {
				ret.append("\n            " + inj.getMethodName());
			}
		}

		if (!getImplementationConstraints().isEmpty()) {
			ret.append("\n         Implementation Constraints");
			for (String inj : getImplementationConstraints()) {
				ret.append("\n            " + inj);
			}
		}
		if (!getInstanceConstraints().isEmpty()) {
			ret.append("\n         Instance Constraints");
			for (String inj : getInstanceConstraints()) {
				ret.append("\n            " + inj);
			}
		}
		if (!getImplementationPreferences().isEmpty()) {
			ret.append("\n         Implementation Preferences");
			for (String inj : getImplementationPreferences()) {
				ret.append("\n            " + inj);
			}
		}
		if (!getInstancePreferences().isEmpty()) {
			ret.append("\n         Instance Preferences");
			for (String inj : getInstancePreferences()) {
				ret.append("\n            " + inj);
			}
		}
		return ret.toString();

	}


	@Override
	public String toString() {
		return printRelationDeclaration("");
	}

}
