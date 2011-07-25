package fr.imag.adele.apam;

import fr.imag.adele.apam.ASMImpl.ASMInstImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.Util;

public class Wire {
    private ASMInstImpl source;
    private ASMInstImpl destination;
    private String      depName;

    public Wire(ASMInst from, ASMInst to, String depName) {
        if (Wire.checkNewWire(from.getComposite(), to)) {
            source = (ASMInstImpl) from;
            destination = (ASMInstImpl) to;
            this.depName = depName;
        }
    }

    /**
     * Check if this new wire is consistent or not.
     * 
     * @param from
     * @param to
     * @return
     */

    public static boolean checkNewWire(Composite compositeFrom, ASMInst to) {
        return Util.checkInstVisible(to, compositeFrom);
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
