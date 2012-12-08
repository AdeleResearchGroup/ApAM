package fr.imag.adele.apam.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.DynamicManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.ResolvableReference;

public class UpdateMan implements DependencyManager, DynamicManager {

	private static Set<String> deployed = new HashSet<String> () ;
	static Logger logger = LoggerFactory.getLogger(UpdateMan.class);

	@Override
	public String getName() {
		return CST.UPDATEMAN;
	}
	@Override
	public int getPriority() {
		return -2; //WARNING !! before ApamMan
	}
	// when in Felix.
	public void start() {
		System.out.println("UpdateMan started");
	}
	public void stop() {}

	public UpdateMan(){}
	public UpdateMan(BundleContext context){
		//this.context = context;
	}


	/**
	 * The component compo needs to be updated.
	 * Compo must be existing.
	 * We will be looking for a bundle with same id as the one from which compo was initially loaded.
	 * If no such bundle is found, does nothing: it is not an update, but a normal installation.
	 * 
	 * Since all the components in the previous bundle will be uninstalled, 
	 * we have to record the components that will be re-installed in order 
	 * to wait for these new versions to be ready,
	 * in case the client accesses the variable during the bundle loading.
	 * 
	 * @param compo
	 * @return
	 */
	public static void updateComponent (Implementation impl) {
		try {
			Bundle bundle = impl.getApformComponent().getBundle ();
			String implName = impl.getName() ;

			//return the composite type that physically deployed the bundle
			CompositeType compoTypeFrom = impl.getFirstDeployed();  

			List<DependencyManager> selectionPath = ApamManagers.getManagers();
			logger.info("Updating implementation " + implName + " in composite " + compoTypeFrom );

			ComponentBundle sel = null;
			for (DependencyManager manager : selectionPath) {
				if (manager.getName().equals(CST.APAMMAN) || manager.getName().equals(CST.UPDATEMAN)) continue ;
				logger.debug(manager.getName() + "  ");
				sel = manager.findBundle(compoTypeFrom, bundle.getSymbolicName(), implName);

				if (sel != null && sel.getComponents().contains(implName)) { //it is indeed a deployment
					logger.info("Updating component " + implName + " in composite " + compoTypeFrom 
							+ ".\n     From bundle: " + sel.getBundelURL());
					UpdateMan.addDeployed (sel, implName) ;
					try {
						/**
						 * WARNING: The new bundle may not start 
						 * if the new bundle has a new package dependency not currently satisfied
						 */
						bundle.update(sel.getBundelURL().openStream()) ;
					} catch (BundleException e) {					
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) { 
			e.printStackTrace() ;
		}
	}

	/**
	 * This method is to be called when a bundle is about to be updated. 
	 * The new bundle contains the components whose name pertain to the set "sel.getComponents()" . 
	 * Therefore, any find or resolution related to these components should wait for the component to be available. 
	 * WARNING: if the component never appears, the client may be locked forever.
	 * 
	 * @param sel: the information about the bundle to deploy.
	 */
	public static void addDeployed (ComponentBundle sel, String name) {
		if (sel == null || sel.getComponents() == null) {
			System.out.println("no component to update ???");
			return ;
		}

		/*
		 * Be sure that the list is atomically updated
		 */
		synchronized (deployed) {
			deployed.addAll(sel.getComponents());
		}

	}

	/**
	 * Must be called when a component appears. If it was an update, its name is in the "deployed" set, 
	 * and it must be removed from that list. 
	 * @param component
	 */
	@Override
	public void addedInApam(Component newComponent) {
		logger.debug("Added : " + newComponent);

		/*
		 * notifications can originate concurrently with updates, so we need to synchronize access
		 * to the list of currently updating components.
		 */
		synchronized (deployed) {
			deployed.remove(newComponent.getName());
		}		
	}

	/**
	 * The current caller requires the component with name "name". 
	 * If this component is under deployment, the current thread must wait until the component appears.
	 * 
	 * @param name
	 */
	private void waitComponent (String name) {

		/*
		 * First try the fast case when there is no pending updates for this component
		 */
		synchronized (deployed) {
			if (!deployed.contains(name))
				return;
		}

		/*
		 * we wait for the component. 
		 * 
		 * INPORTANT Notice that we wait outside the synchronized block, because we must let notifications
		 * proceed while we wait. Otherwise this would lead to a deadlock between the thread requiring the
		 * component and the thread performing the deployment.
		 */

		logger.info("Waiting for " + name + " update.");
		Apform2Apam.waitForComponent(name) ;
		logger.info(name + " update done.");			
	}

	@Override
	public void getSelectionPath(Instance client, DependencyDeclaration dep, List<DependencyManager> selPath) {
	}

	@Override
	public Instance resolveImpl(Instance client, Implementation impl, DependencyDeclaration dep) {
		waitComponent (impl.getName()) ;
		return null;
	}

	@Override
	public Set<Instance> resolveImpls(Instance client, Implementation impl, DependencyDeclaration dep) {
		waitComponent (impl.getName());
		return null;
	}

	@Override
	public void newComposite(ManagerModel model, CompositeType composite) {
	}

	@Override
	public Implementation findImplByName(Instance client, String implName) {
		waitComponent (implName)  ;
		return null;
	}

	@Override
	public Specification findSpecByName(Instance client, String specName) {
		waitComponent (specName);
		return null;
	}

	@Override
	public Set<Implementation> resolveDependency(Instance client, DependencyDeclaration dep, Set<Instance> insts) {
		Specification spec = CST.componentBroker.getSpecResource(dep.getTarget());
		if (spec == null) return null;

		waitComponent (spec.getName());
		return null;
	}


	@Override
	public Instance findInstByName(Instance client, String instName) {
		waitComponent(instName);
		return null;
	}


	@Override
	public Component findComponentByName(Instance client, String componentName) {
		waitComponent(componentName);
		return null;
	}

	
	@Override
	public ComponentBundle findBundle(CompositeType context, String bundleSymbolicName, String componentName) {
		return null;
	}

	@Override
	public void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl, Instance inst,
			Set<Instance> insts) {
		// do not care
	}

	@Override
	public void removedFromApam(Component lostComponent) {
		logger.debug("Removed : " + lostComponent);
	}



}
