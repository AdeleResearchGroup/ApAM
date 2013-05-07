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
package fr.imag.adele.obrMan.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
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
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.impl.RelationImpl;
import fr.imag.adele.obrMan.OBRManCommand;
import fr.imag.adele.obrMan.internal.OBRManager.Selected;

public class OBRMan implements RelationManager, OBRManCommand {

	// Link compositeType with it instance of obrManager
	private final Map<String, OBRManager> obrManagers;

	// iPOJO injected
	private RepositoryAdmin repoAdmin;

	// private Apam apam;

	private final Logger logger = LoggerFactory.getLogger(OBRMan.class);

	private final BundleContext m_context;

	private final long timeout = 10000;

	/**
	 * OBRMAN activated, register with APAM
	 */

	public OBRMan(BundleContext context) {
		m_context = context;
		obrManagers = new HashMap<String, OBRManager>();
	}

	public void start() {
		ApamManagers.addRelationManager(this, 3);
		// logger.info("[OBRMAN] started");
	}

	public void stop() {
		ApamManagers.removeRelationManager(this);
		obrManagers.clear();
		// logger.info("[OBRMAN] stopped");
	}

	static List<String> onLoadingResource = new ArrayList<String>();

	/**
	 * Instal ans instantiate the selected bundle, and return the component. If
	 * forced = false (default) does not try to install if the component is
	 * allready existing.
	 * 
	 * @param selected
	 * @return
	 */
	private Component installInstantiate(Selected selected) {
		if (selected == null)
			return null;

		String name = selected.getComponentName();
		fr.imag.adele.apam.Component c = CST.componentBroker.getComponent(name);
		// Check if already deployed
		if (c == null) {

			if (bundleInactif(selected.resource.getSymbolicName())) {
				logger.info("The bundle " + selected.resource.getSymbolicName() + " is already installed!");
				return null;
			}
			// deploy selected resource
			boolean deployed = selected.obrManager.deployInstall(selected);
			if (!deployed) {
				System.err.print("could not install resource ");
				ObrUtil.printRes(selected.resource);
				return null;
			}
			// waiting for the component to be ready in Apam.
			c = CST.componentBroker.getWaitComponent(name, timeout);
			if (c != null && c instanceof Implementation) {
				for (String instanceName : selected.getInstancesOfSelectedImpl()) {
					CST.componentBroker.getWaitComponent(instanceName, timeout);
				}
			}


		} else { // do not install twice.
			// It is a logical deployment. The already existing component is not
			// visible !
			// System.err.println("Logical deployment of : " + name +
			// " found by OBRMAN but allready deployed.");
		}

		return c;

	}


	public boolean bundleInactif(String symbolicName) {
		Bundle[] bunldes = m_context.getBundles();
		for (Bundle bundle : bunldes) {
			if (bundle.getSymbolicName() != null && bundle.getSymbolicName().equals(symbolicName)) {
				if (bundle.getState() == Bundle.ACTIVE || bundle.getState() == Bundle.STARTING || bundle.getState() == Bundle.UNINSTALLED) {
					return false;
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public String getName() {
		return CST.OBRMAN;
	}

	// at the end
	@Override
	public void getSelectionPath(Component client, Relation dep, List<RelationManager> involved) {
		involved.add(involved.size(), this);
	}

	// @Override
	// public Instance resolveImpl(Component client, Implementation impl,
	// Relation dep) {
	// return null;
	// }
	//
	// @Override
	// public Set<Instance> resolveImpls(Component client, Implementation impl,
	// Relation dep) {
	// return null;
	// }

	@Override
	public int getPriority() {
		return 3;
	}

	@Override
	public void newComposite(ManagerModel model, CompositeType compositeType) {
		OBRManager obrManager;
		if (model == null) { // if no model for the compositeType, set the root
			// composite model
			obrManager = searchOBRManager(compositeType);
		} else {
			try {// try to load the compositeType model
				LinkedProperties obrModel = new LinkedProperties();
				obrModel.load(model.getURL().openStream());
				obrManager = new OBRManager(this, compositeType.getName(), repoAdmin, obrModel);
			} catch (IOException e) {// if impossible to load the model for the
				// compositeType, set the root composite
				// model
				logger.error("Invalid OBRMAN Model. Cannot be read stream " + model.getURL(), e.getCause());
				obrManager = searchOBRManager(compositeType);
			}
		}
		obrManagers.put(compositeType.getName(), obrManager);
	}

	private OBRManager searchOBRManager(CompositeType compoType) {
		OBRManager obrManager = null;

		// in the case of root composite, compoType = null
		if (compoType != null) {
			obrManager = obrManagers.get(compoType.getName());
		}

		// Use the root composite if the model is not specified
		if (obrManager == null) {
			obrManager = obrManagers.get(CST.ROOT_COMPOSITE_TYPE);
			if (obrManager == null) { // If the root manager was never been
				// initialized
				// lookFor root.OBRMAN.cfg and create obrmanager for the root
				// composite in a customized location
				String rootModelurl = m_context.getProperty(ObrUtil.ROOT_MODEL_URL);
				try {// try to load root obr model from the customized location
					if (rootModelurl != null) {
						URL urlModel = (new File(rootModelurl)).toURI().toURL();
						setInitialConfig(urlModel);
					} else {
						LinkedProperties obrModel = new LinkedProperties();
						customizedRootModelLocation();
						obrModel.put(ObrUtil.LOCAL_MAVEN_REPOSITORY, "true");
						obrModel.put(ObrUtil.DEFAULT_OSGI_REPOSITORIES, "true");
						obrManager = new OBRManager(this, CST.ROOT_COMPOSITE_TYPE, repoAdmin, obrModel);
						obrManagers.put(CST.ROOT_COMPOSITE_TYPE, obrManager);
					}
				} catch (Exception e) {// if failed to load customized location,
					// set default properties for the root
					// model
					logger.error("Invalid Root URL Model. Cannot be read stream " + rootModelurl, e.getCause());
					LinkedProperties obrModel = new LinkedProperties();
					customizedRootModelLocation();
					obrModel.put(ObrUtil.LOCAL_MAVEN_REPOSITORY, "true");
					obrModel.put(ObrUtil.DEFAULT_OSGI_REPOSITORIES, "true");
					obrManager = new OBRManager(this, CST.ROOT_COMPOSITE_TYPE, repoAdmin, obrModel);
					obrManagers.put(CST.ROOT_COMPOSITE_TYPE, obrManager);
				}
			}
		}
		return obrManager;
	}

	private void customizedRootModelLocation() {

	}

	@Override
	public Resolved<?> resolveRelation(Component client, Relation dep) {
		Component ret = null;

		//It is either a resolution (spec -> instance) or a resource reference (interface or message)
		if ((dep.getTarget() instanceof ResourceReference) || (((ComponentReference<?>) dep.getTarget()).getKind() == ComponentKind.SPECIFICATION && dep.getTargetKind() != ComponentKind.SPECIFICATION)) {
			ret = resolveByResource(client, dep);
		}
 else
			ret = findByName(client, dep);

		//
		//		
		//		if (dep.getTarget() instanceof ComponentReference<?>) {
		//			/*
		//			 * It is a find by name if the component is the same kind as the
		//			 * target or looking for an instance, but an implementation name is
		//			 * provided : return the implem.
		//			 */
		//			if (((ComponentReference<?>) dep.getTarget()).getKind() == dep.getTargetKind() || ((ComponentReference<?>) dep.getTarget()).getKind() == ComponentKind.IMPLEMENTATION && dep.getTargetKind() == ComponentKind.INSTANCE) {
		//				ret = findByName(client, dep);
		//			} else if (((ComponentReference<?>) dep.getTarget()).getKind() == ComponentKind.COMPONENT) {
		//				//Unclear. Try to find a component of that name, and then check if it is of the right kind
		//				ret = findByName(client, dep);
		//			}
		//		}
		//
		//		//It is either a resolution (spec -> instance) or a resource reference (interface or message)
		//		else {
		//			ret = resolveByResource(client, dep);
		//		}

		//Not found
		if (ret == null)
			return null;

		/*
		 * Check if the found component if of the right kind. If requires an
		 * Instance, and found an implementation, it is ok, but build the
		 * "toInstantiate" result.
		 */
		boolean badKind = (ret.getKind() != dep.getTargetKind());
		if (badKind) {
			logger.debug("Looking for " + dep.getTargetKind() + " but found " + ret);
			if (!(ret instanceof Implementation)) {
				logger.error("invalide return from OBR : expected " + dep.getTargetKind() + " got " + ret.getKind());
				return null;
			}
		}

		switch (dep.getTargetKind()) {
		case SPECIFICATION :
			return new Resolved<Specification>((Specification) ret);
		case IMPLEMENTATION :
			if (badKind)
				return new Resolved<Implementation>((Implementation) ret, true);
			return new Resolved<Implementation>((Implementation) ret);
		case INSTANCE :
			if (badKind)
				return new Resolved<Instance>((Implementation) ret, true);
			return new Resolved<Instance>((Instance) ret);
		case COMPONENT: //Not allowed
		}
		return null;
	}

	private Component findByName(Component source, Relation rel) {

		CompositeType compoType = null;
		if (source instanceof Instance) {
			compoType = ((Instance) source).getComposite().getCompType();
		}
		// Find the composite OBRManager
		OBRManager obrManager = searchOBRManager(compoType);
		if (obrManager == null) {
			logger.error("OBR: No context found for composite " + compoType);
			return null;
		}

		// Relation rel = new RelationImpl(source, new
		// ComponentReference(componentName), kind);
		// ((RelationImpl) rel).computeFilters(source);
		Selected selected = obrManager.lookFor(CST.CAPABILITY_COMPONENT, "(name=" + rel.getTarget().getName() + ")", rel);
		fr.imag.adele.apam.Component c = installInstantiate(selected);
		if (c == null)
			return null;
		if (c.getKind() != rel.getTargetKind()) {
			logger.debug("ERROR : " + rel.getTarget().getName() + " is found but is an " + rel.getTargetKind() + " not an " + c.getKind());
		}

		return c;
	}

	// interface manager
	private Component resolveByResource(Component source, Relation dep) {
		ResolvableReference resource = dep.getTarget();
		CompositeType compoType = null;
		if (source instanceof Instance) {
			compoType = ((Instance) source).getComposite().getCompType();
		}

		// Find the composite OBRManager
		OBRManager obrManager = searchOBRManager(compoType);
		if (obrManager == null)
			return null;


		fr.imag.adele.obrMan.internal.OBRManager.Selected selected = null;
		// Implementation impl = null;
		if (resource instanceof SpecificationReference) {
			selected = obrManager.lookFor(CST.CAPABILITY_COMPONENT, "(provide-specification*>" + resource.as(SpecificationReference.class).getName() + ")", dep);
			// constraints, preferences);
		}
		if (resource instanceof InterfaceReference) {
			selected = obrManager.lookFor(CST.CAPABILITY_COMPONENT, "(provide-interfaces*>" // =*;"
					+ resource.as(InterfaceReference.class).getJavaType() + ")", dep);
			// constraints, preferences);
		}
		if (resource instanceof MessageReference) {
			selected = obrManager.lookFor(CST.CAPABILITY_COMPONENT, "(provide-messages*>" + resource.as(MessageReference.class).getJavaType() + ")", dep);
			// constraints, preferences);
		}
		if (selected != null) {
			return installInstantiate(selected);
		}
		return null;
	}

	// @Override
	// public Component findComponentByName(Component client, String
	// componentName) {
	// return findByName(client, componentName,
	// fr.imag.adele.apam.Component.class);
	// }

	// @Override
	// public Specification findSpecByName(Component client, String specName) {
	// return (Specification) findByName(client, specName,
	// ComponentKind.SPECIFICATION);
	// // return findByName(client, specName,
	// // fr.imag.adele.apam.Specification.class);
	// }
	//
	// @Override
	// public Implementation findImplByName(Component client, String implName) {
	// return (Implementation) findByName(client, implName,
	// ComponentKind.IMPLEMENTATION);
	// // return findByName(client, implName,
	// // fr.imag.adele.apam.Implementation.class);
	// }
	//
	// @Override
	// public Instance findInstByName(Component client, String instName) {
	// return (Instance) findByName(client, instName, ComponentKind.INSTANCE);
	// // return findByName(client, instName,
	// // fr.imag.adele.apam.Instance.class);
	// }

	public OBRManager getOBRManager(String compositeTypeName) {
		return obrManagers.get(compositeTypeName);
	}

	@Override
	public void notifySelection(Component client, ResolvableReference resName, String depName, Implementation impl, Instance inst, Set<Instance> insts) {
		// Do not care
	}

	public String getDeclaredOSGiOBR() {
		return m_context.getProperty(ObrUtil.OSGI_OBR_REPOSITORY_URL);
	}

	@Override
	public Set<String> getCompositeRepositories(String compositeTypeName) {
		Set<String> result = new HashSet<String>();
		OBRManager obrmanager = getOBRManager(compositeTypeName);
		if (obrmanager == null)
			return result;

		for (Repository repository : obrmanager.getRepositories()) {
			result.add(repository.getURI());
		}
		return result;
	}

	@Override
	public void setInitialConfig(URL modellocation) throws IOException {
		LinkedProperties obrModel = new LinkedProperties();
		if (modellocation != null) {
			obrModel.load(modellocation.openStream());
			OBRManager obrManager = new OBRManager(this, CST.ROOT_COMPOSITE_TYPE, repoAdmin, obrModel);
			obrManagers.put(CST.ROOT_COMPOSITE_TYPE, obrManager);
		} else {
			throw new IOException("URL is null");
		}
	}

	@Override
	public ComponentBundle findBundle(CompositeType compoType, String bundleSymbolicName, String componentName) {
		if (bundleSymbolicName == null || componentName == null)
			return null;

		// Find the composite OBRManager
		OBRManager obrManager = searchOBRManager(compoType);
		if (obrManager == null)
			return null;

		return obrManager.lookForBundle(bundleSymbolicName, componentName);
	}

	/**
	 * Update resources from repositories
	 * 
	 * @param compositeName
	 *            the name of the composite to update or *
	 */
	@Override
	public boolean updateRepos(String compositeName) {
		if (compositeName == null)
			return false;
		OBRManager obrmanager = null;
		if (compositeName.equals("*")) {
			for (OBRManager obrManager2 : obrManagers.values()) {
				obrManager2.updateListOfResources(repoAdmin);
			}
			return true;
		} else if (compositeName.equals("root")) {
			obrmanager = getOBRManager(CST.ROOT_COMPOSITE_TYPE);
		} else {
			obrmanager = getOBRManager(compositeName);
		}
		if (obrmanager == null)
			return false;
		obrmanager.updateListOfResources(repoAdmin);
		return true;
	}


}
