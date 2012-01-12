package fr.imag.adele.apam.apamImpl;

import java.util.Set;

import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.util.Dependency;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.apam.util.Dependency.AtomicDependency;
import fr.imag.adele.apam.util.Dependency.DependencyKind;
import fr.imag.adele.apam.util.Dependency.ImplementationDependency;
import fr.imag.adele.apam.util.Dependency.TargetKind;

public class Wire {
    private final InstanceImpl source;
    private final InstanceImpl destination;
    private final String       depName;    // field name for atomic dep; spec name for complex dep, dest type for
                                            // composites

    public Wire(Instance from, Instance to, String depName) {
        source = (InstanceImpl) from;
        destination = (InstanceImpl) to;
        this.depName = depName;
    }

    /**
     * Check if this new wire is consistent or not.
     * 
     * @param from
     * @param to
     * @return
     */

    public static boolean checkNewWire(Instance from, Instance to, String depName) {
        if (!Wire.checkDependency(from, to, depName))
            return false;
        return Util.checkInstVisible(from.getComposite(), to);
    }

    public static boolean checkDependency(Instance from, Instance to, String depName) {
        // it should matches a dependency of the "from" implementation.
        boolean found = false;
        if (from instanceof CompositeType)
            return true; // allready checked
        Set<ImplementationDependency> deps = from.getImpl().getImplemDependencies();
        for (Dependency dep : deps) {
            if (Wire.matchDependency(dep, to, depName)) {
                found = true;
                break;
            }
        }
        if (!found) {
            System.err.println("WARNING dependency not declared : " + from + " -" + depName + "-> " + to);
        }
        from.getApformInst().setWire(to, depName);
        return found;
    }

    private static boolean matchDependency(Dependency dep, Instance to, String depName) {
        if (dep.kind == DependencyKind.COMPOSITE)
            return true; // already checked
        if (dep.kind == DependencyKind.IMPLEMENTATION) { // does not check source
            for (AtomicDependency adep : ((ImplementationDependency) dep).dependencies) {
                if (Wire.matchAtomicDependency(adep.targetKind, adep.fieldName, to, depName) == true)
                    return true;
            }
            return false;
        }
        System.err.println("Invalid dependency type : " + dep);
        return false;
    }

    private static boolean matchAtomicDependency(TargetKind target, String fieldName, Instance to, String depName) {
        switch (target) {
            case INTERFACE: { // "to" must match the target
                for (String interf : to.getSpec().getInterfaceNames()) {
                    if (interf.equals(fieldName)
                                && depName.equals(fieldName))
                        return true;
                }
                return false;
            }
            case SPECIFICATION: {
                if (to.getSpec().getName().equals(fieldName)
                            && depName.equals(fieldName))
                    return true;
                return false;
            }
            case PULL_MESSAGE: {
                if (to.getImpl().getName().equals(fieldName)
                            && depName.equals(fieldName))
                    return true;
                return false;
            }
            case PUSH_MESSAGE: {
                if (to.getImpl().getName().equals(fieldName)
                            && depName.equals(fieldName))
                    return true;
                return false;
            }
        }
        return false;
    }

    public Instance getSource() {
        return source;
    }

    public Instance getDestination() {
        return destination;
    }

    public String getDepName() {
        return depName;
    }

    public void remove() {
        source.removeWire(this);
        destination.removeInvWire(this);
//        if (source.getDepHandler() != null) {
//            source.getDepHandler().remWire(destination, depName);
//        }
    }

}
