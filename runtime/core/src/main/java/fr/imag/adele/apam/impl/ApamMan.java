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
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.RelationManager;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.ImplementationReference;
import fr.imag.adele.apam.declarations.references.components.InstanceReference;
import fr.imag.adele.apam.declarations.references.components.SpecificationReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.util.Util;

public class ApamMan implements RelationManager {

	static Logger logger = LoggerFactory.getLogger(ApamMan.class);

	private BundleContext context;

	//	public ApamMan() {
	//	}

	public ApamMan(BundleContext context) {
		this.context = context;
	}

	@Override
	public String getName() {
		return CST.APAMMAN;
	}


	/**
	 * This is an INTERNAL manager that will be invoked by the core. 
	 * 
	 * So in this method we signal that we are not part of the external handlers to
	 * invoke for this resolution request.
	 * 
	 */
	@Override
	public boolean beginResolving(RelToResolve dep) {
		return false;
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
	public Resolved<?> resolve(RelToResolve relToResolve) {

		Component source = relToResolve.getLinkSource();
		
		Set<Implementation> impls = null;
		String name = relToResolve.getTarget().getName();

		/*
		 * For target by name, wait until the declaration has been processed
		 */
		if (Apform2Apam.isReifying(name) )
			Apform2Apam.waitForComponent(name);

		/*
		 * First analyze the component references
		 */
		if (relToResolve.getTarget() instanceof SpecificationReference) {
			Specification spec = CST.componentBroker.getSpec(name);
			if (spec == null) {
				return null;
			}
			if (relToResolve.getTargetKind() == ComponentKind.SPECIFICATION) {
				return new Resolved<Specification>(spec);
			}
			impls = registered(spec.getImpls());
		} else if (relToResolve.getTarget() instanceof ImplementationReference) {
			Implementation impl = CST.componentBroker.getImpl(name);
			if (impl == null) {
				return null;
			}
			if (relToResolve.getTargetKind() == ComponentKind.IMPLEMENTATION) {
				return new Resolved<Implementation>(impl);
			}
			impls = new HashSet<Implementation>();
			impls.add(impl);
		} else if (relToResolve.getTarget() instanceof InstanceReference) {
			Instance inst = CST.componentBroker.getInst(name);
			if (inst == null) {
				return null;
			}
			if (relToResolve.getTargetKind() == ComponentKind.INSTANCE) {
				return new Resolved<Instance>(inst);
			}
			logger.debug("if (relToResolve.getTarget() instanceof InstanceReference) ") ;
			return null;
		} else if (relToResolve.getTarget() instanceof ComponentReference<?>) {
			logger.error("Invalid target reference : " + relToResolve.getTarget());
			return null;
		}

		/*
		 * We have computed all component references It is either already
		 * resolved, or the implems are in impls. Now Resolve by resource.
		 */
		else if (relToResolve.getTarget() instanceof ResourceReference) {
			if (relToResolve.getTargetKind() == ComponentKind.SPECIFICATION) {
				Set<Specification> specs = new HashSet<Specification>();
				for (Specification spec : CST.componentBroker.getSpecs()) {
					if (spec.getProvidedResources().contains(relToResolve.getTarget())) {
						specs.add(spec);
					}
				}
				return relToResolve.getResolved(specs, false);
			}

			/*
			 * target Kind is implem or instance get all the implems that
			 * implement the resource
			 */
			impls = new HashSet<Implementation>();
			for (Implementation impl : CST.componentBroker.getImpls()) {
				if (impl.getProvidedResources().contains((relToResolve.getTarget()))) {
					impls.add(impl);
				}
			}
		}

		// TargetKind is implem or instance, but no implem found.
		if (impls == null || impls.isEmpty()) {
			return null;
		}

		// If TargetKind is implem, select the good one(s)
		if (relToResolve.getTargetKind() == ComponentKind.IMPLEMENTATION) {
			return relToResolve.getResolved(impls, false);
		}

		/*
		 * We have in impls all the implementations satisfying the relation
		 * target (type and name only). We are looking for instances. Take all
		 * the instances of these implementations satisfying the relation
		 * constraints and visibility.
		 */

		/*
		 *  Fast track : if looking for one instance and no preferences, return the first valid instance
		 */
		boolean fast = (!relToResolve.isMultiple() && !relToResolve.hasPreferences());

		Set<Instance> insts = new HashSet<Instance>();
		for (Implementation impl : impls) {
			for (Instance inst : registered(impl.getInsts())) {
				if (inst.isSharable() && source.canSee(inst) && inst.matchRelationConstraints(relToResolve)) {
					if (fast) {
						return new Resolved<Instance>(inst);
					}
					insts.add(inst);
				}
			}
		}

		if (!insts.isEmpty()) {
			/*
			 * If relation is singleton, select the best instance.
			 */
			if (relToResolve.isMultiple()) {
				return new Resolved<Instance>(insts);
			}
			return new Resolved<Instance>(relToResolve.getPrefered(insts));
		}

		/*
		 * Keep only the implementations satisfying the constraints of the
		 * relation
		 */
		Set<Implementation> valid = new HashSet<Implementation>();
		for (Implementation impl : impls) {
			if (relToResolve.matchRelationConstraints(ComponentKind.IMPLEMENTATION, impl.getAllProperties())) {
				valid.add(impl);
			}
		}

		if (!!!valid.isEmpty()) {
			return new Resolved<Instance>(relToResolve.getPrefered(valid), true);
		}

		/*
		 * In case no solution is found, before to return null ...
		 * If some bundle are starting, they may contain the solution (especially during the starting phase)
		 * Just way for these bundles to complete their starting phase.
		 */
        logger.debug("Checking newBundleArrived()");
        if (newBundleArrived()) {
            logger.debug("a newBundleArrived(), calling resolve again");
			return resolve(relToResolve) ;
		}
		return null ;
	}

	/**
	 * Takes a set of components and filter out those that are not registered in the component broker
	 */
	public <T extends Component> Set<T> registered(Set<T> components) {
		Set<T> registered = new HashSet<T>();
		
		for (T component : components) {
			if (CST.componentBroker.contains(component)) {
				registered.add(component);
			}
		}
		
		return registered;
	}
	
	/**
	 * If some bundle are starting, they may contain the solution (especially during the starting phase)
	 * Just way a short while to let these bundles complete their starting phase.
	 * @return false for there is not starting bundles : no solution.
	 * 		   true if new components have been added : try again
	 */
	private boolean newBundleArrived () {
		if (!!!isStartingBundles()) {
			return false ;
		}
		//Some bundles are starting
		int loop = 0 ;
		try {
			while (isStartingBundles()) {
				loop += 1 ;
				if (loop > 6) {
					logger.error(" looping 7 time 300ms waiting for bundles to start  ... Give up. ");
					return true ;
				}
				//Some bundles are currently starting : wait a bit
				Thread.sleep(300) ;
			}
		} catch (InterruptedException e) { }
		return true ;
	}

	/**
	 * return true if some bundles are in state "starting"
	 * @return
	 */
	private boolean isStartingBundles () {
		for (Bundle bundle : context.getBundles()) {
			if (bundle.getState() == Bundle.STARTING) {
				logger.debug ("Bundle " + bundle.getSymbolicName() + " is starting. Waiting for the bundle to be ready.");
				return true ;
			}
		}
		return false ;
	}


	// when in Felix.
	public void start() {
		// Wait for all bundles to be analyzed
		newBundleArrived() ;
		try {
			Util.printFileToConsole(context.getBundle().getResource("logo.txt"));
		} catch (IOException e) {
		}

		System.out.println("APAMMAN started");
	}

	public void stop() {
		System.out.println("APAMMAN stoped");
	}
}