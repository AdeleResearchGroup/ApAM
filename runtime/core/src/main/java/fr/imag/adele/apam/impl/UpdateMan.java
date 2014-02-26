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

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DeploymentManager;
import fr.imag.adele.apam.DynamicManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.RelationManager;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.Apform2Apam;


/**
 * This is a manager that handles intermediate states arriving when a resolution is
 * requested for a component being updated.
 * 
 * It implements a simple strategy that blocks all resolutions targeting the components
 * being updated.
 * 
 * @author vega
 *
 */
public class UpdateMan implements RelationManager, DynamicManager {

	private static Set<String> deployed = new HashSet<String>();
	static Logger logger = LoggerFactory.getLogger(UpdateMan.class);

	public UpdateMan() {
	}

	@Override
	public String getName() {
		return CST.UPDATEMAN;
	}

	/**
	 * This method is to be called when a bundle is about to be updated. The new
	 * bundle contains the components whose name pertain to the set
	 * "sel.getComponents()" . Therefore, any find or resolution related to
	 * these components should wait for the component to be available. WARNING:
	 * if the component never appears, the client may be locked forever.
	 * 
	 * @param sel
	 *            : the information about the bundle to deploy.
	 */
	public static void addDeployed(DeploymentManager.Unit deploying) {
		if (deploying == null || deploying.getComponents().isEmpty()) {
			System.out.println("no component to update ???");
			return;
		}

		/*
		 * Be sure that the list is atomically updated
		 */
		synchronized (deployed) {
			deployed.addAll(deploying.getComponents());
		}

	}

	/**
	 * Must be called when a component appears. If it was an update, its name is
	 * in the "deployed" set, and it must be removed from that list.
	 * 
	 * @param component
	 */
	@Override
	public void addedComponent(Component newComponent) {
		logger.debug("Added : " + newComponent);

		/*
		 * notifications can originate concurrently with updates, so we need to
		 * synchronize access to the list of currently updating components.
		 */
		synchronized (deployed) {
			deployed.remove(newComponent.getName());
		}
	}

	@Override
	public void removedComponent(Component lostComponent) {
		logger.debug("Removed : " + lostComponent);
	}

	@Override
	public void addedLink(Link wire) {
	}

	@Override
	public void removedLink(Link wire) {
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

	@Override
	public Resolved<?> resolve(RelToResolve dep) {
		
		Specification spec = CST.componentBroker.getSpecResource(dep.getTarget());
		if (spec == null) {
			return null;
		}

		waitComponent(spec.getName());
		return null;
	}

	/**
	 * The current caller requires the component with name "name". If this
	 * component is under deployment, the current thread must wait until the
	 * component appears.
	 * 
	 * @param name
	 */
	private void waitComponent(String name) {

		/*
		 * First try the fast case when there is no pending updates for this
		 * component
		 */
		synchronized (deployed) {
			if (!deployed.contains(name)) {
				return;
			}
		}

		/*
		 * we wait for the component.
		 * 
		 * INPORTANT Notice that we wait outside the synchronized block, because
		 * we must let notifications proceed while we wait. Otherwise this would
		 * lead to a deadlock between the thread requiring the component and the
		 * thread performing the deployment.
		 */

		logger.info("Waiting for " + name + " update.");
		Apform2Apam.waitForComponent(name);
		logger.info(name + " update done.");
	}


	/**
	 * The component compo needs to be updated. Compo must be existing. We will
	 * be looking for a bundle with same id as the one from which compo was
	 * initially loaded. If no such bundle is found, does nothing: it is not an
	 * update, but a normal installation.
	 * 
	 * Since all the components in the previous bundle will be uninstalled, we
	 * have to record the components that will be re-installed in order to wait
	 * for these new versions to be ready, in case the client accesses the
	 * variable during the bundle loading.
	 * 
	 * @param compo
	 * @return
	 */
	public static void updateComponent(Implementation component) {
		try {

			// return the composite type that physically deployed the bundle
			CompositeType compoTypeFrom = component.getFirstDeployed();

			logger.info("Updating implementation " + component.getName() + " in composite " + compoTypeFrom);

			for (DeploymentManager manager : ApamManagers.getDeploymentManagers()) {

				logger.debug(manager.getName() + "  ");
				DeploymentManager.Unit deployed = manager.getDeploymentUnit(compoTypeFrom, component);

				if (deployed != null && deployed.getComponents().contains(component.getName())) { 
					// it is indeed a deployment
					UpdateMan.addDeployed(deployed);
					
					/**
					 * WARNING: The new bundle may not start if the new
					 * bundle has a new package relation not currently
					 * satisfied
					 */
					deployed.update();
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	// when in Felix.
	public void start() {
		System.out.println("UpdateMan started");
	}

	public void stop() {
	}

}
