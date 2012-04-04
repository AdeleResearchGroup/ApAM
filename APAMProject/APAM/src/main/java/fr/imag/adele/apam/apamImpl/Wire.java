package fr.imag.adele.apam.apamImpl;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.core.DependencyDeclaration;
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
    	DependencyDeclaration dependency = from.getImpl().getImplDeclaration().getDependency(depName);
    	boolean found = (dependency != null && to.getSpec().getDeclaration().resolves(dependency));

    	if (!found) {
            System.err.println("WARNING dependency not declared : " + from + " -" + depName + "-> " + to);
        }
        from.getApformInst().setWire(to, depName);
        return found;
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
