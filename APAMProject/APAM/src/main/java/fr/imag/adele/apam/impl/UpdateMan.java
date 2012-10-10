package fr.imag.adele.apam.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.ResolvableReference;

public class UpdateMan implements DependencyManager {

	private static Set<String> deployed = new HashSet<String> () ;
	static Logger logger = LoggerFactory.getLogger(ApamResolverImpl.class);

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
	 * The component compo (spec or implem) needs to be updated.
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
	public static void updateComponent (Component compo) {
		if (compo instanceof Instance) {
			logger.error ("Cannot update an Instance " + compo.getName()) ;
			return ;
		}
		Bundle bundle = compo.getApformComponent().getBundle ();
		String implName = compo.getName() ;

		//return the composite type that physically deployed the bundle
		CompositeType compoTypeFrom = ((ComponentImpl)compo).getFirstDeployed();  
		
		List<DependencyManager> selectionPath = ApamManagers.getManagers();

		ComponentBundle sel = null;
		logger.info("Looking for implementation " + implName + ": ");
		boolean deployed = false;
		for (DependencyManager manager : selectionPath) {
			if (manager.getName().equals(CST.APAMMAN) || manager.getName().equals(CST.UPDATEMAN)) continue ;
			logger.debug(manager.getName() + "  ");
			sel = manager.findBundle(compoTypeFrom, bundle.getSymbolicName());

			if (sel != null && sel.getComponents().contains(implName)) { //it is indeed a deployment
				UpdateMan.addDeployed (sel, implName) ;
				compo = manager.install(sel) ;
				if (compo instanceof Implementation)
					ApamResolverImpl.deployedImpl(compoTypeFrom, (Implementation)compo, deployed);
				return ;
			}
		}
	}
	
	/**
	 * This method is to be called when a bundle containing the components 
	 * whole name pertain to the set "toDeploy" are under deployment. 
	 * Therefore, any find or resolution related to these component should wait for the component to be available. 
	 * WARNING: if the component never appears, the client may be locked forever.
	 * 
	 * @param toDeploy: the set of components that are currently under deployment.
	 */
	public static void addDeployed (ComponentBundle sel, String name) {
		if (sel == null) return ;
		
		/*
		 * If the component is found and do not need to be deployed (ApamMan)
		 */
		if (sel.getComponents() != null) return ; 
		
		/*
		* Look if it will be an update. 
		*/
		deployed.addAll(sel.getComponents()) ;
	}
	
	/**
	 * The current caller requires the compoent with name "name". 
	 * If this compoent is under deployment, the current thread must wait until the component appears.
	 * 
	 * @param name
	 */
	private void waitComponent (String name) {
		if (deployed.contains(name)) {
			System.err.println("On attend le deploiement de " + name);
			Apform2Apam.waitForComponent(name) ;
			System.err.println("Deploiement de " + name + " terminé");			
			deployed.remove(name) ;
		}		
	}

	@Override
	public void getSelectionPath(CompositeType compTypeFrom, DependencyDeclaration dep, List<DependencyManager> selPath) {
	}

	@Override
	public Instance resolveImpl(Composite composite, Implementation impl, DependencyDeclaration dep) {
		waitComponent (impl.getName()) ;
		return null;
	}

	@Override
	public Set<Instance> resolveImpls(Composite composite, Implementation impl, DependencyDeclaration dep) {
		waitComponent (impl.getName());
		return null;
	}

	@Override
	public void newComposite(ManagerModel model, CompositeType composite) {
	}

	@Override
	public Implementation findImplByName(CompositeType compoType, String implName) {
		waitComponent (implName)  ;
		return null;
	}

	@Override
	public Specification findSpecByName(CompositeType compTypeFrom, String specName) {
		waitComponent (specName);
		return null;
	}

	@Override
	public Set<Implementation> resolveSpecs(CompositeType compoType, DependencyDeclaration dep) {
		Specification spec = CST.componentBroker.getSpecResource(dep.getTarget());
		if (spec == null) return null;

		waitComponent (spec.getName());
		return null;
	}

	@Override
	public Implementation resolveSpec(CompositeType compoType, DependencyDeclaration dep) {
		Specification spec = CST.componentBroker.getSpecResource(dep.getTarget());
		if (spec == null)
			return null;	

		waitComponent (spec.getName());
		return null;
	}


	@Override
	public void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl, Instance inst,
			Set<Instance> insts) {
		// do not care
	}


	@Override
	public Component findComponentByName(CompositeType compoType, String componentName) {
		waitComponent(componentName);
		return null;
	}

	@Override
	public Implementation install(ComponentBundle selected) {
		return null;
	}
	@Override
	public ComponentBundle findBundle(CompositeType compoType,
			String bundleSymbolicName) {
		return null;
	}


}
