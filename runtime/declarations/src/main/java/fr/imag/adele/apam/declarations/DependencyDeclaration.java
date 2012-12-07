package fr.imag.adele.apam.declarations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.declarations.CallbackMethod.CallbackTrigger;

/**
 * This class represents the declaration of a required resources needed by a component, that will be resolved at
 * runtime by APAM.
 * 
 * @author vega
 * 
 */
public class DependencyDeclaration extends ConstrainedReference implements Cloneable {

    /**
     * A reference to a dependency declaration. Notice that dependency identifiers must be only
     * unique in the context of their defining component declaration.
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
    private final Map<CallbackTrigger, Set<CallbackMethod>> callbacks;

    /**
     * The reference to this declaration
     */
    private final Reference                           reference;

    /**
     * Whether this dependency is declared explicitly as multiple
     */
    private final boolean                             isMultiple;

    /**
     * The list of fields that will be injected with this dependency in a primitive component
     */
    private final List<DependencyInjection>           injections;

    /**
     * The policy to handle unresolved dependencies
     */

    private MissingPolicy                             missingPolicy;

    /**
     * The exception to throw for the exception missing policy
     * 
     */
    private String                                    missingException;

    /**
     * Whether a dependency matching this policy must be eagerly resolved
     */
    private Boolean                                   isEager;

    /**
     * Whether a resolution error must trigger a backtrack in the architecture
     */

    private Boolean                                   mustHide;

    public DependencyDeclaration(ComponentReference<?> component, String id, boolean isMultiple,
            ResolvableReference resource) {

        super(resource);

        assert component != null;
        
        id 						= (id == null) ? getTarget().as(fr.imag.adele.apam.declarations.Reference.class).getIdentifier() : id;
        this.reference			= new Reference(component,id);

        this.isMultiple 		= isMultiple;
        this.isEager 			= null;
        this.mustHide 			= null;
        this.missingPolicy 		= null;
        this.missingException 	= null;
        this.callbacks 			= new HashMap<CallbackTrigger, Set<CallbackMethod>>();
        this.injections			= new ArrayList<DependencyInjection>();
    }
    
    @Override
    public boolean equals(Object object) {
    	if (! (object instanceof DependencyDeclaration))
    		return false;
    	
    	DependencyDeclaration that = (DependencyDeclaration) object;
    	return this.reference.equals(that.reference);
    }
    
    @Override
    public int hashCode() {
    	return reference.hashCode();
    }

    @Override
    public DependencyDeclaration clone() {

        DependencyDeclaration clone = new DependencyDeclaration(this.reference.getDeclaringComponent(), this.reference
                .getIdentifier(), this.isMultiple(), this.getTarget());

        clone.callbacks.putAll(this.callbacks);
        clone.injections.addAll(this.injections);
        
        clone.getImplementationConstraints().addAll(this.getImplementationConstraints());
        clone.getInstanceConstraints().addAll(this.getInstanceConstraints());
        clone.getImplementationPreferences().addAll(this.getImplementationPreferences());
        clone.getInstancePreferences().addAll(this.getInstancePreferences());

        clone.setMissingException(this.getMissingException());
        clone.setMissingPolicy(this.getMissingPolicy());

        return clone;
    }

    /**
     * The defining component
     */
    public ComponentReference<?> getComponent() {
        return reference.getDeclaringComponent();
    }

    /**
     * Get the id of the dependency in the declaring component declaration
     */
    public String getIdentifier() {
        return reference.getIdentifier();
    }

    /**
     * Get the reference to this declaration
     */
    public Reference getReference() {
        return reference;
    }

    /**
     * The multiplicity of a dependency.
     * 
     * If there are calculated from the declaration of injected fields.
     * 
     * If this is an abstract declaration in specifications or composites, it must be
     * explicitly defined.
     */
    public boolean isMultiple() {

        if (getInjections().isEmpty())
            return isMultiple;

        // If there is at least one field declared collection the dependency is considered multiple
        for (DependencyInjection injection : getInjections()) {
            if (injection.isCollection())
                return true;
        }

        return false;
    }

    /**
     * Get the policy associated with this dependency
     */
    public MissingPolicy getMissingPolicy() {
        return missingPolicy;
    }

    /**
     * Set the missing policy used for this dependency
     */
    public void setMissingPolicy(MissingPolicy missingPolicy) {
        this.missingPolicy = missingPolicy;
    }

    /**
     * Whether dependencies matching this contextual policy must be resolved eagerly
     */
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

    /**
     * Get the exception associated with the missing policy
     */
    public String getMissingException() {
        return missingException;
    }

    /**
     * Set the missing exception used for this dependency
     */
    public void setMissingException(String missingException) {
        this.missingException = missingException;
    }

    /**
     * Get the injections associated to this dependency declaration
     */
    public List<DependencyInjection> getInjections() {
        return injections;
    }

    @Override
    public String toString() {
        return printDependencyDeclaration("");
    }

    public String printDependencyDeclaration(String indent) {
        String ret = indent + " dependency id: " + getIdentifier() + ". toward " + getTarget();
    
        
        if (!injections.isEmpty()) {
            // ret += "\n         Injected dependencies";
            for (DependencyInjection inj : injections) {
                ret += "   " + inj;
            }
        }

        if (getCallback(CallbackTrigger.Bind)!=null && !getCallback(CallbackTrigger.Bind).isEmpty()) {
            ret += "\n         added";
            for (CallbackMethod inj : getCallback(CallbackTrigger.Bind)) {
                ret += "\n            " + inj.methodName;
            }
        }
        
        if (getCallback(CallbackTrigger.Unbind)!=null && !getCallback(CallbackTrigger.Unbind).isEmpty()) {
            ret += "\n         removed";
            for (CallbackMethod inj : getCallback(CallbackTrigger.Unbind)) {
                ret += "\n            " + inj.methodName;
            }
        }
        
        if (!getImplementationConstraints().isEmpty()) {
            ret += "\n         Implementation Constraints";
            for (String inj : getImplementationConstraints()) {
                ret += "\n            " + inj;
            }
        }
        if (!getInstanceConstraints().isEmpty()) {
            ret += "\n         Instance Constraints";
            for (String inj : getInstanceConstraints()) {
                ret += "\n            " + inj;
            }
        }
        if (!getImplementationPreferences().isEmpty()) {
            ret += "\n         Implementation Preferences";
            for (String inj : getImplementationPreferences()) {
                ret += "\n            " + inj;
            }
        }
        if (!getInstancePreferences().isEmpty()) {
            ret += "\n         Instance Preferences";
            for (String inj : getInstancePreferences()) {
                ret += "\n            " + inj;
            }
        }
        return ret;

    }

    public void addCallback(CallbackMethod callback) {
        if (callbacks.get(callback.trigger) == null) {
            callbacks.put(callback.trigger, new HashSet<CallbackMethod>());
        }
        callbacks.get(callback.trigger).add(callback);

    }

    public Set<CallbackMethod> getCallback(CallbackTrigger trigger) {
        return callbacks.get(trigger);
    }

}
