package fr.imag.adele.apam.impl;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Wire;

public class WireImpl implements Wire {
    private final InstanceImpl source;
    private final InstanceImpl destination;
    private final String       depName;    // field name for atomic dep; spec name for complex dep, dest type for

    /**
     * Warning, only creates a Wire object. Does not chain that wire in the client and provider.
     * Must be called only from createWire in InstanceImpl.
     * @param from
     * @param to
     * @param depName
     */
    public WireImpl(Instance from, Instance to, String depName) {
        source = (InstanceImpl) from;
        destination = (InstanceImpl) to;
        this.depName = depName;
    }

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
