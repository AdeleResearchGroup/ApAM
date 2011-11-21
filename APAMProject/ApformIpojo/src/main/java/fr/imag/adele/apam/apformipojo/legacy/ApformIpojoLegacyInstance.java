package fr.imag.adele.apam.apformipojo.legacy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandlerDescription;
import org.osgi.framework.Filter;

import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apformipojo.Dependency;

public class ApformIpojoLegacyInstance implements ApformInstance, Dependency.Resolver {

	/**
	 * The iPojo instance represented by this proxy
	 */
	private final ComponentInstance ipojoInstance;
	
	/**
	 * The associated APAM instance
	 */
	private Instance apamInstance;

    /**
     * The list of dependencies declared in this component
     */
    private final Map<String, Dependency> dependencies;

	public ApformIpojoLegacyInstance(ComponentInstance ipojoInstance) {
		this.ipojoInstance	= ipojoInstance;
		this.dependencies	= new HashMap<String, Dependency>();
	}
	
	@Override
	public void setInst(Instance apamInstance) {
		this.apamInstance = apamInstance;
	}

    /**
     * Adds a new dependency declaration to this instance
     */
    public void addDependency(Dependency dependency) {
        dependencies.put(dependency.getName(), dependency);
    }

    /**
     * The state of the dependencies
     */
    public Collection<Dependency> getDependencies() {
        return dependencies.values();
    }

    /**
     * Delegate APAM to resolve a given dependency.
     * 
     * NOTE nothing is returned from this method, the call to APAM has as
     * side-effect the update of the dependency.
     * 
     * @param dependency
     */
    public void resolve(Dependency dependency) {

        /*
         * This instance is not actually yet managed by APAM
         */
        if (apamInstance == null) {
            System.err.println("resolve failure for client " + ipojoInstance.getInstanceName() + " : ASM instance unkown");
            return;
        }

        System.err.println("resolving " + ipojoInstance.getInstanceName() + " dependency " + dependency.getName());
        /*
         * Make a copy of constraints and preferences before invoking resolution. This allow resolution managers to modify constraints
         * and preferences are part of their processing.
         */
        Set<Filter> constraints = new HashSet<Filter>(dependency.getConstraints());
        List<Filter> preferences = new ArrayList<Filter>(dependency.getPreferences());

        switch (dependency.getKind()) {
            case IMPLEMENTATION:
                if (dependency.isScalar())
                    ApamResolver.newWireImpl(apamInstance, dependency.getTarget(), dependency.getName(), constraints,
                            preferences);
                else
                    ApamResolver.newWireImpls(apamInstance, dependency.getTarget(), dependency.getName(), constraints);
                break;
            case SPECIFICATION:
                if (dependency.isScalar())
                    ApamResolver.newWireSpec(apamInstance, null, dependency.getTarget(), dependency.getName(),
                            constraints, preferences);
                else
                    ApamResolver.newWireSpecs(apamInstance, null, dependency.getTarget(), dependency.getName(),
                            constraints, preferences);
                break;
            case INTERFACE:
                if (dependency.isScalar())
                    ApamResolver.newWireSpec(apamInstance, dependency.getTarget(), null, dependency.getName(),
                            constraints, preferences);
                else
                    ApamResolver.newWireSpecs(apamInstance, dependency.getTarget(), null, dependency.getName(),
                            constraints, preferences);
                break;
        }
    }
    
    
   /**
     * Apform: get the properties of the instance
     */
    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();

        // Get access to the iPojo configuration handler managed properties
        ConfigurationHandlerDescription configurationDescription =
                (ConfigurationHandlerDescription) ipojoInstance.getInstanceDescription().getHandlerDescription("properties");

        if (configurationDescription == null)
            return properties;

        for (PropertyDescription property : configurationDescription.getProperties()) {
            properties.put(property.getName(), property.getObjectValue(ipojoInstance.getContext()));
        }

        return properties;
    }

    /**
     * Apform: get name of the instance
     */
    @Override
    public String getName() {
        return ipojoInstance.getInstanceName();
    }

    /**
     * Apform: get the service object of the instance
     */
    @Override
    public Object getServiceObject() {
        return ((InstanceManager)ipojoInstance).getPojoObject();
    }

    /**
     * Apform: get the name of the implementation of the instance
     */
    @Override
    public String getImplemName() {
        return ipojoInstance.getFactory().getName();
    }

    /**
     * Apform: resolve dependency
     */
    @Override
    public boolean setWire(Instance destInst, String depName) {
        System.err.println("Legacy instance set wire " + depName + " :" + ipojoInstance.getInstanceName() + "->" + destInst);
        Dependency dependency = dependencies.get(depName);

        if (dependency == null)
            return false;

        dependency.addTarget(destInst);
        return true;
    }

    /**
     * Apform: unresolve dependency
     */
    @Override
    public boolean remWire(Instance destInst, String depName) {
        System.err.println("Legacy instance rem wire " + depName + " :" + ipojoInstance.getInstanceName() + "->" + destInst);
        Dependency dependency = dependencies.get(depName);

        if (dependency == null)
            return false;

        dependency.removeTarget(destInst);
        return true;
    }

    /**
     * Apform: substitute dependency
     */
    @Override
    public boolean substWire(Instance oldDestInst, Instance newDestInst, String depName) {
        System.err.println("Legacy instance subs wire " + depName + " :" + ipojoInstance.getInstanceName() + "from ->" + oldDestInst
                + " to ->" + newDestInst);
        Dependency dependency = dependencies.get(depName);

        if (dependency == null)
            return false;

        dependency.substituteTarget(oldDestInst, newDestInst);
        return true;
    }

}
