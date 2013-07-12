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

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.Relation;

public class LinkImpl implements Link {
    private final ComponentImpl source;
    private final ComponentImpl destination;
	private final boolean hasConstraints;
	private final boolean isPromotion;
	private final Relation definition ;
//	private final String depName; // field name for atomic dep; spec name for complex dep, dest type for
//	private final boolean isWire;
//	private final boolean isInjected;

    /**
     * Warning, only creates a Link object. Does not chain that link in the client and provider.
     * Must be called only from createLink in ComponentImpl.
     * 
     * @param from
     * @param to
     * @param depName
     * @param hasConstraints : true if the relation has constraints
     * @param wire true if it is a wire
     */
	public LinkImpl(Component from, Component to, Relation dep, boolean hasConstraints, boolean isPromotion) {
        source = (ComponentImpl) from;
        destination = (ComponentImpl) to;
        this.hasConstraints = hasConstraints ;
        definition = dep ;
        this.isPromotion = isPromotion ;
//		this.isWire = dep.isWire();
//		this.isInjected = dep.isInjected();
//		this.depName = dep.getIdentifier();
    }

    @Override
    public Component getSource() {
        return source;
    }

    @Override
    public Component getDestination() {
        return destination;
    }

    @Override
    public String getName() {
        return definition.getIdentifier();
    }


    @Override
    public boolean hasConstraints() {
        return hasConstraints;
    }
    @Override
    public boolean isPromotion () {
        return isPromotion;
    }


	@Override
	public boolean isWire() {
		return definition.isWire();
	}

	@Override
	public boolean isInjected() {
		return definition.isInjected();
	}
	
	@Override
    public Relation getDefinition () {
    	return definition ;
    }

    public void remove() {
        source.removeLink(this);
        destination.removeInvLink(this);
    }


	@Override
	public String toString() {
		String ret = "" ;
		if (isInjected()) ret = (isWire()) ? "wire " : " Ilink " ;
		else ret = "link " ;
		return ret + getName() + " from " + source + " to " + destination ;
	}

}
