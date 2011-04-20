package fr.imag.adele.apam;

import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.samAPIImpl.ASMInstImpl;
import fr.imag.adele.apam.util.Attributes;

public class Wire {
    ASMInstImpl source;
    ASMInstImpl destination;
    String      depName;

    public Wire(ASMInst from, ASMInst to, String depName) {
        if (Wire.checkNewWire(from, to)) {
            source = (ASMInstImpl) from;
            destination = (ASMInstImpl) to;
            this.depName = depName;
        }
    }

    /**
     * Check if this new wire is consistent or not. Check the shared property, Check if composite are correctly set.
     * Checks if the wire is already existing. • D.shared = shareable. D est utilisable par tout autre objet O. •
     * D.shared = Appli. D n’est utilisable par O que si CO.appli == CD.appli. • D.shared = Composite. D n’est
     * utilisable par O que si CO==CD ou CO-depend-CD. • D.shared = local. D n’est utilisable par O que si CO==CD. •
     * D.shared = private. D n’est utilisable par O que si CO==CD et O’ / O’ utilise D.
     * 
     * @param from
     * @param to
     * @return
     */
    public static boolean checkSpecAccess(ASMSpec spec, Composite compoFrom) {
        String shared = (String) spec.getProperty(Attributes.SHARED);

        if ((shared == null) || (shared.equals(Attributes.SHARABLE)))
            return true;

        if (shared.equals(Attributes.APPLI))
            return (compoFrom.getApplication() == spec.getComposite().getApplication());

        if (shared.equals(Attributes.COMPOSITE))
            return ((compoFrom == spec.getComposite()) || (compoFrom.dependsOn(spec.getComposite())));

        if (shared.equals(Attributes.LOCAL))
            return (compoFrom == spec.getComposite());

        if (shared.equals(Attributes.PRIVATE))
            return (spec.getInvRequires().size() == 0);

        System.out.println("Invalid Shared value :  " + shared);
        return false;
    }

    public static boolean checkImplAccess(ASMImpl impl, Composite compoFrom) {
        String shared = (String) impl.getProperty(Attributes.SHARED);

        if ((shared == null) || (shared.equals(Attributes.SHARABLE)))
            return true;

        if (shared.equals(Attributes.APPLI))
            return (compoFrom.getApplication() == impl.getComposite().getApplication());

        if (shared.equals(Attributes.COMPOSITE))
            return ((compoFrom == impl.getComposite()) || (compoFrom.dependsOn(impl.getComposite())));

        if (shared.equals(Attributes.LOCAL))
            return (compoFrom == impl.getComposite());

        if (shared.equals(Attributes.PRIVATE))
            return (impl.getInvUses().size() == 0);

        System.out.println("Invalid Shared value :  " + shared);
        return false;
    }

    public static boolean checkInstAccess(ASMInst inst, Composite compoFrom) {
        String shared = (String) inst.getProperty(Attributes.SHARED);

        if ((shared == null) || (shared.equals(Attributes.SHARABLE)))
            return true;

        if (shared.equals(Attributes.APPLI))
            return (compoFrom.getApplication() == inst.getComposite().getApplication());

        if (shared.equals(Attributes.COMPOSITE))
            return ((compoFrom == inst.getComposite()) || (compoFrom.dependsOn(inst.getComposite())));

        if (shared.equals(Attributes.LOCAL))
            return (compoFrom == inst.getComposite());

        if (shared.equals(Attributes.PRIVATE))
            return (inst.getInvWires().size() == 0);

        System.out.println("Invalid Shared value :  " + shared);
        return false;

    }

    public static boolean checkNewWire(ASMInst from, ASMInst to) {
        return Wire.checkInstAccess(to, from.getComposite());
    }

    public ASMInst getSource() {
        return source;
    }

    public ASMInst getDestination() {
        return destination;
    }

    public String getDepName() {
        return depName;
    }

    public void remove() {
        source.removeWire(this);
        destination.removeInvWire(this);
        if (source.getDepHandler() != null) {
            source.getDepHandler().remWire(destination, depName);
        }
    }

}
