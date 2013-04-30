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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
//import org.osgi.framework.Filter;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Dependency;
import fr.imag.adele.apam.DependencyManager.ComponentBundle;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.util.ApamFilter;

public class OBRManager {

	private Resolver       resolver;

	private LinkedProperties obrModel;

	private List<Repository>     repositories= new ArrayList<Repository>();

	private final List<Resource> allResources =  new ArrayList<Resource>();

	private final String         compositeTypeName;

	private final OBRMan         obrMan;

	private final Logger         logger = LoggerFactory.getLogger(OBRManager.class);

	private final Repository     runningbundles;

	private final Repository     systembundle;

	private File settings;

	public OBRManager(OBRMan obrman, String compositeTypeName, RepositoryAdmin repoAdmin, LinkedProperties obrModel1) {
		this.compositeTypeName = compositeTypeName;
		runningbundles = repoAdmin.getLocalRepository();
		systembundle = repoAdmin.getSystemRepository();
		obrMan = obrman;
		this.obrModel = obrModel1;
		updateListOfResources(repoAdmin);
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

	public String getAttributeInCapability(Capability aCap, String attr) {
		return (String) (aCap.getPropertiesAsMap().get(attr));
	}

	/**
	 * Return the resource with the given symbolic name and containing the given component
	 * @param symbolicName
	 * @param componentName
	 * @return
	 */
	public Selected lookForBundle (String symbolicName, String componentName) {
		for (Resource res : allResources) {
			if (!res.getSymbolicName().equals(symbolicName)) continue ;
			Capability[] capabilities = res.getCapabilities();
			for (Capability aCap : capabilities) {
				if (aCap.getName().equals(CST.CAPABILITY_COMPONENT)
						&& getAttributeInCapability(aCap, CST.NAME).equals(componentName)) {
					return new Selected (res, aCap, this) ;
				}
			}
		}
		return null ;
	}

	public Set<Selected> lookForAll(String capability, String filterStr, Dependency dep) { //Set<ApamFilter> constraints) {
		if (filterStr == null)
			new Exception("no filter in lookfor all").printStackTrace();

		Set<Selected> allRes = new HashSet<Selected>();

		// Trace preference filter
		logFilterConstraintPreferences(filterStr, dep, true);

		if (allResources.isEmpty()) {
			logger.debug("no resources in OBR");
			return null;
		}
		try {
			ApamFilter filter = ApamFilter.newInstance(filterStr, false);
			for (Resource res : allResources) {
				if (obrMan.bundleInactif(res.getSymbolicName())){
					continue;
				}
				Capability[] capabilities = res.getCapabilities();
				for (Capability aCap : capabilities) {
					if (aCap.getName().equals(capability)) {
						if (filter.matchCase(aCap.getPropertiesAsMap())) {
							//                            if ((constraints == null) || matchConstraints(aCap, constraints) ) {
							if (dep == null || dep.matchDep (aCap.getPropertiesAsMap())) {
								logger.debug("-->Component " + getAttributeInCapability(aCap, CST.NAME)
										+ " found in bundle : " + res.getSymbolicName() + " From "
										+ compositeTypeName + " repositories : \n   " + repositoriesToString());
								allRes.add(new Selected(res, aCap, this));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (allRes.isEmpty())
			logger.debug("   Not Found in " + compositeTypeName + "  repositories : " + repositoriesToString());
		return allRes;
	}

	public Selected lookForPref(String capability, Dependency dep, Set<Selected> candidates) {
		Selected winner = lookForPrefInt(capability, dep, candidates) ;
		if (winner == null)
			return null;
		logger.debug("   Best bundle : " + winner.resource.getSymbolicName() + " Component:  "
				+ getAttributeInCapability(winner.capability, CST.IMPLNAME) + " from "
				+ compositeTypeName + "  repositories : " + repositoriesToString());
		return winner;
	}

	/**
	 * Returns the candidate that best matches the preferences.
	 * Take the preferences in orden: m candidates
	 * find  the n candidates that match the constraint.
	 * 		if n= 0 ignore the constraint
	 *      if n=1 return it.
	 * iterate with the n candidates.
	 * At the end, if n > 1 return one arbitrarily.
	 *
	 */
	public Selected lookForPrefInt(String capability, Dependency dep, Set<Selected> candidates) {
		if (candidates == null || candidates.isEmpty()) return null ;
		// Trace preference filter
		logFilterConstraintPreferences(null, dep, false);

		List<ApamFilter> preferences ;
		switch (dep.getTargetType()) {
			case IMPLEMENTATION : preferences = dep.getImplementationPreferenceFilters () ;
				break ;
			case INSTANCE : preferences = dep.getInstancePreferenceFilters () ;
				break ;
			default : preferences = null ;
			//TODO Should add specification constraints
		    //case SPECIFICATION : preferences = null ; 
		}

		if ((preferences == null) || preferences.isEmpty())
			return (Selected)candidates.toArray()[0] ;

		Set<Selected> valids = new HashSet<Selected> ();
		ApamFilter filter;
		for (ApamFilter f : preferences) {
			//filter = ApamFilter.newInstance(f);
			for (Selected compo : candidates) {
				if (f.matchCase(compo.capability.getPropertiesAsMap()))
					valids.add (compo) ;
			}
			if (valids.size()==1) return (Selected)valids.toArray()[0] ;
			if (!valids.isEmpty()) {
				candidates = valids ;
				valids=new HashSet<Selected> () ;
			}
		}
		return (Selected)candidates.toArray()[0] ;
	}

	/**
	 *
	 * @param capability: an OBR capability
	 * @param filterStr: a single constraint like "(impl-name=xyz)" Should not be null
	 * @param constraints: the other constraints. can be null
	 * @param preferences: the preferences. can be null
	 * @return the pair capability,
	 */
//	public Selected lookFor(String capability, String filterStr, Dependency dep) {
//		if ((preferences != null) && !preferences.isEmpty()) {
//			return lookForPref(capability, dep, lookForAll(capability, filterStr, dep));
//		}
//		return lookFor(capability, filterStr, dep);
//	}

	/*private*/ public Selected lookFor(String capability, String filterStr, Dependency dep) {
		
		// Take care of preferences !
		//TODO 
		if (filterStr == null) {
			logger.debug("No filter for lookFor");
			return null;
		}

		// Trace constraints filter
		logFilterConstraintPreferences(filterStr, dep, false);

		Set<Selected> allSelected = lookForAll(capability, filterStr, dep) ;
		if (allSelected == null || allSelected.isEmpty()) {
			logger.debug("   Not Found in " + compositeTypeName + "  repositories : " + repositoriesToString());
			return null;
		}
		return getBestCandidate(allSelected) ;
	}

	private void logFilterConstraintPreferences(String filterStr, Dependency dep, boolean all) {
		Set<ApamFilter> constraints  ;
		List<ApamFilter> preferences ;
		switch (dep.getTargetType()) {
		case IMPLEMENTATION : 
			constraints = dep.getAllImplementationConstraintFilters ();
			preferences = dep.getImplementationPreferenceFilters () ;
			break ;
		case INSTANCE : preferences = dep.getInstancePreferenceFilters () ;
			constraints = dep.getAllInstanceConstraintFilters () ;
			preferences = dep.getInstancePreferenceFilters () ;
			break ;
			default : 
				constraints = null ;
				preferences = null ;
		}
		
		StringBuffer debugMessage = new StringBuffer ();
		if (filterStr != null) {
			if (all) {
				debugMessage.append("OBR: looking for all " + dep.getTargetType() + " matching " + filterStr);
			} else {
				debugMessage.append("OBR: looking for a " + dep.getTargetType() + "  matching" + filterStr);
			}
		}
		if ((constraints != null) && !constraints.isEmpty()) {
			debugMessage.append("\n     Constraints : ");
			for (ApamFilter constraint : constraints) {
				debugMessage.append(constraint + ", ");
			}
		}

		if ((preferences != null) && !preferences.isEmpty()) {
			debugMessage.append("\n    Preferences : ");
			for (ApamFilter preference : preferences) {
				debugMessage.append(preference + ", ");
			}
		}

		logger.debug(debugMessage.toString());
	}

//	/**
//	 * return true if the provided capability has an implementation that satisfies the constraints
//	 *
//	 * @param aCap
//	 * @param constraints
//	 * @return
//	 */
//	private boolean matchConstraints(Capability aCap, Dependency dep) {
////		if ((constraints == null) || constraints.isEmpty() || (aCap == null))
//		if (dep == null)
//			return true;
//
//		//ApamFilter filter;
//		Map<String, Object> map = (Map<String, Object>)aCap.getPropertiesAsMap();
//		return dep.matchDep(map) ;
//		//       String kindS = ((String)map.get("type")) ;
//
//		//        for (ApamFilter constraint : constraints) {
//		//            //filter = ApamFilter.newInstance(constraint);
//		//            if (!constraint.match(map)) {
//		//                return false;
//		//            }
//		//        }
//		//        return true;
//	}

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
		// the events sent by iPOJO for the previous deployed bundle may interfere and
		// change the state of the local repository, which produces the IllegalStateException.
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
		}

		Reason[] reqs = resolver.getUnsatisfiedRequirements();
		for (Reason req : reqs) {
			logger.error("Unable to resolve: " + req.getRequirement());
		}
		return false;
	}

	protected List<Repository> getRepositoriesFromModel(LinkedProperties obrModel, RepositoryAdmin repoAdmin) {
		List<Repository> declaredRepositories = new ArrayList<Repository>();
		Enumeration<?> keys = obrModel.keys();
		while (keys.hasMoreElements()) {

			String key = (String) keys.nextElement();
			if (ObrUtil.LOCAL_MAVEN_REPOSITORY.equals(key)) {
				// Add the obr repository located in the local maven repository
				Boolean localMavenOBRRepo = new Boolean(obrModel.getProperty(key));
				if (localMavenOBRRepo) {
					URL localMavenObrUrl = findLocalMavenRepository();
					if (localMavenObrUrl==null){
						logger.error("localRepository not found in : " + settings);
					}
					try {
						declaredRepositories.add(repoAdmin.addRepository(localMavenObrUrl));
					} catch (Exception e) {
						logger.error("Error when adding default local repository to repoAdmin",e.getCause());
					}
				}
			} else if (ObrUtil.DEFAULT_OSGI_REPOSITORIES.equals(key)) {
				// Add obr repositories declared in the osgi configuration file
				Boolean osgiRepo = new Boolean(obrModel.getProperty(key));
				if (osgiRepo) {
					String repos = obrMan.getDeclaredOSGiOBR();
					if (repos != null) {
						declaredRepositories.addAll(getRepositoriesFromArray(repoAdmin, repos.split("\\s+")));
					}
				}
			} else if (ObrUtil.REPOSITORIES.equals(key)) {
				// Add obr repositories declared in the composite
				declaredRepositories
				.addAll(getRepositoriesFromArray(repoAdmin, obrModel.getProperty(key).split("\\s+")));

			} else if (ObrUtil.COMPOSITES.equals(key)) {
				// look for obr repositories in other composites
				String[] otherCompositesRepositories = obrModel.getProperty(key).split("\\s+");
				for (String compoTypeName : otherCompositesRepositories) {
					OBRManager manager = obrMan.getOBRManager(compoTypeName);
					if (manager != null) {
						declaredRepositories.addAll(manager.getRepositories());
					} else {
						// If the compositeType is not present, do nothing
						logger.error("The composite " + compositeTypeName + " reference a missing compiste "
								+ compoTypeName);
					}
				}
			}
		}

		return declaredRepositories;
	}

	protected Collection<Repository> getRepositoriesFromArray(RepositoryAdmin repoAdmin, String[] repos) {
		List<Repository> repoList = new ArrayList<Repository>();
		for (String repoUrlStr : repos) {
			try {
				URL url = new URL(repoUrlStr);

				repoList.add(repoAdmin.addRepository(url));
			} catch (Exception e) {
				logger.error("Invalid OBR repository address :" + repoUrlStr,e.getCause());
			}
		}
		return repoList;
	}

	public String getCompositeTypeName() {
		return compositeTypeName;
	}

	public class Selected implements ComponentBundle {
		Resource   resource;
		Capability capability;
		public OBRManager obrManager ;
		private final String   selectedComponentName;
		//        private final String   selectedComponentType;

		public Selected(Resource res, Capability cap, OBRManager managerPrivate) {
			this.obrManager = managerPrivate;
			this.resource = res;
			this.capability = cap;
			if (res == null || cap == null || managerPrivate == null) {
				new Exception("Invalid constructor for Selected").printStackTrace();
			}

			this.selectedComponentName =  getAttributeInCapability (capability, CST.NAME) ;
			if (selectedComponentName == null) {
				new Exception("name is null in capability " + capability).printStackTrace();
			}

			//            this.selectedComponentType = getAttributeInCapability (capability, CST.COMPONENT_TYPE) ;
		}

		//@Override
		public Resource getResource() {
			return resource;
		}

		public String getComponentName () {
			return this.selectedComponentName;
		}
		//@Override
		public Capability getCapability() {
			return capability;
		}

		@Override
		public URL getBundelURL() {
			URL url = null ;
			try {
				url = new URL(resource.getURI());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			return url;
		}

		@Override
		public Set<String> getComponents() {
			Set<String> components = new HashSet<String> () ;
			for (Capability aCap : resource.getCapabilities()) {
				if (aCap.getName().equals(CST.CAPABILITY_COMPONENT)) {
					components.add(getAttributeInCapability(aCap, CST.NAME)) ;
				}
			}
			return components ;
		}

		//        // Generalize not used for now
		//        private Set<String> getComponentsByType(String componentName,String type) {
			//
			//            Set<String> components = new HashSet<String> () ;
		//            if (type== null) return components;
		//            for (Capability aCap : resource.getCapabilities()) {
		//                if (aCap.getName().equals(CST.CAPABILITY_COMPONENT)  && aCap.getPropertiesAsMap().get(CST.COMPONENT_TYPE).equals(type) ) {
		//                    if ( aCap.getPropertiesAsMap().get(CST.IMPLNAME).equals(componentName))
		//                        components.add(getAttributeInCapability(aCap, CST.NAME)) ;
		//                }
		//            }
		//            return components ;
		//        }

		//        private Set<String> getImplementationsOfSelectedCap(){
		//            return getComponentsByType(getComponentName(),CST.IMPLEMENTATION);
		//        }

		public Set<String> getInstancesOfSelectedImpl(){
			Set<String> components = new HashSet<String> () ;
			for (Capability aCap : resource.getCapabilities()) {
				if (aCap.getName().equals(CST.CAPABILITY_COMPONENT)  && CST.INSTANCE.equals(aCap.getPropertiesAsMap().get(CST.COMPONENT_TYPE)) ) {
					if ( selectedComponentName.equals(aCap.getPropertiesAsMap().get(CST.IMPLNAME)) )
						components.add(getAttributeInCapability(aCap, CST.NAME)) ;
				}
			}
			return components ;
		}
	}

	/**
	 * Determines which resource is preferred to deliver the required capability.
	 * This method selects the resource providing the highest version of the capability.
	 * If two resources provide the same version of the capability, the resource with
	 * the largest number of cabailities be preferred
	 * @param allSelected
	 * @return
	 */
	private Selected getBestCandidate(Set<Selected> allSelected)
	{
		Version bestVersion = null;
		Selected best = null;
		boolean bestLocal = false;

		//        for(int capIdx = 0; capIdx < caps.size(); capIdx++)
		for (Selected current : allSelected)
		{
			// ResourceCapability current = (ResourceCapability) caps.get(capIdx);
			boolean isCurrentLocal = current.getResource().isLocal();

			if (best == null)
			{
				best = current;
				bestLocal = isCurrentLocal;
				Object v = current.getCapability().getPropertiesAsMap().get(Resource.VERSION);
				if ((v != null) && (v instanceof Version))
				{
					bestVersion = (Version) v;
				}
			}
			//        m_resolutionFlags = flags; int parameter of the resolve method:
				else if (/* (Resolver.START & Resolver.DO_NOT_PREFER_LOCAL) != 0 || */ !bestLocal || isCurrentLocal)
				{
					Object v = current.getCapability().getPropertiesAsMap().get(Resource.VERSION);

					// If there is no version, then select the resource
					// with the greatest number of capabilities.
					if ((v == null) && (bestVersion == null)
							&& (best.getResource().getCapabilities().length
									< current.getResource().getCapabilities().length))
					{
						best = current;
						bestLocal = isCurrentLocal;
						bestVersion = null;
					}
					else if ((v != null) && (v instanceof Version))
					{
						// If there is no best version or if the current
						// resource's version is lower, then select it.
						if ((bestVersion == null) || (bestVersion.compareTo(v) < 0))
						{
							best = current;
							bestLocal = isCurrentLocal;
							bestVersion = (Version) v;
						}
						// If the current resource version is equal to the
						// best, then select the one with the greatest
						// number of capabilities.
						else if ((best.getResource().getCapabilities().length
								< current.getResource().getCapabilities().length))
						{
							best = current;
							bestLocal = isCurrentLocal;
							bestVersion = (Version) v;
						}
					}
				}
		}

		return (best == null) ? null : best;
	}

	protected URL findLocalMavenRepository() {

		// try to find the maven settings.xml file
		settings = ObrUtil.searchSettingsFromM2Home();
		if (settings == null) {
			settings = ObrUtil.searchSettingsFromUserHome();
		}
		logger.debug("Maven settings location: " + settings);

		// Extract localRepository from settings.xml
		URL defaultLocalRepo = null;
		if (settings != null) {
			defaultLocalRepo = ObrUtil.searchMavenRepoFromSettings(settings);
		}

		if (defaultLocalRepo == null) {
			// Special case for Linux Server :
			defaultLocalRepo = ObrUtil.searchRepositoryFromDefaultLinux();
		}
		if (defaultLocalRepo != null) {
			return defaultLocalRepo;
		}
		return null;
	}

	public List<Repository> getRepositories() {
		List<Repository> tempList = new ArrayList<Repository>();
		tempList.addAll(repositories);
		tempList.remove(runningbundles);
		tempList.remove(systembundle);
		return tempList;
	}

	public Repository getRunningResources(){
		return runningbundles;
	}

	//
	protected List<String> repositoriesToString() {
		List<String> repoString = new ArrayList<String>();
		for (Repository repo : getRepositories()) {
			repoString.add(repo.getURI());
		}
		return repoString;
	}

	protected void updateListOfResources(RepositoryAdmin repoAdmin){
		// First Read model if it exist
		if (obrModel != null) {
			repositories = getRepositoriesFromModel(obrModel, repoAdmin);
		}
		allResources.clear();
		// Get resources from repositories and remove them from repoAdmin.
		for (Repository repository : repositories) {
			allResources.addAll(Arrays.asList(repository.getResources()));
			repoAdmin.removeRepository(repository.getURI());
		}

		// Add the system as repository
		repositories.add(0, runningbundles);
		repositories.add(0, systembundle);
		resolver = repoAdmin.resolver(repositories.toArray(new Repository[repositories.size()]));
	}
}
