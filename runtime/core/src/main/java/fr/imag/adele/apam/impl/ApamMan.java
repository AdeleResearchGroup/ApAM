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
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.RelationManager;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceReference;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.util.Util;

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
	public void getSelectionPath(Component client, RelToResolve dep, List<RelationManager> selPath) {
	}

	@Override
	public int getPriority() {
		return -1;
	}

	@Override
	public void newComposite(ManagerModel model, CompositeType composite) {
	}

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
	public Resolved<?> resolveRelation(Component source, RelToResolve relToResolve) {

		Set<Implementation> impls = null ;
		String name = relToResolve.getTarget().getName();

		/*
		 * First analyze the component references
		 */
		if (relToResolve.getTarget() instanceof SpecificationReference) {
			Specification spec = CST.componentBroker.getSpec(name);
			if (spec == null) {
				//logger.debug("No spec with name " + name + " from component" + source);
				return null;
			}
			if (relToResolve.getTargetKind() == ComponentKind.SPECIFICATION) {
				return new Resolved<Specification> (spec) ;
			}
			impls = spec.getImpls();
		} else 	if (relToResolve.getTarget() instanceof ImplementationReference) {
			Implementation impl = CST.componentBroker.getImpl(name);
			if (impl == null) {
				return null;
			}
			if (relToResolve.getTargetKind() == ComponentKind.IMPLEMENTATION) {
				return new Resolved<Implementation> (impl) ;
			}
			impls = new HashSet<Implementation> () ;
			impls.add(impl) ;
		} else if  (relToResolve.getTarget() instanceof InstanceReference) {
			Instance inst = CST.componentBroker.getInst(name);
			if (inst == null) {
				return null;
			}
			if (relToResolve.getTargetKind() == ComponentKind.INSTANCE) {
				return new Resolved<Instance> (inst) ;
			}
			return null ;
		} else if (relToResolve.getTarget() instanceof ComponentReference<?>) {
			System.err.println("Invalid target reference : "+relToResolve.getTarget());
			return null ;
		}


		/*
		 * We have computed all component references
		 * It is either already resolved, or the implems are in impls.
		 * Now Resolve by resource.
		 */
		else if (relToResolve.getTarget() instanceof ResourceReference) {
			if (relToResolve.getTargetKind() == ComponentKind.SPECIFICATION) {
				Set <Specification> specs = new HashSet<Specification> () ;
				for (Specification spec : CST.componentBroker.getSpecs()) {
					if (spec.getDeclaration().getProvidedResources().contains(
							((ResourceReference) relToResolve.getTarget()))) {
						specs.add(spec) ;
					}
				}
				return relToResolve.getResolved(specs,false);
			}
			
			/*
			 * target Kind is implem or instance
			 * get all the implems that implement the resource
			 */
			impls = new HashSet<Implementation> () ;
			for (Implementation impl : CST.componentBroker.getImpls()) {
				if (impl.getDeclaration().getProvidedResources().contains(
						((ResourceReference) relToResolve.getTarget()))) {
					impls.add(impl) ;
				}
			}
		} 

		//TargetKind is implem or instance, but no implem found.
		if (impls == null || impls.isEmpty()) 
			return null ;

		//If TargetKind is implem, select the good one(s)
		if (relToResolve.getTargetKind() == ComponentKind.IMPLEMENTATION) {
			return relToResolve.getResolved(impls, false);
		}

		/*
		 * We have in impls all the implementations satisfying the relation
		 * target (type and name only). We are looking for instances. 
		 * Take all the instances of these implementations satisfying the relation
		 * constraints and visibility.
		 */
		
		//Fast track : if looking for one instance and no preferences, return the first valid instance
		boolean fast = 	(! relToResolve.isMultiple() && ! relToResolve.hasPreferences())  ;

		Set<Instance> insts = new HashSet<Instance> () ; 
		for (Implementation impl : impls) {
			for (Instance inst : impl.getInsts()) {
				if (inst.isSharable() 
						&& source.canSee(inst)
						&& inst.matchRelationConstraints(relToResolve)) {
					if (fast) 
						return new Resolved<Instance> (inst) ;
					insts.add(inst) ;
				}
			}
		}

		if (!insts.isEmpty()) {
			/*
			 * If relation is singleton, select the best instance.
			 */
			if (relToResolve.isMultiple())
				return new Resolved<Instance> (insts) ;
			return new Resolved<Instance>(relToResolve.getPrefered(insts));
		}
		
			/*
			 *  Keep only the implementations satisfying the constraints of the relation
			 */
			Set<Implementation> valid = new HashSet<Implementation> ();
			for (Implementation impl : impls) {
				if (relToResolve.matchRelationConstraints(ComponentKind.IMPLEMENTATION, impl.getAllProperties()))
					valid.add(impl) ;
			}
			if (valid.isEmpty()) 
				return null ;

			return new Resolved <Instance> (relToResolve.getPrefered(valid), true);
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