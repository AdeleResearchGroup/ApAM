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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.util.ApamFilter;

/**
 * This manager handles OBR request for a specific composite.
 * 
 * Resolution in this context is based on repositories specified in the OBR manager
 * model associated with the context in APAM
 * 
 * 
 * @author vega
 *
 */
public class OBRManager {


	private final Logger logger = LoggerFactory.getLogger(OBRManager.class);

	/**
	 * The context this manager handles
	 */
	private final CompositeType context;

	/**
	 * The global obr manager
	 */
	private final OBRMan obrMan;

	/**
	 * The model associated with this composite
	 */
	private final Model model;

	/**
	 * The list of loaded repositories
	 */
	private List<Repository> repositories = new ArrayList<Repository>();

	/**
	 * The list of resources, from all repositories
	 */
	private final List<Resource> allResources = new ArrayList<Resource>();



	public OBRManager(OBRMan obrman, CompositeType context, Model model) {
		this.obrMan 	= obrman;
		this.context	= context;
		this.model		= model;
		refresh();
	}
	
	public Model getModel() {
		return model;
	}

	public CompositeType getContext() {
		return context;
	}

	public void refresh() {
		repositories = obrMan.loadRepositories(this);
		
		allResources.clear();
		for (Repository repository : repositories) {
			allResources.addAll(Arrays.asList(repository.getResources()));
		}

	}

	/**
	 * Deploys, installs and instantiate
	 * 
	 * @param selected
	 * @return
	 */
	public boolean deployInstall(Selected selected) {
		// first check if res is not under deployment by another thread.
		// and remove when the deployment is done.

		boolean deployed = false;
		
		Resolver resolver = obrMan.getResolver(this);
		
		/*
		 *  the events sent by iPOJO for the previous deployed bundle may interfere and
		 * change the state of the local repository, which produces the IllegalStateException.
		 */
		while (!deployed) {
			try {

				resolver.add(selected.resource);
				// printRes(res);
				if (resolver.resolve()) {
					resolver.deploy(Resolver.START);
					return true;
				}
				deployed = true;
			} catch (IllegalStateException e) {
				logger.debug("OBR changed state. Resolving again " + selected.resource.getSymbolicName());
			}
			catch (Exception e) {
				logger.error ("Deployment of " + selected.selectedComponentName + " failed. ") ;
				return false ;
			}
		}

		Reason[] reqs = resolver.getUnsatisfiedRequirements();
		for (Reason req : reqs) {
			logger.error("Unable to resolve: " + req.getRequirement());
		}
		return false;
	}



	// serious stuff now !
	public String getAttributeInResource(Resource res, String capability, String attr) {
		for (Capability aCap : res.getCapabilities()) {
			if (aCap.getName().equals(capability)) {
				return (String) (aCap.getPropertiesAsMap().get(attr));
			}
		}
		return null;
	}

	/**
	 * Determines which resource is preferred to deliver the required
	 * capability. This method selects the resource providing the highest
	 * version of the capability. If two resources provide the same version of
	 * the capability, the resource with the largest number of cabailities be
	 * preferred
	 * 
	 * @param allSelected
	 * @return
	 */
	private Selected getBestCandidate(Set<Selected> allSelected) {
		Version bestVersion = null;
		Selected best = null;
		boolean bestLocal = false;

		// for(int capIdx = 0; capIdx < caps.size(); capIdx++)
		for (Selected current : allSelected) {
			// ResourceCapability current = (ResourceCapability)
			// caps.get(capIdx);
			boolean isCurrentLocal = current.getResource().isLocal();

			if (best == null) {
				best = current;
				bestLocal = isCurrentLocal;
				Object v = current.getCapability().getPropertiesAsMap().get(Resource.VERSION);
				if ((v != null) && (v instanceof Version)) {
					bestVersion = (Version) v;
				}
			}
			// m_resolutionFlags = flags; int parameter of the resolve method:
			else if (/* (Resolver.START & Resolver.DO_NOT_PREFER_LOCAL) != 0 || */!bestLocal || isCurrentLocal) {
				Object v = current.getCapability().getPropertiesAsMap().get(Resource.VERSION);

				// If there is no version, then select the resource
				// with the greatest number of capabilities.
				if ((v == null) && (bestVersion == null) && (best.getResource().getCapabilities().length < current.getResource().getCapabilities().length)) {
					best = current;
					bestLocal = isCurrentLocal;
					bestVersion = null;
				} else if ((v != null) && (v instanceof Version)) {
					// If there is no best version or if the current
					// resource's version is lower, then select it.
					if ((bestVersion == null) || (bestVersion.compareTo(v) < 0)) {
						best = current;
						bestLocal = isCurrentLocal;
						bestVersion = (Version) v;
					}
					// If the current resource version is equal to the
					// best, then select the one with the greatest
					// number of capabilities.
					else if ((best.getResource().getCapabilities().length < current.getResource().getCapabilities().length)) {
						best = current;
						bestLocal = isCurrentLocal;
						bestVersion = (Version) v;
					}
				}
			}
		}

		return (best == null) ? null : best;
	}


	private void logFilterConstraintPreferences(String filterStr, RelToResolve dep) {
		StringBuffer debugMessage = new StringBuffer();
		if (dep.isMultiple()) {
			debugMessage.append("OBR: looking for all " + dep.getTargetKind() + " matching " + filterStr);
		} else {
			debugMessage.append("OBR: looking for a " + dep.getTargetKind() + "  matching" + filterStr);
		}
	}


	public class Selected  {
		Resource resource;
		Capability capability;
		public OBRManager obrManager;
		private final String selectedComponentName;

		// private final String selectedComponentType;

		public Selected(Resource res, Capability cap, OBRManager managerPrivate) {
			this.obrManager = managerPrivate;
			this.resource = res;
			this.capability = cap;
			if (res == null || cap == null || managerPrivate == null) {
				new Exception("Invalid constructor for Selected").printStackTrace();
			}

			this.selectedComponentName = getAttributeInCapability(capability, CST.NAME);
			if (selectedComponentName == null) {
				new Exception("name is null in capability " + capability).printStackTrace();
			}

			// this.selectedComponentType = getAttributeInCapability
			// (capability, CST.COMPONENT_TYPE) ;
		}

		public Set<String> getComponents() {
			Set<String> components = new HashSet<String>();
			for (Capability aCap : resource.getCapabilities()) {
				if (aCap.getName().equals(CST.CAPABILITY_COMPONENT)) {
					components.add(getAttributeInCapability(aCap, CST.NAME));
				}
			}
			return components;
		}

		public URL getBundelURL() {
			URL url = null;
			try {
				url = new URL(resource.getURI());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			return url;
		}

		// @Override
		public Capability getCapability() {
			return capability;
		}

		public String getComponentName() {
			return this.selectedComponentName;
		}


		public Set<String> getInstancesOfSelectedImpl() {
			Set<String> components = new HashSet<String>();
			for (Capability aCap : resource.getCapabilities()) {
				if (aCap.getName().equals(CST.CAPABILITY_COMPONENT) && CST.INSTANCE.equals(aCap.getPropertiesAsMap().get(CST.COMPONENT_TYPE))) {
					if (selectedComponentName.equals(aCap.getPropertiesAsMap().get(CST.IMPLNAME))) {
						components.add(getAttributeInCapability(aCap, CST.NAME));
					}
				}
			}
			return components;
		}

		// // Generalize not used for now
		// private Set<String> getComponentsByType(String componentName,String
		// type) {
		//
		// Set<String> components = new HashSet<String> () ;
		// if (type== null) return components;
		// for (Capability aCap : resource.getCapabilities()) {
		// if (aCap.getName().equals(CST.CAPABILITY_COMPONENT) &&
		// aCap.getPropertiesAsMap().get(CST.COMPONENT_TYPE).equals(type) ) {
		// if (
		// aCap.getPropertiesAsMap().get(CST.IMPLNAME).equals(componentName))
		// components.add(getAttributeInCapability(aCap, CST.NAME)) ;
		// }
		// }
		// return components ;
		// }

		// private Set<String> getImplementationsOfSelectedCap(){
		// return getComponentsByType(getComponentName(),CST.IMPLEMENTATION);
		// }

		// @Override
		public Resource getResource() {
			return resource;
		}

	}


	/**
	 * 
	 * @param capability
	 *            : an OBR capability
	 * @param filterStr
	 *            : a single constraint like "(impl-name=xyz)" Should not be
	 *            null
	 * @param constraints
	 *            : the other constraints. can be null
	 * @param preferences
	 *            : the preferences. can be null
	 * @return the pair capability,
	 */

	public Selected lookFor(String capability, String filterStr, RelToResolve dep) {

		// Take care of preferences !
		if (filterStr == null) {
			logger.debug("No filter for lookFor");
			return null;
		}

		// Trace constraints filter
		// logFilterConstraintPreferences(filterStr, dep, false);

		Set<Selected> allSelected = lookForAll(capability, filterStr, dep);
		if (allSelected == null || allSelected.isEmpty()) {
			// logger.debug("   Not Found in " + compositeTypeName +
			// "  repositories : " + repositoriesToString());
			return null;
		}
		return getBestCandidate(allSelected);
	}

	@SuppressWarnings("unchecked")
	private Set<Selected> lookForAll(String capability, String filterStr, RelToResolve dep) { // Set<ApamFilter> constraints) {
		if (filterStr == null) {
			new Exception("no filter in lookfor all").printStackTrace();
		}

		Set<Selected> allRes = new HashSet<Selected>();

		// Trace
		logFilterConstraintPreferences(filterStr, dep);

		if (allResources.isEmpty()) {
			logger.debug("no resources in OBR");
			return null;
		}
		try {
			ApamFilter filter = ApamFilter.newInstance(filterStr);
			for (Resource res : allResources) {
				// if (obrMan.bundleExists(res.getSymbolicName())){
				// continue;
				// }
				Capability[] capabilities = res.getCapabilities();
				ComponentKind candidateKind;
				for (Capability aCap : capabilities) {
					if (aCap.getName().equals(capability)) {
						if (filter.matchCase(aCap.getPropertiesAsMap())) {
							candidateKind = toKind(aCap);
							if (dep == null || dep.matchRelationConstraints(candidateKind, aCap.getPropertiesAsMap())) {
								// candidateKind =
								// getAttributeInCapability(aCap,
								// CST.COMPONENT_TYPE);
								// ignore if found a matching specification, but
								// an implementation or instance is required
								// if (!!!("specification".equals(candidateKind)
								// && dep.getTargetKind() !=
								// ComponentKind.SPECIFICATION)) {
								if (!!!(candidateKind.equals(ComponentKind.SPECIFICATION) && dep.getTargetKind() != ComponentKind.SPECIFICATION)) {
									allRes.add(new Selected(res, aCap, this));
									logger.debug("Found " + candidateKind + " " + getAttributeInCapability(aCap, CST.NAME) + " in bundle " + res.getSymbolicName() + " From " + context.getName() + " repositories : " + model.getRepositories());
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (allRes.isEmpty()) {
			logger.debug("Not Found " + dep.getTargetKind() + " matching " + filterStr);
		}

		return allRes;
	}

	/**
	 * Return the resource with the given symbolic name and containing the given
	 * component
	 * 
	 * @param symbolicName
	 * @param componentName
	 * @return
	 */
	public Selected lookForBundle(String symbolicName, String componentName) {
		for (Resource res : allResources) {
			if (!res.getSymbolicName().equals(symbolicName)) {
				continue;
			}
			Capability[] capabilities = res.getCapabilities();
			for (Capability aCap : capabilities) {
				if (aCap.getName().equals(CST.CAPABILITY_COMPONENT) && getAttributeInCapability(aCap, CST.NAME).equals(componentName)) {
					return new Selected(res, aCap, this);
				}
			}
		}
		return null;
	}

	/*
	 * Utility methods to manipulate resources and capabilities
	 */
	private static String getAttributeInCapability(Capability aCap, String attr) {
		return (String) (aCap.getPropertiesAsMap().get(attr));
	}

	private static ComponentKind toKind(Capability aCap) {
		String candidateKindS = getAttributeInCapability(aCap, CST.COMPONENT_TYPE);
		if (CST.SPECIFICATION.equals(candidateKindS)) {
			return ComponentKind.SPECIFICATION;
		}
		if (CST.IMPLEMENTATION.equals(candidateKindS)) {
			return ComponentKind.IMPLEMENTATION;
		}
		if (CST.INSTANCE.equals(candidateKindS)) {
			return ComponentKind.INSTANCE;
		}
		return ComponentKind.COMPONENT;
	}

}
