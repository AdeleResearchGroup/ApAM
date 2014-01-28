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

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.DynamicManager;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.RelationDefinition;

public class LinkImpl implements Link {
	private final ComponentImpl source;
	private final ComponentImpl destination;
	private final boolean hasConstraints;
	private final boolean isPromotion;
	private final RelToResolve relToResolve;

	// private final String depName; // field name for atomic dep; spec name for
	// complex dep, dest type for
	// private final boolean isWire;
	// private final boolean isInjected;

	/**
	 * Warning, only creates a Link object. Does not chain that link in the
	 * client and provider. Must be called only from createLink in
	 * ComponentImpl.
	 * 
	 * @param from
	 * @param to
	 * @param depName
	 * @param hasConstraints
	 *            : true if the relation has constraints
	 * @param wire
	 *            true if it is a wire
	 */
	public LinkImpl(Component from, Component to, RelToResolve dep, boolean hasConstraints, boolean isPromotion) {
		source = (ComponentImpl) from;
		destination = (ComponentImpl) to;
		this.hasConstraints = hasConstraints;
		relToResolve = dep;
		this.isPromotion = isPromotion;
		// this.isWire = dep.isWire();
		// this.isInjected = dep.isInjected();
		// this.depName = dep.getIdentifier();
	}

	@Override
	public Component getDestination() {
		return destination;
	}

	@Override
	public String getName() {
		return relToResolve.getName();
	}

	@Override
	public RelationDefinition getRelDefinition() {
		return relToResolve.getRelationDefinition();
	}

	@Override
	public RelToResolve getRelToResolve() {
		return relToResolve;
	}

	@Override
	public Component getSource() {
		return source;
	}

	@Override
	public boolean hasConstraints() {
		return hasConstraints;
	}

	@Override
	public boolean isInjected() {
		return relToResolve.isInjected();
	}

	@Override
	public boolean isPromotion() {
		return isPromotion;
	}

	@Override
	public boolean isWire() {
		return relToResolve.isWire();
	}

	@Override
	public void reevaluate(boolean force, boolean eager) {
		if (force || (hasConstraints() && !this.getRelToResolve().matchRelationConstraints(this.getDestination()))) {
			remove();
			if (eager) {
				CST.apamResolver.resolveLink(getSource(), getRelDefinition());
			}
		}
	}

	/**
	 * Check if the current link is valid. Makes all the validations.
	 * @return
	 */
	public boolean isValid () {
		if (!this.isPromotion && ! source.canSee(destination)) {
			//logger.error("CreateLink: Source  " + this + " does not see its target " + to);
			return false;
		}
		RelToResolve rel = getRelToResolve() ;
		if (source.getKind() != rel.getSourceKind()) {
//			logger.error("CreateLink: Source kind " + getKind()
//					+ " is not compatible with relation sourceType "
//					+ dep.getSourceKind());
			return false;
		}
		
		if (destination.getKind() != rel.getTargetKind()) {
//			logger.error("CreateLink: Target kind " + to.getKind()
//					+ " is not compatible with relation targetType "
//					+ dep.getTargetKind());
			return false;
		}
		
		if (hasConstraints && !rel.matchRelationConstraints(destination)) {
//			logger.error("CreateLink: Target does not satisfies the constraints" );
			return false ;
		}
		return true ;
	}
	
	@Override
	public void remove() {
		source.removeLink(this);
		destination.removeInvLink(this);

		// Notify Dynamic managers that a link has been deleted. A new
		// resolution can be possible now.
		for (DynamicManager manager : ApamManagers.getDynamicManagers()) {
			manager.removedLink(this);
		}

	}

	@Override
	public String toString() {
		String ret = "";
		if (isInjected()) {
			ret = (isWire()) ? "wire " : " Ilink ";
		} else {
			ret = "link ";
		}
		return ret + getName() + " from " + source + " to " + destination;
	}

}
