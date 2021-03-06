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

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.felix.ipojo.Pojo;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.ComponentBroker;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformCompositeType;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.references.ResolvableReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.ApamInstall;

public class ComponentBrokerImpl implements ComponentBroker {

	/**
	 * An special apform specification created only for those specifications
	 * that do not exist in the Apform ipojo layer. Creates a minimal definition
	 * structure.
	 */
	private static class ApamOnlySpecification extends BaseApformComponent<Specification, SpecificationDeclaration> implements ApformSpecification {

		public ApamOnlySpecification(String name, Set<ResourceReference> resources, Map<String, String> properties) {
			super(new SpecificationDeclaration(name));
			declaration.getProvidedResources().addAll(resources);
			if (properties != null) {
				declaration.getProperties().putAll(properties);
			}
		}

	}

	private static Logger logger = LoggerFactory.getLogger(ComponentBrokerImpl.class);
	/*
	 * The three main table to maintain.
	 */
	private final Set<Specification> specifications = Collections.newSetFromMap(new ConcurrentHashMap<Specification, Boolean>());
	private final Set<Implementation> implementations = Collections.newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());

	private final Set<Instance> instances = Collections.newSetFromMap(new ConcurrentHashMap<Instance, Boolean>());

	/**
	 * The task executor. We use a pool of a threads to handle component removal,as this may block as a side-efect and may cause deadlocks
	 * specially at platform shutdown.
	 */
	private final Executor executor = Executors.newCachedThreadPool(new ThreadPoolFactory("unregistration", Executors.defaultThreadFactory()));
	
	/**
	 * The task in charge of performing asynchronous removal 
	 */
	private class UnregisterTask implements Runnable {
	
		private final Component 		component;
		
		public UnregisterTask(Component component) {
			this.component	= component;
		}

		@Override
		public void run() {
			
			/*
			 * Remove the component from the model and notify managers
			 * 
			 * TODO perhaps we should notify managers before actually destroying the component
			 * graph, this will have implications as navigation in the manager may recreate 
			 * deleted edges and have other side-effects
			 */
			((ComponentImpl)component).unregister();
			ApamManagers.notifyRemovedFromApam(component);
		}
	}
	
	/**
	 * Asynchronously destroy a component
	 */
	private void disappearedComponent(Component component) {
		try {
			
			Set<? extends Component> collection = null;
			
			switch (component.getKind()) {
				case IMPLEMENTATION:
					collection = implementations;
					break;
				case INSTANCE:
					collection = instances;
					break;
				case SPECIFICATION:
					collection = specifications;
					break;
				default:
					break;
			}

			if (collection == null) {
				return;
			}

			/*
			 * First remove from the collection, so that is no longer visible for resolution
			 */
			boolean exists = collection.remove(component);
					
			/*
			 * Remove the component from the model and notify managers
			 * 
			 * TODO perhaps we should notify managers before actually destroying the component
			 * graph, this will have implications as navigation in the manager may recreate 
			 * deleted edges and have other side-effects
			 */
			if (exists) {
				executor.execute(new UnregisterTask(component));
			}
			
		}
		catch (Exception unexpected) {
			
		}
	}

	/**
	 * a component disappeared in the platform; we have to delete it from the
	 * ASM.
	 */
	public void disappearedComponent(String componentName) {
		
		Component component = getComponent(componentName);
		if (component == null) {
			return;
		}
		
		disappearedComponent(component);
	}

	void add(Implementation implementation) {
		assert implementation != null && !implementations.contains(implementation);
		implementations.add(implementation);
	}

	void add(Instance instance) {
		assert instance != null && !instances.contains(instance);
		instances.add(instance);
	}

	void add(Specification spec) {
		assert spec != null;
		specifications.add(spec);
	}

	@Override
	public Implementation addImpl(CompositeType composite, ApformImplementation apfImpl) {

		// assert apfImpl != null;

		if (apfImpl == null) {
			logger.error("Error adding implementation:  null Apform instance");
			return null;
		}

		String implementationName = apfImpl.getDeclaration().getName();

		Implementation implementation = getImpl(implementationName);
		if (implementation != null) {
			logger.error("Error adding implementation: already exists" + implementationName);
			return implementation;
		}

		if (composite == null) {
			composite = CompositeTypeImpl.getRootCompositeType();
		}

		/*
		 * Create and register the object in the APAM state model
		 */
		try {

			// create a primitive or composite implementation
			if (apfImpl instanceof ApformCompositeType) {
				implementation = new CompositeTypeImpl(composite, (ApformCompositeType) apfImpl);
			} else {
				implementation = new ImplementationImpl(composite, apfImpl);
			}

			((ImplementationImpl) implementation).register(null);
			return implementation;

		} catch (InvalidConfiguration configurationError) {
			logger.error("Error adding implementation: exception in APAM registration, with message: {}", configurationError.getMessage());
		}
		return null;
	}

	@Override
	public Instance addInst(Composite composite, ApformInstance apfInst) {

		if (apfInst == null) {
			logger.error("Error adding instance: null Apform instance");
			return null;
		}
		String instanceName = apfInst.getDeclaration().getName();
		String implementationName = apfInst.getDeclaration().getImplementation().getName();

		Instance instance = getInst(instanceName);
		if (instance != null) {
			logger.error("Error adding instance: already exists " + instanceName);
			return instance;
		}

		/*
		 * Get the implementation group to create the instance. If the
		 * implementation can not be resolved the creation of the instance
		 * fails.
		 */
		Implementation implementation = getImpl(implementationName);
		if (implementation == null) {
			logger.error("Error adding instance: implementation not found" + implementationName);
			return null;
		}

		if (composite == null) {
			composite = CompositeImpl.getRootAllComposites();
		}

		/*
		 * Create and register the object in the APAM state model
		 */
		try {

			instance = ((ImplementationImpl) implementation).reify(composite, apfInst);
			((InstanceImpl) instance).register(null);
			return instance;

		} catch (Exception instantiationError) {
			
			/*
			 * TODO START CLONE
			 * 
			 * This is a clone of exactly the same code in Implementation.createInstance, we should unify treatment
			 */
			
			/*
			 * TODO this should be done in method register, we should try to have some form
			 * of atomic registration that includes registration and addition in the broker
			 */
			if (instance != null) {
				if (CST.componentBroker.getComponent(instance.getName()) != null) {
					/*
					 * If creation failed after broker registration, undo registration 
					 */
					((ComponentBrokerImpl) CST.componentBroker).disappearedComponent(instance);
				}
				else {
					
					/* 
					 * The instance was partially created, just undo registration
					 */
					((ComponentImpl)instance).unregister();
				}
			}
			
			
			/*
			 * TODO END CLONE
			 */
			
			logger.error("Error adding instance: exception registering instance in APAM ",instantiationError);
		}

		return null;
	}

	@Override
	public Specification addSpec(ApformSpecification apfSpec) {

		if (apfSpec == null) {
			logger.error("Error adding specification: null Apform");
			return null;
		}

		String specificationName = apfSpec.getDeclaration().getName();

		Specification specification = getSpec(specificationName);
		if (specification != null) {
			logger.error("Error adding specification: already exists " + specificationName);
			return specification;
		}

		/*
		 * Create and register the object in the APAM state model
		 */
		try {

			specification = new SpecificationImpl(apfSpec);
			((SpecificationImpl) specification).register(null);
			return specification;

		} catch (InvalidConfiguration configurationError) {
			logger.error("Error adding specification: exception in apam registration", configurationError);
		}

		return null;

	}

	/*
	 * Probably only a bundle remove, that does the same as update. Uninstall a
	 * bundle, and removes all the components inside that bundle.
	 */
	@Override
	public void componentBundleRemove(Component component) {
		if (component == null) {
			return;
		}

		try {
			component.getApformComponent().getBundle().uninstall();
		} catch (BundleException e) {
			logger.error("could not unsintall component " + component.getName());
			e.printStackTrace();
		}
	}

	@Override
	public Implementation createImpl(CompositeType compo, String implName, URL url, Map<String, String> properties) {
		assert implName != null && url != null;

		Implementation impl = getImpl(implName);
		if (impl != null) { // do not create twice
			return impl;
		}
		impl = ApamInstall.intallImplemFromURL(url, implName);
		if (impl == null) {
			logger.error("deployment failed :" + implName + " at URL " + url);
			return null;
		}

		if (compo != null && !((CompositeTypeImpl) compo).isSystemRoot()) {
			((CompositeTypeImpl) compo).deploy(impl);
		}

		return impl;
	}

	@Override
	public Specification createSpec(String specName, Set<ResourceReference> resources, Map<String, String> properties) {

		assert specName != null && resources != null;
		return addSpec(new ApamOnlySpecification(specName, resources, properties));
	}

	@Override
	public Component getComponent(String name) {
		if (name == null) {
			return null;
		}
		for (Specification spec : specifications) {
			if (name.equals(spec.getName())) {
				return spec;
			}
		}
		for (Implementation impl : implementations) {
			if (name.equals(impl.getName())) {
				return impl;
			}
		}
		for (Instance inst : instances) {
			if (name.equals(inst.getName())) {
				return inst;
			}
		}
		return null;
	}

	public boolean contains(Component component) {

		switch (component.getDeclaration().getKind()) {
			case SPECIFICATION:
				return specifications.contains(component);
			case IMPLEMENTATION:
				return implementations.contains(component);
			case INSTANCE:
				return instances.contains(component);
			default:
				return false;
		}
		
	}
	

	@Override
	public Implementation getImpl(String name) {

		if (name == null) {
			return null;
		}

		for (Implementation impl : implementations) {
			if (name.equals(impl.getName())) {
				return impl;
			}
		}
		return null;
	}

	@Override
	public Set<Implementation> getImpls() {
		return Collections.unmodifiableSet(implementations);
	}

	@Override
	public Set<Implementation> getImpls(String goal) {
		if (goal == null) {
			return getImpls();
		}

		Set<Implementation> ret = new HashSet<Implementation>();
		ApamFilter f = ApamFilter.newInstance(goal);
		for (Implementation impl : implementations) {
			if (impl.match(f)) {
				ret.add(impl);
			}
		}
		return ret;
	}

	// /Remove

	@Override
	public Instance getInst(String instName) {
		if (instName == null) {
			return null;
		}
		for (Instance inst : instances) {
			if (inst.getName().equals(instName)) {
				return inst;
			}
		}
		return null;
	}

	@Override
	public Set<Instance> getInsts() {
		return Collections.unmodifiableSet(instances);
	}

	@Override
	public Set<Instance> getInsts(String goal) {
		if (goal == null) {
			return getInsts();
		}
		Set<Instance> ret = new HashSet<Instance>();
		ApamFilter f = ApamFilter.newInstance(goal);
		for (Instance inst : instances) {
			if (inst.match(f)) {
				ret.add(inst);
			}
		}
		return ret;
	}

	// /Add internal to package

	@Override
	public Instance getInstService(Object service) {

		/*
		 * If this is a Apam native component (iPOJO+APAM) then just follow the
		 * chained references
		 */
		if (service instanceof Pojo) {
			Pojo pojo = (Pojo) service;
			if (pojo.getComponentInstance() instanceof ApformInstance) {
				ApformInstance apform = (ApformInstance) pojo.getComponentInstance();
				return apform.getApamComponent();
			}
		}

		// it is a legacy, use brute force
		for (Instance inst : instances) {
			if (inst.getServiceObject() == service) {
				return inst;
			}
		}
		return null;
	}

	@Override
	public Specification getSpec(String name) {
		if (name == null) {
			return null;
		}
		for (Specification spec : specifications) {
			if (name.equals(spec.getName())) {
				return spec;
			}
		}
		return null;
	}

	@Override
	public Specification getSpecResource(ResolvableReference resource) {
		for (Specification spec : specifications) {
			// Verify if the requested resource is the spec itself
			if (spec.getDeclaration().getReference().equals(resource)) {
				return spec;
			}
			// Verify if the requested resource is provided by the spec
			if (spec.getDeclaration().getProvidedResources().contains(resource)) {
				return spec;
			}
		}
		return null;
	}

	@Override
	public Set<Specification> getSpecs() {

		return new HashSet<Specification>(specifications);
	}

	@Override
	public Set<Specification> getSpecs(String goal) {
		if (goal == null) {
			return getSpecs();
		}

		Set<Specification> ret = new HashSet<Specification>();
		ApamFilter f = ApamFilter.newInstance(goal);
		for (Specification spec : specifications) {
			if (spec.match(f)) {
				ret.add(spec);
			}
		}
		return ret;
	}

	@Override
	public Component getWaitComponent(String name) {
		return getWaitComponent(name, 0);
	}

	// /special case

	@Override
	public Component getWaitComponent(String name, long timeout) {
		Component compo = getComponent(name);
		if (compo != null) {
			return compo;
		}

		/*
		 * If not found wait and try again
		 */
		Apform2Apam.waitForComponent(name, timeout);
		compo = getComponent(name);

		if (compo == null) {// occur when timeout elapsed
			logger.debug("wake up but component is not present " + name);
		}

		return compo;
	}

}
