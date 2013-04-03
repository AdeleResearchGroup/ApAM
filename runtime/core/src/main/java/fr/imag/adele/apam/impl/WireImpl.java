/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.impl;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Wire;

public class WireImpl implements Wire {
    private final InstanceImpl source;
    private final InstanceImpl destination;
    private final String       depName;    // field name for atomic dep; spec name for complex dep, dest type for
    private final boolean 	   hasConstraints ;
    private final boolean      isWire ;

    /**
     * Warning, only creates a Wire object. Does not chain that wire in the client and provider.
     * Must be called only from createWire in InstanceImpl.
     * @param from
     * @param to
     * @param depName
     */
    public WireImpl(Instance from, Instance to, String depName, boolean hasConstraints, boolean wire) {
        source = (InstanceImpl) from;
        destination = (InstanceImpl) to;
        this.isWire = wire ;
        this.depName = depName;
        this.hasConstraints = hasConstraints ;
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

    @Override
    public boolean hasConstraints() {
        return hasConstraints;
    }

    public void remove() {
        source.removeWire(this);
        destination.removeInvWire(this);
    }
    

    

}
