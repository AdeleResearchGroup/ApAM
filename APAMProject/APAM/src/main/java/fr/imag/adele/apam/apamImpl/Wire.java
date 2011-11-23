package fr.imag.adele.apam.apamImpl;

import java.util.Set;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Implementation.DependencyModel;
import fr.imag.adele.apam.util.Util;

public class Wire {
    private final InstanceImpl source;
    private final InstanceImpl destination;
    private final String       depName;

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
        Set<DependencyModel> deps = from.getImpl().getDependencies();
        for (DependencyModel dep : deps) {
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

//        // if it matches a dependency of the current composite, it is a promotion
//        DependencyModel depFound = null;
//        deps = from.getComposite().getCompType().getDependencies();
//        for (DependencyModel dep : deps) {
//            if (Wire.matchDependencyCompo(dep, from, to, depName)) {
//                depFound = dep;
//                break;
//            }
//        }
//        if (depFound == null)
//            return true;
//        
//        // it is a declared promotion.
//        // check cardinality
//        if (!depFound.isMultiple && (from.getComposite().getWire(to, depFound.dependencyName) != null)) {
//            System.err.println("ERROR : wire " + from.getComposite() + " -" + depFound.dependencyName + "-> " + to
//                    + " allready existing.");
//            return false;
//        }
//        System.out.println("Promoting " + from + " : " + from.getComposite() + " -" + depFound.dependencyName + "-> "
//                + to);
//        
//        //The destination implem, if inside current composieType, must be moved to the from.compositeType
//        //the destination instance, if instantiated inside the current composite, must be moved to the from.composite 
//        
//        // create wire from from.composite to destination implem (or composite)
//        from.getComposite().createWire(to, depFound.dependencyName);
//        // and set the wire from the promoted implementation.
//
//    return from.getApformInst().setWire(to, depName);
//}

//    /**
//     * Checks if the provided composite dependency "dep" matches the provided wire (from, to, depName).
//     * "from" is supposed to be inside the composite.
//     * if true this wire need a promotion.
//     * 
//     * @param dep : a composite dependency. Not interpreted for composites.
//     * @param from
//     * @param to
//     * @param depName
//     * @return
//     */
//    private static boolean matchDependencyCompo(DependencyModel dep, Instance from, Instance to) {
//        String fromSpec = from.getSpec().getName();
//        switch (dep.targetKind) {
//            case INTERFACE: { // "to" must match the target
//                for (String interf : to.getSpec().getInterfaceNames()) {
//                    if (interf.equals(dep.target)) { // same target
//                        for (String sourceSpec : dep.source) {
//                            if (sourceSpec.equals(fromSpec))
//                                return true;
//                        }
//                    }
//                }
//            }
//            case SPECIFICATION: {
//                if (to.getSpec().getName().equals(dep.target)) {
//                    for (String sourceSpec : dep.source) {
//                        if (sourceSpec.equals(fromSpec))
//                            return true;
//                    }
//                }
//            }
//            case IMPLEMENTATION: {
//                if (to.getImpl().getName().equals(dep.target)) {
//                    for (String sourceSpec : dep.source) {
//                        if (sourceSpec.equals(fromSpec))
//                            return true;
//                    }
//                }
//            }
//        }
//        return false;
//    }

    private static boolean matchDependency(DependencyModel dep, Instance from, Instance to, String depName) {
        switch (dep.targetKind) {
            case INTERFACE: { // "to" must match the target
                for (String interf : to.getSpec().getInterfaceNames()) {
                    if (interf.equals(dep.target)
                            && depName.equals(dep.dependencyName))
                        return true;
                }
            }
            case SPECIFICATION: {
                if (to.getSpec().getName().equals(dep.target)
                        && depName.equals(dep.dependencyName))
                    return true;
            }

            case IMPLEMENTATION: {
                if (to.getImpl().getName().equals(dep.target)
                        && depName.equals(dep.dependencyName))
                    return true;
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
