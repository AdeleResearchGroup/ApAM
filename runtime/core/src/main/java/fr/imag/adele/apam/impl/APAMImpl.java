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
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Manager;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.RelationManager;
import fr.imag.adele.apam.apform.ApformCompositeType;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.util.Attribute;
import fr.imag.adele.apam.util.Util;

public class APAMImpl implements Apam {

	/**
	 * An special apform instance created only for those composites that do not
	 * exist in the Apform ipojo layer. Creates a minimal definition structure.
	 */
	private static class ApamOnlyComposite extends BaseApformComponent<Composite, InstanceDeclaration> implements ApformInstance {

		public ApamOnlyComposite(ImplementationReference<?> implementation, String name, Map<String, String> initialProperties) {

			super(new InstanceDeclaration(implementation, name, null));
			if (initialProperties != null) {
				for (Map.Entry<String, String> property : initialProperties.entrySet()) {
					if (!Attribute.isFinalAttribute(property.getKey())) {
						declaration.getProperties().put(property.getKey(), property.getValue());
					}
				}
			}

		}

		@Override
		public Object getServiceObject() {
			throw new UnsupportedOperationException("method not available in application composite instance");
		}

	}

	/**
	 * An special apform implementation created only for those composites types
	 * that do not exist in the Apform ipojo layer. Creates a minimal definition
	 * structure.
	 */
	private static class ApamOnlyCompositeType extends BaseApformComponent<CompositeType, CompositeDeclaration> implements ApformCompositeType {

		/**
		 * The associated models
		 */
		private final Set<ManagerModel> models = new HashSet<ManagerModel>();
		/**
		 * The number of instances created for this composite type
		 */
		private int numInstances;

		public ApamOnlyCompositeType(String name, String specificationName, String mainName, Set<ManagerModel> models, Map<String, String> properties) {

			super(new CompositeDeclaration(name, specificationName != null && !specificationName.trim().isEmpty() ? new SpecificationReference(specificationName) : null, new ComponentReference<ComponentDeclaration>(mainName)));
			if (properties != null) {
				declaration.getProperties().putAll(properties);
			}

			assert name != null && mainName != null && models != null;

			this.models.addAll(models);

			numInstances = 0;
		}

		@Override
		public ApformInstance addDiscoveredInstance(Map<String, Object> configuration) throws InvalidConfiguration, UnsupportedOperationException {
			throw new UnsupportedOperationException("method not available for appliation composite type");
		}

		@Override
		public ApformInstance createInstance(Map<String, String> initialProperties) {
			numInstances++;
			String name = declaration.getName() + "-" + numInstances;
			return new ApamOnlyComposite(declaration.getReference(), name, initialProperties);
		}

		@Override
		public Set<ManagerModel> getModels() {
			return models;
		}

	}

	private static Logger logger = LoggerFactory.getLogger(APAMImpl.class);
	/*
	 * The bundle context used for deployment in the execution paltform
	 */
	public static BundleContext context;
	/*
	 * A reference to the ApamMan manager.
	 * 
	 * This are the managers required to start the platform.
	 */
	private RelationManager apamMan;

	private UpdateMan updateMan;

	private FailedResolutionManager failureMan;

	/**
	 * The list of expected managers
	 */
	private Set<String> expectedManagers = new ConcurrentSkipListSet<String>();

	public APAMImpl(BundleContext context) {
		APAMImpl.context = context;
		apamMan = new ApamMan();
		if (apamMan == null) {
			throw new RuntimeException("Error while constructor of ApamMan");
		}
		updateMan = new UpdateMan();
		if (updateMan == null) {
			throw new RuntimeException("Error while constructor of updateMan");
		}
		failureMan = new FailedResolutionManager();
		System.err.println("deadlock ?");
		new CST(this);

		for (ManagerModel rootModel : CompositeTypeImpl.getRootCompositeType().getModels()) {
			expectedManagers.add(rootModel.getManagerName());
		}

		/*
		 * disable resolution temporarily, until all the required managers of
		 * the root composite are registered
		 */
		if (!expectedManagers.isEmpty()) {
			((ApamResolverImpl) CST.apamResolver).disable("Registration of the required managers " + expectedManagers, 20 * 1000/* ms */);
		}

		DynaMan dynaMan = new DynaMan();

		ApamManagers.addRelationManager(apamMan, -1); // -1 to be sure it is not
		// in the main loop
		ApamManagers.addRelationManager(updateMan, -2); // -2 to be sure it is
		// not in the main loop
		ApamManagers.addRelationManager(failureMan, -3); // -2 to be sure it is
		// not in the main loop
		ApamManagers.addDynamicManager(updateMan);

		dynaMan.start(this);
		failureMan.start(this);
		try {
			Util.printFileToConsole(context.getBundle().getResource("logo.txt"));
		} catch (IOException e) {
		}
	}

	/**
	 * Creates a composite type from the specified parameters
	 */
	private CompositeType createCompositeType(CompositeType parent, String name, String specification, String mainComponent, Set<ManagerModel> models, Map<String, String> properties) {

		assert name != null && mainComponent != null;

		if (models == null) {
			models = new HashSet<ManagerModel>();
		}

		if (parent == null) {
			parent = CompositeTypeImpl.getRootCompositeType();
		}

		ApformImplementation apfCompo = new ApamOnlyCompositeType(name, specification, mainComponent, models, properties);

		/*
		 * If the provided specification is not installed force a resolution
		 */
		if (specification != null && CST.componentBroker.getSpec(specification) == null) {
			CST.apamResolver.findSpecByName(parent.getInst(), specification);
		}

		return (CompositeType) CST.componentBroker.addImpl(parent, apfCompo);
	}

	@Override
	public CompositeType createCompositeType(String inCompoType, String name, String specification, String mainComponent, Set<ManagerModel> models, Map<String, String> attributes) {

		/*
		 * Verify if it already exists
		 */
		CompositeType compositeType = getCompositeType(name);
		if (compositeType != null) {
			logger.error("Error creating composite type: already exists " + name);
			return compositeType;
		}

		/*
		 * Get the specified enclosing composite type
		 */
		Implementation parent = null;
		if (inCompoType != null) {
			parent = CST.apamResolver.findImplByName(null, inCompoType);
			if (parent == null || !(parent instanceof CompositeType)) {
				logger.error("Error creating composite type: specified enclosing composite " + inCompoType + " is not a deployed composite type.");
				return null;
			}
		}

		return createCompositeType((CompositeType) parent, name, specification, mainComponent, models, attributes);
	}

	public RelationManager getApamMan() {
		return apamMan;
	}

	@Override
	public Composite getComposite(String name) {
		return CompositeImpl.getComposite(name);
	}

	@Override
	public Collection<Composite> getComposites() {
		return CompositeImpl.getComposites();
	}

	@Override
	public CompositeType getCompositeType(String name) {
		return CompositeTypeImpl.getCompositeType(name);
	}

	@Override
	public Collection<CompositeType> getCompositeTypes() {
		return CompositeTypeImpl.getCompositeTypes();
	}

	public RelationManager getFailedResolutionManager() {
		return failureMan;
	}

	@Override
	public Collection<Composite> getRootComposites() {
		return CompositeImpl.getRootComposites();
	}

	@Override
	public Collection<CompositeType> getRootCompositeTypes() {
		return CompositeTypeImpl.getRootCompositeTypes();
	}

	public RelationManager getUpdateMan() {
		return updateMan;
	}

	public boolean isPredefinedManager(RelationManager manager) {
		return manager.equals(getApamMan()) || manager.equals(getUpdateMan()) || manager.equals(getFailedResolutionManager());
	}

	public void managerRegistered(Manager manager) {
		expectedManagers.remove(manager.getName());
		if (expectedManagers.isEmpty()) {
			((ApamResolverImpl) CST.apamResolver).enable();
		}
	}

	@Override
	public Composite startAppli(CompositeType composite) {
		return (Composite) composite.createInstance(null, null);
	}

	@Override
	public Composite startAppli(String compositeName) {

		Implementation compoType = CST.apamResolver.findImplByName(null, compositeName);
		if (compoType == null) {
			logger.error("Error starting application: " + compositeName + " is not a deployed composite.");
			return null;
		}

		if (!(compoType instanceof CompositeType)) {
			logger.error("Error starting application: " + compoType.getName() + " is not a composite.");
			return null;
		}

		return startAppli((CompositeType) compoType);
	}

	@Override
	public Composite startAppli(URL compoURL, String compositeName) {

		Implementation compoType = CST.componentBroker.createImpl(null, compositeName, compoURL, null);

		if (compoType == null) {
			logger.error("Error starting application: " + compositeName + " can not be deployed.");
			return null;
		}

		if (!(compoType instanceof CompositeType)) {
			logger.error("Error starting application: " + compoType.getName() + " is not a composite.");
			return null;
		}

		return startAppli((CompositeType) compoType);
	}

}
