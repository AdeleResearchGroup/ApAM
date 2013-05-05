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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Relation;
import fr.imag.adele.apam.RelationManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.apam.util.Visible;

public class ApamMan implements RelationManager {

	private BundleContext context;


	@Override
	public String getName() {
		return CST.APAMMAN;
	}

	public ApamMan(){
	}
	public ApamMan(BundleContext context){
		this.context = context;
	}

	// when in Felix.
	public void start() {
		try {
			Util.printFileToConsole(context.getBundle().getResource("logo.txt"));
		} catch (IOException e) {
		}
		System.out.println("APAMMAN started");
	}

	public void stop() {
		System.out.println("APAMMAN stoped");
	}


	@Override
	public void getSelectionPath(Component client, Relation dep, List<RelationManager> selPath) {
	}

	// @Override
	// public Instance resolveImpl(Component client, Implementation impl,
	// Relation dep) {
	// //List<ApamFilter> f = Util.toFilterList(preferences) ;
	// return dep.getPrefered(resolveImpls(client, impl, dep)) ;
	// }
	//
	// @Override
	// public Set<Instance> resolveImpls(Component client, Implementation impl,
	// Relation dep) {
	//
	// //Set<ApamFilter> f = Util.toFilter(constraints) ;
	// Set<Instance> insts = new HashSet<Instance>();
	// for (Instance inst : impl.getInsts()) {
	// if (inst.isSharable() && inst.matchRelationConstraints(dep) &&
	// client.canSee(inst))
	// insts.add(inst);
	// }
	// return insts;
	// }

	@Override
	public int getPriority() {
		return -1;
	}

	@Override
	public void newComposite(ManagerModel model, CompositeType composite) {
	}

	// @Override
	// public Instance findInstByName(Component client, String instName) {
	// if (instName == null) return null;
	// Instance inst = CST.componentBroker.getInst(instName);
	// if (inst == null) return null;
	// if (client.canSee(inst)) {
	// return inst;
	// }
	// return null;
	// }
	//
	// @Override
	// public Implementation findImplByName(Component client, String implName) {
	// if (implName == null)
	// return null;
	// Implementation impl = CST.componentBroker.getImpl(implName);
	// if (client.canSee(impl)) {
	// return impl ;
	// }
	// return null ;
	// }
	//
	// @Override
	// public Specification findSpecByName(Component client, String specName) {
	// if (specName == null)
	// return null;
	// return CST.componentBroker.getSpec(specName);
	// }

	//	@Override
	//	public Component findComponentByName(Component client, String componentName) {
	//		Component ret = CST.componentBroker.getComponent (componentName) ;
	//		if (ret == null) return null ;
	//		if (Visible.isVisible(client, ret)) {
	//			return ret ;
	//		}
	//		return null ;
	//	}

	/**
	 * dep target can be a specification, an implementation or a resource:
	 * interface or message. We have to find out all the implementations and all
	 * the instances that can be a target for that relation and satisfy
	 * visibility and the constraints,
	 * 
	 * First compute all the implementations, visible or not that is a good
	 * target; then add in insts all the instances of these implementations that
	 * satisfy the constraints and are visible.
	 * 
	 * If parameter needsInstances is null, do not take care of the instances.
	 * 
	 * Then remove the implementations that are not visible.
	 * 
	 */
	@Override
	public Resolved<?> resolveRelation(Component source, Relation relation) {

		if (relation.getTargetKind() == ComponentKind.SPECIFICATION) {
			return relation.getResolved(CST.componentBroker.getSpecs());
		}

		/*
		 * The target is Implementation or Instance. First try to find out the implems that satisfy the resources
		 * without constraints nor visibility control 
		 */
		Set<Implementation> impls = null ;
		String name = relation.getTarget().getName();

		if (relation.getTarget() instanceof SpecificationReference) {
			Specification spec = CST.componentBroker.getSpec(name);
			if (spec == null) {
				System.err.println("No spec with name " + name
				// + " for relation " + relation.getIdentifier()
						+ " from component" + source);
				return null;
			}
			impls = spec.getImpls();
		} else 	{
			impls = new HashSet<Implementation> () ;
			if (relation.getTarget() instanceof ResourceReference) {
				for (Implementation impl : CST.componentBroker.getImpls()) {
					if (impl.getDeclaration()
							.getProvidedResources()
							.contains(
									((ResourceReference) relation.getTarget()))) {
						impls.add(impl) ;
					}
				}
			} else {
				if (relation.getTarget() instanceof ImplementationReference) {
					Implementation impl = CST.componentBroker.getImpl(name);
					if (impl != null) {
						impls.add(impl) ;
					} 
				} else if (relation.getTarget() instanceof ComponentReference<?>) {
					Component component = CST.componentBroker.getComponent(name);
					if (component != null) {
						if (component instanceof Implementation) {
							impls.add((Implementation) component);
						} else if (component instanceof Instance) {
							impls.add(((Instance) component).getImpl());
						} else if (component instanceof Specification) {
							impls.addAll(((Specification) component).getImpls());
						}
					}
				}
			}
		}

		//Not found
		if (impls == null || impls.isEmpty()) 
			return null ;

		//We have the implementations. Select the good one(s)
		if (relation.getTargetKind() == ComponentKind.IMPLEMENTATION) {
			return relation.getResolved(impls);
		}

		/*
		 * We have in impls all the implementations satisfying the relation
		 * target (type and name only). We are looking for instances Take all
		 * the instances of these implementations satisfying the relation
		 * constraints and visibility.
		 */		
		Set<Instance> insts = new HashSet<Instance> () ; 
		//Compute all the instances visible and satisfying the constraints  ;
		for (Implementation impl : impls) {
			for (Instance inst : impl.getInsts()) {
				if (inst.isSharable() 
				// && Visible.isVisible(client, inst)
						&& source.canSee(inst)
						&& inst.matchRelationConstraints(relation)) {
					insts.add(inst) ;
				}
			}
		}

		if (insts == null  ||insts.isEmpty()) 
			return null ;

		/*
		 * If relation is singleton, select the best instance.
		 */
		if (relation.isMultiple())
			return new Resolved<Instance> (insts) ;
		return new Resolved<Instance>(relation.getPrefered(insts));
	}


	@Override
	public void notifySelection(Component client, ResolvableReference resName, String depName, Implementation impl, Instance inst,
			Set<Instance> insts) {
		// do not care
	}

	@Override
	public ComponentBundle findBundle(CompositeType context, String bundleSymbolicName, String componentName) {
		return null;
	}
}