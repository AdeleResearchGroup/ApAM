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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
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
import fr.imag.adele.apam.impl.CompositeTypeImpl;
import fr.imag.adele.obrMan.OBRManCommand;


/**
 * This manager handles automatic component installation from a bundle repository. This allows
 * incremental installation of components as they are requested by service clients.
 * 
 * This is a contextual manager, a model can be associated with each composite type context to
 * configure the repositories used to look for components.
 * 
 * This is the main entry point that extends the APAM core, it dispatches to appropriate managers
 * depending on the context of the source component of the request.
 * 
 * This class handles a pool of bundle repositories that is shared by all contextual managers,
 * this reduces the memory footprint in the usual case that the same repository is configured
 * for several composite types. 
 *  
 * @author vega
 *
 */
public class OBRMan implements ContextualManager, DeploymentManager, RelationManager, OBRManCommand {

	private final Logger logger = LoggerFactory.getLogger(OBRMan.class);

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

	/**
	 * This class represents an APAM component repository associated with a bundle repository.
	 * 
	 * The repository handles a read-only view on the APAM components declared in the bundle
	 * repository
	 */
	private static class ComponentRepository  {
		
		private final Repository repository;
		
		private final Set<DeployableComponent> components;
		
		public ComponentRepository(OBRMan manager, Repository repository) {
			this.repository = repository;
			
			this.components = new HashSet<DeployableComponent>();
			for (Resource resource : repository.getResources()) {
				for (Capability capability : resource.getCapabilities()) {
					if (capability.getName().equals(CST.CAPABILITY_COMPONENT))
						components.add(new DeployableComponent(repository,manager,resource,capability));
				}
			}
			
		}
		
		public Repository getBundleRepository() {
			return repository;
		}
		
		public Set<DeployableComponent> getComponents() {
			return components; 
		}
	}

	/**
	 * The pool of all APAM components repositories, shared by all contexts
	 */
	private final Map<URI,ComponentRepository> repositories;

	/**
	 * Updates the in-memory repositories associated with the given contextual managers, and
	 * the metadata associated with APAM components in that context
	 */
	private void update(Set<OBRManager> contexts) {
		
		Set<URI> loadedBundleRepositories = new HashSet<URI>();
		
		for (OBRManager context : contexts) {
			
			for (URI repositoryLocation : context.getModel().getRepositoryLocations()) {
				try {

					/*
					 * just avoid loading the same repository several times
					 */
					if (loadedBundleRepositories.contains(repositoryLocation)) {
						continue;
					}
					
					/*
					 * load repository in memory and extract APAM component metadata
					 */
					Repository bundleRepository 			= repoAdmin.getHelper().repository(repositoryLocation.toURL());
					ComponentRepository componentRepository = new ComponentRepository(this,bundleRepository);
					
					loadedBundleRepositories.add(repositoryLocation);
					repositories.put(repositoryLocation,componentRepository);
					
				} catch (Exception e) {
					logger.error("Composite "+context.getName(),"Error when loading repository  :" + repositoryLocation, e);
				}
			}
			
		}
	}

	/**
	 * This class provides a filtered read-only view of the APAM component repositories visible
	 * in a given context.
	 * 
	 * The view show only the components that are specified as part of the model associated with
	 * the composite context.
	 *
	 */
	private static class ContextualIterator implements Iterator<DeployableComponent> {

		/**
		 * Iterator over the visible repositories in the context
		 */
		private final Iterator<ComponentRepository> repositories;

		/**
		 * The currently iterated component repository
		 */
		private Iterator<DeployableComponent> repository;

		private ContextualIterator(OBRMan manager, OBRManager context) {
			
			Set<ComponentRepository> selectedRepositories = new HashSet<ComponentRepository>();
			
			for (URI location : context.getModel().getRepositoryLocations()) {
				ComponentRepository repository = manager.repositories.get(location);
				
				if (repository != null)
					selectedRepositories.add(repository);
			}
			
			this.repositories 	= selectedRepositories.iterator();
			this.repository		= null;
		}


		/**
		 * Updates the reference to the next repository in the iteration 
		 */
		private void changeRepositoryIfNeeded() {
			while ((repository == null || !repository.hasNext()) && repositories.hasNext()) {
				repository = repositories.next().getComponents().iterator();
			}
		}
				
		@Override
		public boolean hasNext() {
			
			changeRepositoryIfNeeded();
			
			return repository != null && repository.hasNext();
		}

		@Override
		public DeployableComponent next() {
			
			changeRepositoryIfNeeded();
			
			if (repository == null)
				throw new NoSuchElementException();
			
			return repository.next();
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException("component repository is read-only");
		}
	}
	
	/**
	 * Initialize OBR Man
	 */
	public OBRMan(BundleContext context) {
		m_context 		= context;
		repositories	= new ConcurrentSkipListMap<URI,ComponentRepository>();

		obrManagers 	= new HashMap<CompositeType, OBRManager>();
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
	 * Initializes a new context from its specified model
	 */
	@Override
	public synchronized void initializeContext(CompositeType compositeType) {
		
		Model model = Model.loadModel(this,compositeType,m_context);
		
		/*
		 * If no model is specified for this context, use the model of the root composite
		 * context. This ensures that there is always a manager created for every context.
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

		OBRManager contextualManager = new OBRManager(this, compositeType, model);
		obrManagers.put(compositeType,contextualManager);

		update(Collections.singleton(contextualManager));
	}

	/**
	 * The model associated with a given context
	 */
	public Model getModel(CompositeType context) {
		return obrManagers.get(context).getModel();
	}


	/**
	 * Get the components available in the specified context
	 */
	public Iterable<DeployableComponent> getComponents(final OBRManager context) {
		return new Iterable<DeployableComponent>() {
			public Iterator<DeployableComponent> iterator() {
				return new ContextualIterator(OBRMan.this,context);
			}
		};
	}
	
	/**
	 * Creates a query that can be used to filter components in the repository
	 * associated with the given context
	 */
	public Requirement parseRequirement(OBRManager context, String requirement) {
		return repoAdmin.getHelper().requirement(CST.CAPABILITY_COMPONENT,requirement);
	}
	
	
	/**
	 * Creates a resolver that can be used to install components from the repository
	 * associated with the given context
	 */
	public Resolver getResolver(OBRManager context) {
		List<Repository> scope = new ArrayList<Repository>();
		
		for (URI repositoryLocation : context.getModel().getRepositoryLocations()) {
			ComponentRepository repository = repositories.get(repositoryLocation);
			
			if (repository != null)
				scope.add(repository.getBundleRepository());
		}
		
		scope.add(0,repoAdmin.getLocalRepository());
		scope.add(0,repoAdmin.getSystemRepository());
		
		return repoAdmin.resolver(scope.toArray(new Repository[scope.size()]));
	}

	/**
	 * Search a deployed version of the specified resource in the running platform
	 */
	public Bundle getBundle(Resource resource) {
		
		String bundleName = resource.getSymbolicName();
		for (Bundle bundle : m_context.getBundles()) {
			if (bundle.getSymbolicName() != null && bundle.getSymbolicName().equals(bundleName)) {
				return bundle;
			}
		}
		
		return null;
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
	public Resolved<?> resolve(RelToResolve relation) {
		
		/*
		 * Find the context in which the resolution must be performed.
		 * 
		 * This has some subtle corner cases as this method is used for two very different
		 * purposes :
		 * 
		 * 1) resolving a relationship : in this case the context is determined by the source
		 * of the relation, but there are several cases depending on its kind.
		 * 
		 * 2) finding a component by name : in this case either the specified source is an
		 * instance (and the component must be visible in its enclosing composite type), or
		 * directly the composite type that must be used as context. 
		 * 
		 */
		Component source		= relation.getLinkSource();
		CompositeType context 	= null;
		
		if (relation.isRelation()) {
			
			switch (relation.getRelationDefinition().getSourceKind()) {
			case INSTANCE:
				context = ((Instance) source).getComposite().getCompType();
				break;
			case IMPLEMENTATION:
				context = ((Implementation) source).getFirstDeployed();
				break;
			case SPECIFICATION:
				context = CompositeTypeImpl.getRootCompositeType();
				break;
			default:
				break;
			}
			
		} else {

			if (source instanceof Instance) {
				context = ((Instance) source).getComposite().getCompType();
			}
			
			if (source instanceof CompositeType) {
				context = (CompositeType) source;
			}
		}

		if (context == null) {
			logger.error("OBR: No context found for resolution " + relation);
			return null;
		}


		/*
		 *  Resolve in the appropriate context
		 */
		return obrManagers.get(context).resolve(relation);
		
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
		return obrManagers.get(context).getDeploymentUnit(component);
	}


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
		
		return obrManagers.get(context).getModel().getRepositories();
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
			update(new HashSet<OBRManager>(obrManagers.values()));
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
		
		
		update(Collections.singleton(obrManagers.get(context)));
		return true;
	}


}

