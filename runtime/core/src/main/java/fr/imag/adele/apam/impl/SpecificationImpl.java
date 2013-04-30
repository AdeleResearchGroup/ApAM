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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.declarations.ComponentKind;


public class SpecificationImpl extends ComponentImpl implements Specification {

	private static final long serialVersionUID = -2752578219337076677L;

	private final Set<Implementation> implementations = Collections
			.newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());

//	/*
//	 * All relation requires, derived from all the used implementations
//	 */
//	private final Set<Specification>  requires        = Collections
//			.newSetFromMap(new ConcurrentHashMap<Specification, Boolean>());
//
//	/*
//	 * All reverse requires, the opposite of requires
//	 */
//	private final Set<Specification>  invRequires     = Collections
//			.newSetFromMap(new ConcurrentHashMap<Specification, Boolean>());  // all


	protected SpecificationImpl(ApformSpecification apfSpec) throws InvalidConfiguration {
		super(apfSpec);
	}


	@Override
	public void register(Map<String,String> initialProperties) throws InvalidConfiguration {
		/*
		 * Terminates the initalisation, and computes properties
		 */
		finishInitialize(initialProperties) ;

		/*
		 * Add to broker
		 */
		((ComponentBrokerImpl) CST.componentBroker).add(this);

		/*
		 * Notify managers
		 *
		 * Add call back to add specification?
		 */
		ApamManagers.notifyAddedInApam(this) ;
	}

	@Override
	public void unregister() {

		/*
		 * Remove all implementations providing this specification
		 *
		 * TODO Is this really necessary? We should consider the special case of
		 * updates because we probably can reduce the impact of the modification.
		 */
		for (Implementation impl : implementations) {
			ComponentBrokerImpl.disappearedComponent(impl) ;
		}

		/*
		 * TODO What to do with implementations that reference this specification
		 * in its dependencies?
		 */

		/*
		 * remove from broker
		 */
		//((ComponentBrokerImpl)CST.componentBroker).remove(this);

	}

	@Override
	public ApformSpecification getApformSpec() {
		return (ApformSpecification)getApformComponent();
	}

	public void addImpl(Implementation impl) {
		implementations.add(impl);
	}

	@Override
	public Implementation getImpl(String name) {
		if (name == null)
			return null;
		for (Implementation impl : implementations) {
			if (impl.getName().equals(name))
				return impl;
		}
		return null;
	}


//	// relation requires control
//	public void addRequires(Specification dest) {
//		if (requires.contains(dest))
//			return;
//		requires.add(dest);
//		((SpecificationImpl) dest).addInvRequires(this);
//	}
//
//	public void removeRequires(Specification dest) {
//		for (Implementation impl : implementations) {
//			for (Implementation implDest : impl.getUses())
//				if (implDest.getSpec() == dest) {
//					return; // it exists another instance that uses that destination. Do nothing.
//				}
//		}
//		requires.remove(dest);
//		((SpecificationImpl) dest).removeInvRequires(this);
//	}
//
//	private void addInvRequires(Specification orig) {
//		invRequires.add(orig);
//	}
//
//	private void removeInvRequires(Specification orig) {
//		invRequires.remove(orig);
//	}
//
//	@Override
//	public Set<Specification> getRequires() {
//		return Collections.unmodifiableSet(requires);
//	}
//
//	@Override
//	public Set<Specification> getInvRequires() {
//		return Collections.unmodifiableSet(invRequires);
//	}


	protected void removeImpl(Implementation impl) {
		implementations.remove(impl);
	}

	@Override
	public Set<Implementation> getImpls() {
		return Collections.unmodifiableSet(implementations);
	}

	@Override
	public Set<? extends Component> getMembers() {
		return Collections.unmodifiableSet(implementations);
	}

	@Override
	public Component getGroup() {
		return null;
	}


	@Override
	public ComponentKind getKind() {
		return ComponentKind.SPECIFICATION ;
	}
}

