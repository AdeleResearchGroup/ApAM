package fr.imag.adele.apam;

//import java.util.Properties;
import java.util.List;
import java.util.Map;
import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.apam.apamImpl.Dependency.ImplementationDependency;
import fr.imag.adele.apam.apform.ApformImplementation;
//import fr.imag.adele.apam.util.Attributes;

//import fr.imag.adele.sam.Implementation;

public interface Implementation extends ConcurrentMap<String, Object> {

    /**
     * Returns all the composite type that contains this implementation.
     * An implementation is contained in all the composite types that deployed (logically or physically) it.
     * 
     * @return
     */
    public Set<CompositeType> getInCompositeType();

    public String getName();

    /**
     * return the apfform implementation associated with this implementation (same name)
     * 
     * @return
     */
    public ApformImplementation getApformImpl();

    /**
     * return the dependencies associated with this implementation (same name)
     * 
     * @return
     */
    public Set<ImplementationDependency> getImplemDependencies();

    /**
     * return the value of the shared attribute
     * 
     * @return
     */
    public String getShared();

    /**
     * returns the vazlue of the scope attribute
     * 
     * @return
     */
    public String getVisible();

    public void remove();

    /**
     * Creates an instance of that implementation, and initialize its properties with the set of provided properties.
     * The actual new service properties are those provided plus those found in the associated sam implementation, plus
     * those in the associated specification.
     * <p>
     * 
     * @param initialproperties the initial properties
     * @return the instance
     */
    public Instance createInst(Composite compo, Map<String, Object> initialproperties);

    public boolean match(Filter goal);

    /**
     * @return the specification that this ASMImpls implements
     */
    public Specification getSpec();

    /**
     * Returns the implementation currently used by this implementation.
     * 
     * @return the implementation that this ASMImpl requires.
     */
    public Set<Implementation> getUses();

    /**
     * Returns the implementation currently using this implementation.
     * 
     * @return the implementation that use this ASMImpl .
     */
    public Set<Implementation> getInvUses();

    /**
     * Returns the instance (ASMInsts)of that implementation having that name.
     * <p>
     * There is no constraint that an service instance has an Id.
     * 
     * @param name the name
     * @return the service instance
     */
    public Instance getInst(String name);

    /**
     * Returns an instance arbitrarily selected (ASMInsts) of that service implementation Null if not instance are
     * existing.
     * 
     * @return An instance of that service implementation or null if not existing.
     */
    public Instance getInst();

    /**
     * Returns all the instances (ASMInsts) of that service implementation. Empty if not existing.
     * 
     * @return All instances of that service implementation.
     */

    public Set<Instance> getInsts();

    /**
     * Returns all the sharable instances (ASMInsts) of that service implementation. Empty if not existing.
     * 
     * @return All instances of that service implementation.
     */
    public Set<Instance> getSharableInsts();

    /**
     * Returns all the instances of that implementation that satisfy the provided Goal,
     * if existing. Null if not existing.
     * 
     * @param goal the goal
     * @return All instances satisfying the goal
     * @throws InvalidSyntaxException the invalid syntax exception
     */
    public Set<Instance> getInsts(Filter goal) throws InvalidSyntaxException;

    /**
     * Returns all the sharable instances of that implementation that satisfy the provided Goal,
     * 
     * @param goal the goal
     * @return All instances satisfying the goal
     * @throws InvalidSyntaxException the invalid syntax exception
     */
    public Set<Instance> getSharableInsts(Filter goal) throws InvalidSyntaxException;

    /**
     * Returns all the sharable instances of that implementation that satisfy all the filters.
     * 
     * @param constraints. All the constraint that must be satisfied by the instances.
     */
    public Set<Instance> getSharableInsts(Set<Filter> constraints);

    /**
     * Returns all the instances of that implementation that satisfy all the filters.
     * Null if not existing.
     * 
     * @param constraints. All the constraint that must be satisfied by the instances.
     */
    public Set<Instance> getInsts(Set<Filter> constraints);

    /**
     * Returns all the instances of that implementation that satisfy all the filters.
     * Null if not existing.
     * 
     * @param constraints. All the constraint that must be satisfied by the instances.
     * @param preferences. Return the instance that matches the maximum number of constraints, taken in the order
     */
    public Instance getInst(Set<Filter> constraints, List<Filter> preferences);

    /**
     * Returns all the sharable instances of that implementation that satisfy all the filters.
     * Null if not existing.
     * 
     * @param constraints. All the constraint that must be satisfied by the instances.
     * @param preferences. Return the instance that matches the maximum number of constraints, taken in the order
     */

    public Instance getSharableInst(Set<Filter> constraints, List<Filter> preferences);

    /**
     * Among the instance in "candidates", return the instance that matches the maximum number of constraints, taken in
     * the order
     * 
     * @param preferences. The preferences ordered from the most important (first).
     */
    public Instance getPreferedInst(Set<Instance> candidates, List<Filter> preferences);

    /**
     * Checks if is an instantiator.
     * 
     * @return true if method createASMInst is supported
     */
    public boolean isInstantiable();

    public boolean isUsed();

//    /**
//     * A minimal model of the information known by the handler about the potential dependencies
//     * of its managed instance.
//     */
//
//    public enum TargetKind {
//        INTERFACE, SPECIFICATION, IMPLEMENTATION
//    }
//
//    public static class DependencyModel {
//        public String     dependencyName; // depName. field inside source code. Not relevant for composites.
//        public String     target;        // spec name, interface or implem
//        public TargetKind targetKind;    // INTERFACE, SPECIFICATION, IMPLEMENTATION
//        public String[]   source;        // for composites, the list of source specifications.
//        public boolean    isMultiple;    // cardinality multiple
//
//        @Override
//        public String toString() {
//            String val = "Dependency: \n         target: " + targetKind + "  " + target;
//            if (dependencyName != null)
//                val = val + "; field: " + dependencyName;
//            val = val + "; multiple = " + isMultiple;
//
//            if ((source != null) && (source.length > 0)) {
//                val = val + "\n         source specification: ";
//                for (String sc : source) {
//                    val = val + "  " + sc;
//                }
//            }
//            return val + "\n";
//        }
//    }

    /**
     * Get the list of dependencies known by the handler
     */
//    public Set<DependencyModel> getDependencies();

}
