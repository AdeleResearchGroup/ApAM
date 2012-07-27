package fr.imag.adele.apam.apformipojo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandlerDescription;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformInstance;

public class ApformIpojoInstance extends InstanceManager implements ApformInstance {

    /**
     * Whether this instance was created directly using the APAM API
     */
    private final boolean                 isApamCreated;

    /**
     * The APAM instance associated to this component instance
     */
    private Instance                      apamInstance;

    /**
     * The list of dependencies declared in this component
     */
    private final Map<String, Dependency> dependencies;

    /**
     * 
     * @param implementation
     * @param isApamCreated
     * @param context
     * @param handlers
     */
    public ApformIpojoInstance(ApformIpojoImplementation implementation, boolean isApamCreated, BundleContext context,
            HandlerManager[] handlers) {
        super(implementation, context, handlers);

        this.isApamCreated = isApamCreated;
        dependencies = new HashMap<String, Dependency>();

    }

    @Override
    public ApformIpojoImplementation getFactory() {
        return (ApformIpojoImplementation) super.getFactory();
    }

    /**
     * Attach an APAM logical instance to this platform instance
     */
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
            System.err.println("resolve failure for client " + getInstanceName() + " : ASM instance unkown");
            return;
        }

        Apam apam = getFactory().getApam();
        if (apam == null) {
            System.err.println("resolve failure for client " + getInstanceName() + " : APAM not found");
            return;
        }

        System.err.println("resolving " + getInstanceName() + " dependency " + dependency.getName());
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
     * Notify instance activation/deactivation
     */
    @Override
    public void setState(int state) {
        super.setState(state);

        if (isApamCreated || !isStarted())
            return;

        if (state == ComponentInstance.VALID)
            Apform2Apam.newInstance(getInstanceName(), this);

        if (state == ComponentInstance.INVALID)
            Apform2Apam.vanishInstance(getInstanceName());
    }

    /**
     * Apform: get the properties of the instance
     */
    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();

        // Get access to the iPojo configuration handler managed properties
        ConfigurationHandlerDescription configurationDescription =
                (ConfigurationHandlerDescription) getInstanceDescription().getHandlerDescription("properties");

        if (configurationDescription == null)
            return properties;

        for (PropertyDescription property : configurationDescription.getProperties()) {
            properties.put(property.getName(), property.getObjectValue(getContext()));
        }

        return properties;
    }

    /**
     * Apform: get name of the instance
     */
    @Override
    public String getName() {
        return getInstanceName();
    }

    /**
     * Apform: get the service object of the instance
     */
    @Override
    public Object getServiceObject() {
        return getPojoObject();
    }

    /**
     * Apform: get the name of the implementation of the instance
     */
    @Override
    public String getImplemName() {
        return getFactory().getName();
    }

    /**
     * Apform: resolve dependency
     */
    @Override
    public boolean setWire(Instance destInst, String depName) {
        System.err.println("Native instance set wire " + depName + " :" + getInstanceName() + "->" + destInst);
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
        System.err.println("Native instance rem wire " + depName + " :" + getInstanceName() + "->" + destInst);
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
        System.err.println("Native instance subs wire " + depName + " :" + getInstanceName() + "from ->" + oldDestInst
                + " to ->" + newDestInst);
        Dependency dependency = dependencies.get(depName);

        if (dependency == null)
            return false;

        dependency.substituteTarget(oldDestInst, newDestInst);
        return true;
    }

}
