package fr.imag.adele.apam.core;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the declaration of a required resources needed by a component, that will be resolved at
 * runtime by APAM.
 * 
 * @author vega
 *
 */
public class DependencyDeclaration extends TargetDeclaration {

    /**
     * A reference to a dependency declaration. Notice that dependency identifiers must be only
     * unique in the context of their defining component declaration.
     */
    public static class Reference extends fr.imag.adele.apam.core.Reference {

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
    private final Reference					reference;
    
    /**
     * Whether this dependency is declared explicitly as multiple
     */
    private final boolean					isMultiple;
    
    /**
     * The list of preferences to choose among candidate service provider implementation
     */
    private final List<String> 				implementationPreferences;

    /**
     * The list of preferences to choose among candidate service provider instances
     */
    private final List<String> 				instancePreferences;

    /**
     * The list of fields that will be injected with this dependency in a primitive component
     */
    private final List<DependencyInjection> injections;

    /**
     * The policy to handle unresolved dependencies
     */

    private MissingPolicy 					missingPolicy;

    public DependencyDeclaration(ComponentReference<?> component, String id, boolean isMultiple, ResolvableReference resource) {

        super(resource);

        assert component != null;
        
        id = (id == null) ? getTarget().as(fr.imag.adele.apam.core.Reference.class).getIdentifier() : id;
        this.reference	= new Reference(component,id);

        this.isMultiple	= isMultiple;

        implementationPreferences 	= new ArrayList<String>();
        instancePreferences 		= new ArrayList<String>();
        injections					= new ArrayList<DependencyInjection>();
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
     * @param missingPolicy
     */
    public void setMissingPolicy(MissingPolicy missingPolicy) {
        this.missingPolicy = missingPolicy;
    }

    /**
     * Get the resource provider preferences
     */
    public List<String> getImplementationPreferences() {
        return implementationPreferences;
    }

    /**
     * Get the instance provider preferences
     */
    public List<String> getInstancePreferences() {
        return instancePreferences;
    }

    /**
     * Get the injections associated to this dependency declaration
     */
    public List<DependencyInjection> getInjections() {
        return injections;
    }


    @Override
    public String toString() {
        return printDependencyDeclaration ("");
    }
    
    public String printDependencyDeclaration (String indent) {
        String ret = indent + " dependency id: " + getIdentifier() + ". toward " + getTarget();
        if (! injections.isEmpty()) {
            // ret += "\n         Injected dependencies";
            for (DependencyInjection inj : injections) {
                ret += "   " + inj;
            }
        }

        if (! getImplementationConstraints().isEmpty()) {
            ret += "\n         Implementation Constraints";
            for (String inj : getImplementationConstraints()) {
                ret += "\n            " + inj;
            }
        }
        if (! getInstanceConstraints().isEmpty()) {
            ret += "\n         Instance Constraints";
            for (String inj : getInstanceConstraints()) {
                ret += "\n            " + inj;
            }
        }
        if (! getImplementationPreferences().isEmpty()) {
            ret += "\n         Implementation Preferences";
            for (String inj : getImplementationPreferences()) {
                ret += "\n            " + inj;
            }
        }
        if (! getInstancePreferences().isEmpty()) {
            ret += "\n         Instance Preferences";
            for (String inj : getInstancePreferences()) {
                ret += "\n            " + inj;
            }
        }
        return ret;
    	
    }

}
