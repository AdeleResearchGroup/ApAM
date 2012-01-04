package fr.imag.adele.apam.apamImpl;

import java.util.Set;

import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apamImpl.Dependency;
import fr.imag.adele.apam.apamImpl.Dependency.AtomicDependency;
import fr.imag.adele.apam.util.Util;

public class Wire {
    private final InstanceImpl source;
    private final InstanceImpl destination;
    private final String       depName;    // field name for atomic dep; spec name for composite dep

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
        Set<Dependency> deps = from.getImpl().getApformImpl().getDependencies();
        for (Dependency dep : deps) {
            if (Wire.matchDependency(dep, from, to, depName)) {
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

    private static boolean matchDependency(Dependency deps, Instance from, Instance to, String depName) {
        if (from instanceof Composite) {
            return true; // already checked
        }
        //
        for (AtomicDependency dep : deps.dependencies)
            switch (dep.targetKind) {
                case INTERFACE: { // "to" must match the target
                    for (String interf : to.getSpec().getInterfaceNames()) {
                        if (interf.equals(dep.fieldName)
                                && depName.equals(dep.fieldName))
                            return true;
                    }
                    return false;
                }
                case SPECIFICATION: {
                    if (to.getSpec().getName().equals(dep.fieldName)
                            && depName.equals(dep.fieldName))
                        return true;
                    return false;
                }
                case PULL_MESSAGE: {
                    if (to.getImpl().getName().equals(dep.fieldName)
                            && depName.equals(dep.fieldName))
                        return true;
                    return false;
                }
                case PUSH_MESSAGE: {
                    if (to.getImpl().getName().equals(dep.fieldName)
                            && depName.equals(dep.fieldName))
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
