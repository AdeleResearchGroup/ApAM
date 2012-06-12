package fr.imag.adele.apam.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * This class represents the declaration of a composite implementation
 * 
 * @author vega
 *
 */
public class CompositeDeclaration extends ImplementationDeclaration {

    /**
     * The main implementation of the composite
     */
    private final ComponentReference<?> mainComponent;
    
    /**
     * The list of owned components
     */
	private final Set<OwnedComponentDeclaration> ownedComponents;

	/**
	 * The list of declared instances in this composite
	 */
	private final List<InstanceDeclaration> instances;

	/**
	 * The list of contextual missing policies of this composite
	 */
	private final List<ContextualMissingPolicy> missingPolicies;

	/**
	 * The list of states of this composite
	 */
	private final List<String> states;
	
	/**
	 * The default state
	 */
	private final String initialState;
	
	/**
	 * The list of automatic resource grants
	 */
	private final List<GrantDeclaration> grants;
	
	/**
	 * The list of automatic resource releases
	 */
	private final List<ReleaseDeclaration> releases;
	
    public CompositeDeclaration(String name, SpecificationReference specification, ComponentReference<?> mainComponent, String initialState, List<String> states) {
        super(name, specification);

        assert mainComponent != null;

        this.mainComponent 		= mainComponent;
        this.states				= states;
        this.initialState		= initialState;

        this.ownedComponents	= new HashSet<OwnedComponentDeclaration>();
        this.instances			= new ArrayList<InstanceDeclaration>();
        this.missingPolicies	= new ArrayList<ContextualMissingPolicy>();
        this.grants				= new ArrayList<GrantDeclaration>();
        this.releases			= new ArrayList<ReleaseDeclaration>();
        
    }

	/**
	 * A reference to a composite implementation
	 */
    private static class Reference extends ImplementationReference<CompositeDeclaration> {

		public Reference(String name) {
			super(name);
		}

	}

    /**
     * Generates the reference to this implementation
     */
    @Override
    protected ImplementationReference<CompositeDeclaration> generateReference() {
    	return new Reference(getName());
    }

    /**
     * Get the main implementation
     */
    public ComponentReference<?> getMainComponent() {
        return mainComponent;
    }

    /**
     * The list of possible states of this composite
     */
    public List<String> getStates() {
		return states;
	}
    
    /**
     * The default state of the composite
     */
    public String getInitialState() {
		return initialState;
	}
    
    /**
     * Get the list of owned appearing components 
     */
    public Set<OwnedComponentDeclaration> getOwnedComponents() {
    	return ownedComponents;
    }
    
    /**
     * Get the list of declared instances
     */
    public List<InstanceDeclaration> getInstanceDeclarations() {
    	return instances;
    }
    
    /**
     * The list of contextual missing policies
     */
    public List<ContextualMissingPolicy> getMissingPolicies() {
		return missingPolicies;
	}
    
    /**
     * The list of resource grants
     */
    public List<GrantDeclaration> getGrants() {
		return grants;
	}

    /**
     * The list of resource releases
     */
    public List<ReleaseDeclaration> getReleases() {
		return releases;
	}
    
    @Override
    public String toString() {
        String ret = "\nComposite declaration " + super.toString();
        ret += "\n   Main Implementation: " + mainComponent.getIdentifier();
        return ret;
    }

}
