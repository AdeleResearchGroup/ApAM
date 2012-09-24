package fr.imag.adele.apam.impl;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Wire;

public class WireImpl implements Wire {
    private final InstanceImpl source;
    private final InstanceImpl destination;
    private final String       depName;    // field name for atomic dep; spec name for complex dep, dest type for

    public WireImpl(Instance from, Instance to, String depName) {
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


    @Override
    public Instance getSource() {
        return source;
    }

    @Override
    public Instance getDestination() {
        return destination;
    }

    @Override
    public String getDepName() {
        return depName;
    }

    protected void remove() {
        source.removeWire(this);
        destination.removeInvWire(this);
    }
}
