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

/**
 * This class represents the declaration of a required resources needed by a component, that will be resolved at
 * runtime by APAM.
 * 
 * @author vega
 * 
 */
public class RelationDeclaration extends ConstrainedReference {
	
	/**
	 * The events associated to the runtime life-cycle of the relation
	 */
	public enum Event {
		BIND,
		UNBIND
	}
	
    /**
	 * A reference to a relation declaration. Notice that relation identifiers
	 * must be only unique in the context of their defining component
	 * declaration.
	 */
    public static class Reference extends fr.imag.adele.apam.declarations.Reference {

        private final String identifier;

        public Reference(ComponentReference<?> definingComponent, String identifier) {
            super(definingComponent);
            this.identifier = identifier;
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        public ComponentReference<?> getDeclaringComponent() {
            return (ComponentReference<?>) namespace;
        }

    }

    /**
     * The reference to this declaration
     */
    private final Reference			reference;

    /**
     * The source component for this declaration in the case of contextual dependencies
     */
    private final String			sourceName;

    /**
	 * The level of abstraction where this relation can be instantiated
	 */
    private final ComponentKind		sourceKind;
    
    /**
	 * The level of abstraction of the target of the relation
	 */
    private final ComponentKind		targetKind;

    /**
	 * Whether this relation is declared explicitly as multiple
	 */
    private final boolean			isMultiple;

    /**
     * The policy to handle unresolved dependencies
     */

    private final MissingPolicy		missingPolicy;

    /**
     * The exception to throw for the exception missing policy
     * 
     */
    private final String			missingException;

    /**
	 * The list of instrumentation that need to be performed in the source 
	 * primitive component to implement the semantics of this relation at
	 * runtime. 
	 */
    protected final List<RequirerInstrumentation>			instrumentations;

    /**
     * The map of list of call back methods associated to the relation lifecycle
     */
    protected final Map<Event, Set<CallbackDeclaration>>	callbacks;
    
    /**
	 * Whether this relation is declared explicitly as an override
	 */
    private final boolean				isOverride;
    
    /**
     * Whether a resolution error must trigger a backtrack in the architecture
     */

    private final Boolean            	mustHide;

    /**
     * Whether a resolution error must trigger a backtrack in the architecture
     */
    private final ResolvePolicy			resolvePolicy;
    private final CreationPolicy		creationPolicy;
    
    public RelationDeclaration(ComponentReference<?> component,  String id, ResolvableReference target, boolean isMultiple) {
    	this(component,id,
    	null,ComponentKind.INSTANCE,
    	target,ComponentKind.INSTANCE,
    	CreationPolicy.MANUAL, ResolvePolicy.EXIST, isMultiple,
    	MissingPolicy.OPTIONAL,null,
    	false,false);
    }

    public RelationDeclaration(ComponentReference<?> component, String id, 
    				String sourceName, ComponentKind sourceKind, 
    				ResolvableReference target, ComponentKind targetKind,
       				CreationPolicy creationPolicy, 
       				ResolvePolicy resolvePolicy,
       				boolean isMultiple,
    				MissingPolicy missingPolicy, String missingException,
     				boolean isOverride, Boolean mustHide) {

        super(target);

        assert component != null && target != null;
        
        id 						= (id == null) ? getTarget().as(fr.imag.adele.apam.declarations.Reference.class).getIdentifier() : id;
        this.reference			= new Reference(component,id);

        this.sourceName			= sourceName;
        this.sourceKind			= sourceKind;
        this.targetKind			= targetKind;

        this.missingPolicy 		= missingPolicy;
        this.missingException 	= missingException;
        
        this.creationPolicy		= creationPolicy;
        this.resolvePolicy		= resolvePolicy;
        this.isMultiple 		= isMultiple;
        
        this.isOverride			= isOverride;
        this.mustHide 			= mustHide;

        this.instrumentations	= new ArrayList<RequirerInstrumentation>();
        this.callbacks 			= new HashMap<Event, Set<CallbackDeclaration>>();
        
    }

    /**
     * Computes the effective declaration that is the result of applying the specified refinement to this
     * declaration.
     * 
	 * TODO currently we keep the most general target definition and we loose the target declaration of 
	 * the lower level, should we add additional constraints to represent the narrowing of the target?
     * 
     */
    public RelationDeclaration refinedBy(RelationDeclaration refinement) {
    	
    	assert this.getIdentifier().equals(refinement.getIdentifier());
    	
        RelationDeclaration effective = new RelationDeclaration(refinement.getComponent(),this.getIdentifier(),
        										this.getSourceName(),this.getSourceKind(),
        										this.getTarget(),this.getTargetKind(), 
        										this.getCreationPolicy() == null ? refinement.getCreationPolicy() : this.getCreationPolicy(),
        										this.getResolvePolicy() == null ? refinement.getResolvePolicy() : this.getResolvePolicy(),
        										refinement.isMultiple,
        										this.getMissingPolicy() == null ? refinement.getMissingPolicy() : this.getMissingPolicy(),
        										this.getMissingException() == null ? refinement.getMissingException() : this.getMissingException(),
        										refinement.isOverride,refinement.mustHide);


        effective.instrumentations.addAll(this.instrumentations);
        effective.instrumentations.addAll(refinement.instrumentations);

        effective.callbacks.putAll(this.callbacks);
        effective.callbacks.putAll(refinement.callbacks);
        
        effective.getImplementationConstraints().addAll(this.getImplementationConstraints());
        effective.getImplementationConstraints().addAll(refinement.getImplementationConstraints());
        
        effective.getInstanceConstraints().addAll(this.getInstanceConstraints());
        effective.getInstanceConstraints().addAll(refinement.getInstanceConstraints());
        
        effective.getImplementationPreferences().addAll(this.getImplementationPreferences());
        effective.getImplementationPreferences().addAll(refinement.getImplementationPreferences());
        
        effective.getInstancePreferences().addAll(this.getInstancePreferences());
        effective.getInstancePreferences().addAll(refinement.getInstancePreferences());

        return effective;

    }
    
    /**
     * Computes the effective declaration that is the result of applying the specified override to this
     * declaration.
     * 
     */
    public RelationDeclaration overriddenBy(RelationDeclaration override) {
    	
    	assert override.isOverride() && this.getIdentifier().matches(override.getIdentifier());
    	
        RelationDeclaration effective = new RelationDeclaration(this.getComponent(),this.getIdentifier(),
        										this.getSourceName(),this.getSourceKind(),
        										this.getTarget(),this.getTargetKind(), 
        										override.getCreationPolicy() != null ? override.getCreationPolicy() : this.getCreationPolicy(),
              									override.getResolvePolicy() != null ? override.getResolvePolicy() : this.getResolvePolicy(),
        										this.isMultiple,
        										override.getMissingPolicy() != null ? override.getMissingPolicy() : this.getMissingPolicy(),
        										override.getMissingException() != null ? override.getMissingException() : this.getMissingException(),
        										this.isOverride,override.mustHide);


        effective.getImplementationConstraints().addAll(this.getImplementationConstraints());
        effective.getImplementationConstraints().addAll(override.getImplementationConstraints());
        
        effective.getInstanceConstraints().addAll(this.getInstanceConstraints());
        effective.getInstanceConstraints().addAll(override.getInstanceConstraints());
        
        effective.getImplementationPreferences().addAll(this.getImplementationPreferences());
        effective.getImplementationPreferences().addAll(override.getImplementationPreferences());
        
        effective.getInstancePreferences().addAll(this.getInstancePreferences());
        effective.getInstancePreferences().addAll(override.getInstancePreferences());

        return effective;

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
    
    /**
     * The source component for contextual dependencies
     */
    public String getSourceName() {
        return this.sourceName;
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

        if (getInstrumentations().isEmpty())
            return isMultiple;

		/*
		 * If there is at least one instrumentation that can handle multiple
		 * providers the relation is considered is multiple.
		 * 
		 * TODO currently the way messages are handled, they always support multiple
		 * providers, and consequently this forces the relation to be multi-valued. 
		 * This is not very intuitive so we added a special case to ignore messages.
		 *
		 * Perhaps we should consider the more systematic alternative of declaring a
		 * relation multiple if all the instrumentation can handle it. But allow an
		 * explicit override.
		 */
        
        boolean oneRequiredService 	= false;
        boolean supportMultiple		= false;
        
        for (RequirerInstrumentation injection : getInstrumentations()) {
        	
        	boolean isService	= injection.getRequiredResource().as(InterfaceReference.class) != null;
        	oneRequiredService	= isService || oneRequiredService;
        	
            if (isService && injection.acceptMultipleProviders())
            	supportMultiple = true;
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
	 * Get the instrumentation metadata associated to this relation declaration
	 */
    public List<RequirerInstrumentation> getInstrumentations() {
        return instrumentations;
    }

    public void addCallback(Event trigger, CallbackDeclaration callback) {
        if (callbacks.get(trigger) == null) {
            callbacks.put(trigger, new HashSet<CallbackDeclaration>());
        }
        callbacks.get(trigger).add(callback);

    }

    public Set<CallbackDeclaration> getCallback(Event trigger) {
        return callbacks.get(trigger);
    }

    /**
     * Whether this declaration is an override
     */
    public boolean isOverride() {
    	return isOverride;
    }
    
    /**
	 * Whether an error resolving a relation matching this policy should trigger
	 * a backtrack in resolution
	 */
    public Boolean isHide() {
        return mustHide;
    }

    @Override
    public boolean equals(Object object) {
    	if (! (object instanceof RelationDeclaration))
    		return false;
    	
    	RelationDeclaration that = (RelationDeclaration) object;
    	return this.reference.equals(that.reference);
    }
    
    @Override
    public int hashCode() {
    	return reference.hashCode();
    }

    @Override
    public String toString() {
        return printRelationDeclaration("");
    }

    public String printRelationDeclaration(String indent) {
        StringBuffer ret = new StringBuffer ();
		ret.append(indent + " relation " + getIdentifier() + " towards "
				+ getTarget().getName() +" (creation ="+creationPolicy+", resolve="+resolvePolicy+")");
    
        
        if (!instrumentations.isEmpty()) {
            // ret += "\n         Injected dependencies";
            for (RequirerInstrumentation inj : instrumentations) {
            	ret.append ("   " + inj);
            }
        }

        if (getCallback(Event.BIND)!= null && !getCallback(Event.BIND).isEmpty()) {
        	ret.append ("\n         added");
            for (CallbackDeclaration inj : getCallback(Event.BIND)) {
            	ret.append ("\n            " + inj.methodName);
            }
        }
        
        if (getCallback(Event.UNBIND)!=null && !getCallback(Event.UNBIND).isEmpty()) {
        	ret.append ("\n         removed");
            for (CallbackDeclaration inj : getCallback(Event.UNBIND)) {
            	ret.append ("\n            " + inj.methodName);
            }
        }
        
        if (!getImplementationConstraints().isEmpty()) {
        	ret.append ("\n         Implementation Constraints");
            for (String inj : getImplementationConstraints()) {
            	ret.append ("\n            " + inj);
            }
        }
        if (!getInstanceConstraints().isEmpty()) {
        	ret.append ("\n         Instance Constraints");
            for (String inj : getInstanceConstraints()) {
            	ret.append ("\n            " + inj);
            }
        }
        if (!getImplementationPreferences().isEmpty()) {
        	ret.append ("\n         Implementation Preferences");
            for (String inj : getImplementationPreferences()) {
            	ret.append ("\n            " + inj);
            }
        }
        if (!getInstancePreferences().isEmpty()) {
        	ret.append ("\n         Instance Preferences");
            for (String inj : getInstancePreferences()) {
            	ret.append ("\n            " + inj);
            }
        }
        return ret.toString();

    }


    
}
