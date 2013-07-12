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
public class RelationDeclaration extends ConstrainedReference implements Cloneable {
	
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
     * The map of list of call back methods associated to the same trigger
     */
    protected final Map<Event, Set<CallbackDeclaration>> callbacks;

    /**
     * The reference to this declaration
     */
    private final Reference    reference;

    /**
     * The source component for this declaration in the case of contextual dependencies
     */
    private final String 		sourceName;

    /**
	 * The level of abstraction where this relation can be instantiated
	 */
    private final ComponentKind	sourceKind;
    
    /**
	 * The level of abstraction of the target of the relation
	 */
    private final ComponentKind	targetKind;

    /**
	 * Whether this relation is declared explicitly as multiple
	 */
    private final boolean		isMultiple;
    
    /**
	 * Whether this relation is declared explicitly as an override
	 */
    private final boolean		isOverride;
    

    /**
	 * The list of instrumentation that need to be performed in the source 
	 * primitive component to implement the semantics of this relation at
	 * runtime. 
	 */
    protected final List<RequirerInstrumentation>		instrumentations;

    /**
     * The policy to handle unresolved dependencies
     */

    private MissingPolicy  		missingPolicy;

    /**
     * The exception to throw for the exception missing policy
     * 
     */
    private String              missingException;

    /**
	 * Whether a relation matching this policy must be eagerly resolved
	 */
    //private Boolean            isEager;

    /**
     * Whether a resolution error must trigger a backtrack in the architecture
     */

    private Boolean             mustHide;

    /**
     * Whether a resolution error must trigger a backtrack in the architecture
     */
    private ResolvePolicy     resolvePolicy=null;
    private CreationPolicy      creationPolicy=null;
    
    
    /**
	 * 
	 * @param component
	 *            the name of the component who had the reference
	 * @param id
	 *            the id of the relation
	 * @param isMultiple
	 *            define if this relation is multiple
	 * @param resource
	 *            the resource which we should look for
	 */
    
    public RelationDeclaration(ComponentReference<?> component,  String id, boolean isOverride,
    		boolean isMultiple, ResolvableReference resource) {
    	this(component,id,isOverride,isMultiple,resource, null,ComponentKind.INSTANCE,ComponentKind.INSTANCE,null,null);
    }

    public RelationDeclaration(ComponentReference<?> component, String id, boolean isOverride,
    		boolean isMultiple, ResolvableReference resource,
    		String sourceName, ComponentKind sourceKind, ComponentKind targetKind, ResolvePolicy resolve, CreationPolicy creation) {

        super(resource);

        assert component != null;
        
        if (id==null && getTarget() == null) {
        	System.err.println("ERRROR Both id and target null for relation in component " + component ) ;
        }
        
        id 						= (id == null) ? getTarget().as(fr.imag.adele.apam.declarations.Reference.class).getIdentifier() : id;
        this.isOverride			= isOverride;
        this.reference			= new Reference(component,id);

        this.sourceName			= sourceName;
        this.sourceKind			= sourceKind;
        this.targetKind			= targetKind;
        
        this.resolvePolicy=resolve;
        
        this.creationPolicy=creation;
        
        this.isMultiple 		= isMultiple;
        //this.isEager 			= null;
        this.mustHide 			= null;
        this.missingPolicy 		= null;
        this.missingException 	= null;
        this.callbacks 			= new HashMap<Event, Set<CallbackDeclaration>>();
        this.instrumentations	= new ArrayList<RequirerInstrumentation>();
        
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
    public RelationDeclaration clone() {

        RelationDeclaration clone = new RelationDeclaration(this.reference.getDeclaringComponent(), this.reference.getIdentifier(), this.isOverride,
        		this.isMultiple(), this.getTarget(), this.sourceName, this.sourceKind, this.targetKind,this.resolvePolicy,this.creationPolicy);

//        clone.setSourceKind(this.sourceKind);
//        clone.setTargetType(this.targetKind);
        
        clone.callbacks.putAll(this.callbacks);
        clone.instrumentations.addAll(this.instrumentations);
        
        clone.getImplementationConstraints().addAll(this.getImplementationConstraints());
        clone.getInstanceConstraints().addAll(this.getInstanceConstraints());
        clone.getImplementationPreferences().addAll(this.getImplementationPreferences());
        clone.getInstancePreferences().addAll(this.getInstancePreferences());

        clone.setMissingException(this.getMissingException());
        clone.setMissingPolicy(this.getMissingPolicy());

        clone.setCreationPolicy(this.getCreationPolicy());
        clone.setResolvePolicy(this.getResolvePolicy());
        
        return clone;
    }

    /**
     * The defining component
     */
    public ComponentReference<?> getComponent() {
        return reference.getDeclaringComponent();
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
	 * Get the id of the relation in the declaring component declaration
	 */
    public String getIdentifier() {
        return reference.getIdentifier();
    }

    /**
     * Whether this declaration is an override
     */
    public boolean isOverride() {
    	return isOverride;
    }
    
    /**
     * Get the reference to this declaration
     */
    public Reference getReference() {
        return reference;
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

    /**
	 * Get the policy associated with this relation
	 */
    public MissingPolicy getMissingPolicy() {
        return missingPolicy;
    }

    /**
	 * Set the missing policy used for this relation
	 */
    public void setMissingPolicy(MissingPolicy missingPolicy) {
        this.missingPolicy = missingPolicy;
    }

    /**
     * Whether dependencies matching this contextual policy must be resolved eagerly
     */
    @Deprecated
    public Boolean isEager() {
        return this.creationPolicy==null?null:this.creationPolicy==CreationPolicy.EAGER;
    }

    @Deprecated
    public boolean isEffectiveEager() {
    	return this.creationPolicy != null ? this.creationPolicy==CreationPolicy.EAGER : false;
    }
    
    public void setEager(Boolean isEager) {
    	
        this.creationPolicy = isEager?CreationPolicy.EAGER:null;
    }

    /**
	 * Whether an error resolving a relation matching this policy should trigger
	 * a backtrack in resolution
	 */
    public Boolean isHide() {
        return mustHide;
    }

    public void setHide(Boolean mustHide) {
        this.mustHide = mustHide;
    }

    /**
     * Get the exception associated with the missing policy
     */
    public String getMissingException() {
        return missingException;
    }

    /**
	 * Set the missing exception used for this relation
	 */
    public void setMissingException(String missingException) {
        this.missingException = missingException;
    }

    /**
	 * Get the instrumentations associated to this relation declaration
	 */
    public List<RequirerInstrumentation> getInstrumentations() {
        return instrumentations;
    }

    @Override
    public String toString() {
        return printRelationDeclaration("");
    }

    public String printRelationDeclaration(String indent) {
        StringBuffer ret = new StringBuffer ();
		ret.append(indent + " relation " + getIdentifier() + " towards "
				+ getTarget().getName());
    
        
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

    public void addCallback(Event trigger, CallbackDeclaration callback) {
        if (callbacks.get(trigger) == null) {
            callbacks.put(trigger, new HashSet<CallbackDeclaration>());
        }
        callbacks.get(trigger).add(callback);

    }

    public Set<CallbackDeclaration> getCallback(Event trigger) {
        return callbacks.get(trigger);
    }

	public ResolvePolicy getResolvePolicy() {
		return resolvePolicy;
	}

	public void setResolvePolicy(ResolvePolicy resolvePolicy) {
		this.resolvePolicy = resolvePolicy;
	}

	public CreationPolicy getCreationPolicy() {
		return creationPolicy;
	}

	public void setCreationPolicy(CreationPolicy creationPolicy) {
		this.creationPolicy = creationPolicy;
	}


    
}
