package fr.imag.adele.apam.apamImpl;

import java.util.Set;

import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.SpecificationDeclaration;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.Util;

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
        boolean found = Wire.matchDependency(from.getImpl().getImplDeclaration().getDependencies(), to, depName);
        //    }
        //        
        //        if (from instanceof CompositeType)
        //            return true; // allready checked
        //        Set<ImplementationDependency> deps = from.getImpl().getImplemDependencies();
        //        for (Dependency dep : deps) {
        //            if (Wire.matchDependency(dep, to, depName)) {
        //                found = true;
        //                break;
        //            }
        //        }
        if (!found) {
            System.err.println("WARNING dependency not declared : " + from + " -" + depName + "-> " + to);
        }
        from.getApformInst().setWire(to, depName);
        return found;
    }

    private static boolean matchDependency(Set<DependencyDeclaration> deps, Instance to, String depName) {
        for (DependencyDeclaration dependency : deps) {
            //        }
            //        if (dep.kind == DependencyKind.COMPOSITE)
            //            return true; // already checked
            //        if (dep.kind == DependencyKind.IMPLEMENTATION) { // does not check source
            //            for (AtomicDependency adep : ((ImplementationDependency) dep).dependencies) {
            if (Wire.matchAtomicDependency(dependency, to, depName) == true)
                return true;
        }
        return false;
    }

    private static boolean matchAtomicDependency(DependencyDeclaration dependency, Instance to, String depName) {
        if (!dependency.getName().equals(depName) ) return false ;

        if (dependency.getResource() instanceof SpecificationReference) {
            if (to.getSpec().getName().equals(dependency.getResource().getName())) return true ;
            return false ;
        }
        if (to.getSpec().getDeclaration().getProvidedResources().contains(dependency.getResource()))
            return true;
        return false ;
    }

//
    //        switch (target) {
    //            case INTERFACE: { // "to" must match the target
    //                for (String interf : to.getSpec().getInterfaceNames()) {
    //                    if (interf.equals(fieldName)
    //                            && depName.equals(fieldName))
    //                        return true;
    //                }
    //                return false;
    //            }
    //            case SPECIFICATION: {
    //                if (to.getSpec().getName().equals(fieldName)
    //                        && depName.equals(fieldName))
    //                    return true;
    //                return false;
    //            }
    //            case PULL_MESSAGE: {
    //                if (to.getImpl().getName().equals(fieldName)
    //                        && depName.equals(fieldName))
    //                    return true;
    //                return false;
    //            }
    //            case PUSH_MESSAGE: {
    //                if (to.getImpl().getName().equals(fieldName)
    //                        && depName.equals(fieldName))
    //                    return true;
    //                return false;
    //            }
    //        }
    //        return false;
    //    }

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
