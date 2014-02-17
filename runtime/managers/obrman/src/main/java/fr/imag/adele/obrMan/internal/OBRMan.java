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
import org.apache.felix.bundlerepository.Resolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.ContextualManager;
import fr.imag.adele.apam.DeploymentManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.RelationManager;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.impl.CompositeTypeImpl;
import fr.imag.adele.obrMan.OBRManCommand;
import fr.imag.adele.obrMan.internal.OBRManager.Selected;

public class OBRMan implements ContextualManager, DeploymentManager, RelationManager, OBRManCommand {

	/**
	 * An injected reference to the APAM core, used to ensure proper order
	 */
	private Apam apam;

	/**
	 * An injected reference to the OSGi Bundle Repository administrator 
	 */
	private RepositoryAdmin repoAdmin;

	/**
	 * The context of this bundle
	 */
	private final BundleContext m_context;

	/**
	 * The list of OBR managers associated with each composite context
	 */
	private final Map<CompositeType, OBRManager> obrManagers;


	private final Logger logger = LoggerFactory.getLogger(OBRMan.class);

	/**
	 * Initialize OBR Man
	 */
	public OBRMan(BundleContext context) {
		m_context = context;
		obrManagers = new HashMap<CompositeType, OBRManager>();
	}

	@Override
	public String getName() {
		return CST.OBRMAN;
	}

	/**
	 * Register with APAM on start up
	 */
	public void start() {
		ApamManagers.addRelationManager(this,Priority.HIGH);
	}

	/**
	 * Unregister from APAM on stop
	 */
	public void stop() {
		ApamManagers.removeRelationManager(this);
		obrManagers.clear();
	}

	/**
	 * Give access to the apam instance
	 */
	public Apam getApam() {
		return apam;
	}
	
	
	/**
	 * Loads the repositories associated with the given context
	 */
	public List<Repository> loadRepositories(OBRManager manager) {
		List<Repository> result = new ArrayList<Repository>();
		
		for (URL repositoryLocation : manager.getModel().getRepositoryLocations()) {
			try {
				result.add(repoAdmin.getHelper().repository(repositoryLocation));
			} catch (Exception e) {
				logger.error("Composite "+manager.getContext().getName(),"Error when loading repository  :" + repositoryLocation, e);
			}
		}
		
		return result;
	}

	/**
	 * Get a resolver that allows to install bundles from resources in the given context
	 */
	public Resolver getResolver(OBRManager manager) {
		
		List<Repository> repositories = loadRepositories(manager);
		repositories.add(0,repoAdmin.getSystemRepository());
		repositories.add(0,repoAdmin.getLocalRepository());
		
		return repoAdmin.resolver(repositories.toArray(new Repository[repositories.size()]));
	}

	/**
	 * Updates the list of managers associated with a new context
	 */
	@Override
	public synchronized void initializeContext(CompositeType compositeType) {
		
		Model model = Model.loadModel(this,compositeType,m_context);
		
		/*
		 * If no model is specified for this context, use the model of the root composite
		 * context.
		 *
		 * If no model is specified for the root composite, then initialize a default model. 
		 * 
		 * NOTE: a manager can be assured that the root composite context is initialized by
		 * the APAM core before any other context.
		 */
		
		if (model == null) {
			if (compositeType.equals(CompositeTypeImpl.getRootCompositeType())) {
				model = Model.loadDefaultRootModel(this,m_context);
			} else {
				OBRManager rootManager = obrManagers.get(CompositeTypeImpl.getRootCompositeType());
				model = rootManager.getModel();
			}
		}
		
		obrManagers.put(compositeType, new OBRManager(this, compositeType, model));
	}

	public synchronized OBRManager getManager(CompositeType context) {
		return obrManagers.get(context);
	}
	
	/**
	 * This external manager is actively involved in resolution
	 */
	@Override
	public boolean beginResolving(RelToResolve dep) {
		return true;
	}

	/**
	 * Perform resolution
	 */
	@Override
	public Resolved<?> resolve(RelToResolve dep) {
		
		Component ret 		= null;

		// It is either a resolution (spec -> instance) or a resource reference
		// (interface or message)
		if ((dep.getTarget() instanceof ResourceReference) || (((ComponentReference<?>) dep.getTarget()).getKind() == ComponentKind.SPECIFICATION && dep.getTargetKind() != ComponentKind.SPECIFICATION)) {
			ret = resolveByResource(dep);
		} else {
			ret = findByName(dep);
		}

		// Not found
		if (ret == null) {
			return null;
		}

		/*
		 * Check if the found component is of the right kind. If requires an
		 * Instance, and found an implementation, it is ok, but build the
		 * "toInstantiate" result.
		 */
		boolean badKind = (ret.getKind() != dep.getTargetKind());
		if (badKind) {
			logger.debug("Looking for " + dep.getTargetKind() + " but found " + ret);
			// It is Ok to return an implem when an instance is required. The
			// resolver will instantiate.
			if (!(ret.getKind() == ComponentKind.IMPLEMENTATION && dep.getTargetKind() == ComponentKind.INSTANCE)) {
				logger.error("invalide return from OBR : expected " + dep.getTargetKind() + " got " + ret.getKind());
				return null;
			}
		}

		switch (dep.getTargetKind()) {
		case SPECIFICATION:
			return new Resolved<Specification>((Specification) ret);
		case IMPLEMENTATION:
			if (badKind) {
				return new Resolved<Implementation>((Implementation) ret, true);
			}
			return new Resolved<Implementation>((Implementation) ret);
		case INSTANCE:
			if (badKind) {
				return new Resolved<Instance>((Implementation) ret, true);
			}
			return new Resolved<Instance>((Instance) ret);
		case COMPONENT: // Not allowed
		}
		return null;
	}

	/**
	 * Find the target of the relation, by name
	 */
	private Component findByName(RelToResolve rel) {

		Component source		= rel.getLinkSource();
		CompositeType compoType = null;
		
		if (source instanceof Instance) {
			compoType = ((Instance) source).getComposite().getCompType();
		}
		
		if (source instanceof CompositeType) {
			compoType = (CompositeType) source;
		}
		
		// Find the composite OBRManager
		OBRManager obrManager = getManager(compoType);
		if (obrManager == null) {
			logger.error("OBR: No context found for composite " + compoType);
			return null;
		}

		Selected selected = obrManager.lookFor(CST.CAPABILITY_COMPONENT, "(name=" + rel.getTarget().getName() + ")", rel);
		fr.imag.adele.apam.Component c = installInstantiate(selected);
		if (c == null) {
			return null;
		}
		if (c.getKind() != rel.getTargetKind()) {
			logger.debug("ERROR : " + rel.getTarget().getName() + " is found but is an " + rel.getTargetKind() + " not an " + c.getKind());
		}

		return c;
	}

	/**
	 * Search for a component providing the target resource
	 */
	private Component resolveByResource(RelToResolve dep) {

		Component source				= dep.getLinkSource();
		ResolvableReference resource	= dep.getTarget();
		CompositeType compoType = null;
		if (source instanceof Instance) {
			compoType = ((Instance) source).getComposite().getCompType();
		}

		// Find the composite OBRManager
		OBRManager obrManager = getManager(compoType);
		if (obrManager == null) {
			return null;
		}

		fr.imag.adele.obrMan.internal.OBRManager.Selected selected = null;
		if (resource instanceof SpecificationReference) {
			selected = obrManager.lookFor(CST.CAPABILITY_COMPONENT, "(provide-specification*>" + resource.as(SpecificationReference.class).getName() + ")", dep);
		}
		if (resource instanceof InterfaceReference) {
			selected = obrManager.lookFor(CST.CAPABILITY_COMPONENT, "(provide-interfaces*>" + resource.as(InterfaceReference.class).getJavaType() + ")", dep);
		}
		if (resource instanceof MessageReference) {
			selected = obrManager.lookFor(CST.CAPABILITY_COMPONENT, "(provide-messages*>" + resource.as(MessageReference.class).getJavaType() + ")", dep);
		}
		if (selected != null) {
			return installInstantiate(selected);
		}
		return null;
	}

	/**
	 * Get the deployment unit associated with a bundle deployed by this manager.
	 */
	@Override
	public DeploymentManager.Unit getDeploymentUnit(CompositeType context, Implementation component) {
		
		Bundle bundle 			= component.getApformComponent().getBundle();
		String componentName 	= component.getName();

		if (bundle.getSymbolicName() == null || componentName == null) {
			return null;
		}

		// Find the composite OBRManager
		OBRManager obrManager = getManager(context);
		if (obrManager == null) {
			return null;
		}

		Selected componentCapability = obrManager.lookForBundle(bundle.getSymbolicName(), componentName);
		return componentCapability != null ? new DeployedComponent(context, component,componentCapability,logger) : null;
	}

	/**
	 * This represents a component that has been deployed in the system by obrman, and that can be
	 * updated from the repository
	 */
	private static class DeployedComponent implements DeploymentManager.Unit {

		private final Logger 		logger;
		
		private final CompositeType	context;
		private final Bundle 		componentBundle;
		private final Selected		componentCapability;
		
		
		public DeployedComponent(CompositeType context, Implementation component, Selected capability, Logger logger) {
			
			this.context				= context;
			this.componentBundle		= component.getApformComponent().getBundle();
			this.componentCapability	= capability;
			
			this.logger					= logger;
		}
		
		
		@Override
		public Set<String> getComponents() {
			return componentCapability.getComponents();
		}

		@Override
		public void update() throws Exception {

			logger.info("Updating component " + componentCapability.getComponentName() + " in composite " + context + ".\n     From bundle: " + componentCapability.getBundelURL());
			componentBundle.update(componentCapability.getBundelURL().openStream());
			
		}
		
	}
	

	/**
	 * Deploy and return the component, if possible. Null if failed. if : the
	 * component we are looking for is existing (maybe arrived in the mean
	 * time), return it the bundle does not exist currently : deploy, wait for
	 * the component and return it the bundle is already deployed and active :
	 * it is not the version we are looking for : do nothing and return false
	 * wait for the component and return it the bundle is starting : wait for
	 * the component and return it. the bundle is installed but is not started :
	 * try to start it, wait for the component and return it, if failed, return
	 * null
	 * 
	 * @param selected
	 *            : the selected bundle and component(s)
	 * @return
	 */
	private Component installInstantiate(Selected selected) {
		if (selected == null) {
			return null;
		}

		/*
		 * If the component exists, maybe arrived in the mean time, or from
		 * another bundle or in another version, do nothing
		 */
		fr.imag.adele.apam.Component c = CST.componentBroker.getComponent(selected.getComponentName());
		if (c != null) {
			return c;
		}

		/*
		 * Check if the bundle is already existing
		 */
		Bundle[] bundles = m_context.getBundles();
		Bundle theBundle = null;
		String bundleName = selected.resource.getSymbolicName();
		for (Bundle bundle : bundles) {
			if (bundle.getSymbolicName() != null && bundle.getSymbolicName().equals(bundleName)) {
				theBundle = bundle;
				break;
			}
		}

		/*
		 * Normal case : bundle does not exist : deploy, wait and return the
		 * component
		 */
		if (theBundle == null) {
			if (! selected.obrManager.deployInstall(selected)) {
				return null;
			}
			return waitAndReturnComponent(selected);
		}

		/*
		 * the bundle is already deployed and active : it is not the version we
		 * are looking for. Do nothing and return false It may be active or
		 * starting if started in parallel by another thread ... OK wait for the
		 * component and return it.
		 */
		if (theBundle.getState() == Bundle.ACTIVE || theBundle.getState() == Bundle.STARTING) {
			if (theBundle.getVersion().equals(selected.resource.getVersion())) {
				return waitAndReturnComponent(selected);
			}
			logger.error("Bundle " + selected.getComponentName() + " is already installed under version " + theBundle.getVersion() + " while trying to deploy version " + selected.resource.getVersion());
			return null;
		}

		/*
		 * the bundle is installed but is not started : try to start it, wait
		 * for the component and return it, if failed, return null
		 */
		if (theBundle.getState() == Bundle.INSTALLED || theBundle.getState() == Bundle.RESOLVED) {
			try {
				theBundle.start();
			} catch (BundleException e) {
				// Starting failed. No solution : cannot be deployed and cannot
				// be started. Make as if failed
				logger.info("The bundle " + theBundle.getSymbolicName() + " is installed but cannot be started!");
				return null;
			}

			logger.info("The bundle " + theBundle.getSymbolicName() + " is installed and has been be started!");
			return waitAndReturnComponent(selected);
		}
		return null;
	}


	private Component waitAndReturnComponent(Selected selected) {
		// waiting for the component to be ready in Apam.
		Component c = CST.componentBroker.getWaitComponent(selected.getComponentName(), timeout);

		/*
		 * In fact, we are waiting for its instances; they are "arriving"; wait
		 * for them
		 */
		if (c != null && c instanceof Implementation) {
			for (String instanceName : selected.getInstancesOfSelectedImpl()) {
				CST.componentBroker.getWaitComponent(instanceName, timeout);
			}
		}
		return c;
	}
	

	private final long timeout = 10000;


	@Override
	public synchronized void setInitialConfig(URL modelLocation) throws IOException {
		
		CompositeType compositeType = CompositeTypeImpl.getRootCompositeType();
		Model model = Model.loadRootModel(this,m_context,modelLocation);
		obrManagers.put(compositeType, new OBRManager(this, compositeType, model));
	}

	@Override
	public Set<String> getCompositeRepositories(String compositeName) {
		Set<String> result = new HashSet<String>();
		
		CompositeType context = !compositeName.equals("root") ? 
				apam.getCompositeType(compositeName) :
				CompositeTypeImpl.getRootCompositeType();
				
		if (context == null)
			return result;
		
		OBRManager obrmanager = getManager(context);
		if (obrmanager == null)
			return result;

		return obrmanager.getModel().getRepositories();
	}


	/**
	 * Update resources from repositories
	 * 
	 * @param compositeName
	 *            the name of the composite to update or *
	 */
	@Override
	public boolean updateRepos(String compositeName) {
		if (compositeName == null) {
			return false;
		}
		
		/*
		 * refresh all
		 */
		if (compositeName.equals("*")) {
			for (OBRManager obrManager : new HashSet<OBRManager>(obrManagers.values())) {
				obrManager.refresh();
			}
			return true;
		}

		/*
		 * refresh the specified manager
		 */
		CompositeType context = !compositeName.equals("root") ? 
										apam.getCompositeType(compositeName) :
										CompositeTypeImpl.getRootCompositeType();
										
		if (context == null)
			return false;
		
		OBRManager obrmanager = getManager(context);
		if (obrmanager == null)
			return false;
		
		obrmanager.refresh();
		return true;
	}


}
