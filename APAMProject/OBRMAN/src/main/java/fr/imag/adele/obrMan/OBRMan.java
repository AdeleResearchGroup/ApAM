package fr.imag.adele.obrMan;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Filter;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.Apform;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.MessageReference;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.obrMan.internal.OBRManager;
import fr.imag.adele.obrMan.internal.OBRManager.Selected;

public class OBRMan implements DependencyManager {

	private static OBRManager obr;

	// iPOJO injected
	private RepositoryAdmin   repoAdmin;

	//    // Default repository
	//    private String[] repostiories;
	/**
	 * OBRMAN activated, register with APAM
	 */

	// when in Felix.
	public void start() {
		System.out.println("OBRMAN started");
		obr = new OBRManager(null, repoAdmin);
		ApamManagers.addDependencyManager(this, 3);
	}

	public void stop() {
		ApamManagers.removeDependencyManager(this);
	}

	/**
	 * Given the res OBR resource, supposed to contain the implementation implName.
	 * Install and start from the OBR repository.
	 * 
	 * @param res : OBR resource (to contain the implementation implName)
	 * @param implName : the symbolic name of the implementation to deploy.
	 * @return
	 */
	private Implementation installInstantiateImpl(Resource res, String implName) {
		
		Implementation asmImpl = CST.ImplBroker.getImpl(implName);
		// Check if already deployed
		if (asmImpl == null) {
			// deploy selected resource
			boolean deployed = OBRMan.obr.deployInstall(res);
			if (!deployed) {
				System.err.print("could not install resource ");
				OBRMan.obr.printRes(res);
				return null;
			}
			// waiting for the implementation to be ready in Apam.
			asmImpl = Apform.getWaitImplementation(implName);
		} else { // do not install twice.
			// It is a logical deployement. The allready existing impl is not visible !
			//            System.out.println("Logical deployment of : " + implName + " found by OBRMAN but allready deployed.");
			//            asmImpl = CST.ASMImplBroker.addImpl(implComposite, asmImpl, null);
		}

		return asmImpl;
	}

	/**
	 * Given the res OBR resource, supposed to contain the implementation implName.
	 * Install and start from the OBR repository.
	 * 
	 * @param res : OBR resource (to contain the implementation implName)
	 * @param specName : the symbolic name of the implementation to deploy.
	 * @return
	 */
	private Specification installInstantiateSpec(Resource res, String specName) {

		Specification spec = CST.SpecBroker.getSpec(specName);
		// Check if already deployed
		if (spec == null) {
			// deploy selected resource
			boolean deployed = OBRMan.obr.deployInstall(res);
			if (!deployed) {
				System.err.print("could not install resource ");
				OBRMan.obr.printRes(res);
				return null;
			}
			// waiting for the implementation to be ready in Apam.
			spec = Apform.getWaitSpecification(specName);
		} else { // do not install twice.
			// It is a logical deployement. The allready existing impl is not visible !
			// System.out.println("Logical deployment of : " + implName + " found by OBRMAN but allready deployed.");
			// asmImpl = CST.ASMImplBroker.addImpl(implComposite, asmImpl, null);
		}

		return spec;
	}

	@Override
	public String getName() {
		return CST.OBRMAN;
	}

	// at the end
	@Override
	public void getSelectionPathSpec(CompositeType compTypeFrom, String specName, List<DependencyManager> involved) {
		involved.add(involved.size(), this);
	}

	@Override
	public void getSelectionPathImpl(CompositeType compTypeFrom, String implName, List<DependencyManager> selPath) {
		selPath.add(selPath.size(), this);
	}

	@Override
	public void getSelectionPathInst(Composite compoFrom, Implementation impl,
			Set<Filter> constraints, List<Filter> preferences, List<DependencyManager> selPath) {
		return;
	}

	@Override
	public Instance resolveImpl(Composite composite, Implementation impl, Set<Filter> constraints,
			List<Filter> preferences) {
		return null;
	}

	@Override
	public Set<Instance> resolveImpls(Composite composite, Implementation impl, Set<Filter> constraints) {
		return null;
	}

	@Override
	public int getPriority() {
		return 3;
	}

	@Override
	public void newComposite(ManagerModel model, CompositeType composite) {
		if (model == null)
			return;
		String obrModel;
		try {
			obrModel = OBRMan.readFileAsString(model.getURL());
		} catch (IOException e1) {
			System.err.println("invalid OBRMAN Model. Cannot be read :" + model.getURL());
			return;
		}

		//wait for OBR initialization 
		//        
		//        while(obr==null){
		//        }
		//        System.out.println("OBR >>> " +obr );

		OBRMan.obr.newModel(obrModel, composite.getName());
	}

	private static String readFileAsString(URL url) throws java.io.IOException {
		InputStream is = url.openStream();
		byte[] buffer = new byte[is.available()];
		BufferedInputStream f = null;
		try {
			f = new BufferedInputStream(is);
			f.read(buffer);
		} finally {
			if (f != null)
				try {
					f.close();
				} catch (IOException ignored) {
				}
		}
		return new String(buffer);
	}

	// interface manager
	private Implementation resolveSpec(CompositeType compoType, ResolvableReference resource,
			Set<Filter> constraints, List<Filter> preferences) {

		// temporary ??
		if (preferences == null)
			preferences = new ArrayList<Filter>();
		Filter f = ApamFilter.newInstance("(apam-composite=true)");
		preferences.add(f);
		// end

		f= ApamFilter.newInstance("(" +  CST.COMPONENT_TYPE + "=" + CST.IMPLEMENTATION + ")" );
		constraints.add(f) ;
		
		fr.imag.adele.obrMan.internal.OBRManager.Selected selected = null;
		Implementation impl = null;
		if (resource instanceof SpecificationReference) {
			selected = obr.lookFor(CST.CAPABILITY_COMPONENT, "(provide-specification="
					+ resource.as(SpecificationReference.class).getName() + ")",
					constraints, preferences);
		}
		if (resource instanceof InterfaceReference) {
			selected = obr.lookFor(CST.CAPABILITY_COMPONENT, "(provide-interfaces=*;" 
					+ resource.as(InterfaceReference.class).getJavaType() + ";*)",
					constraints, preferences);
		}
		if (resource instanceof MessageReference) {
			selected = obr.lookFor(CST.CAPABILITY_COMPONENT, "(provide-messages=*;" 
					+ resource.as(MessageReference.class).getJavaType() + ";*)",
					constraints, preferences);
		}
		if (selected != null) {
			String implName = obr.getAttributeInCapability(selected.capability, "impl-name");
			impl = installInstantiateImpl(selected.resource, implName);
			// System.out.println("deployed :" + impl);
			// printRes(selected);
			return impl;
		}
		return null;
	}

	@Override
	public Implementation resolveSpecByResource(CompositeType compoType, ResolvableReference resource,
			Set<Filter> constraints, List<Filter> preferences) {
		return resolveSpec(compoType, resource, constraints, preferences);
	}

	@Override
	public Implementation findImplByName(CompositeType compoType, String implName) {
		if (implName == null) {
			new Exception("parameter implName canot be null in findImplByName ").printStackTrace() ;
		}

		Selected selected = obr.lookFor(CST.CAPABILITY_COMPONENT, "(name=" + implName + ")", null, null);

		if (selected == null) return null ;
		if (! obr.getAttributeInCapability(selected.capability, CST.COMPONENT_TYPE).equals(CST.IMPLEMENTATION)) {
			System.err.println("ERROR : " + implName + " is found but is not an Implementation" );
			return null ;
		}
		return installInstantiateImpl(selected.resource, implName);
	}

	@Override
	public Specification findSpecByName(CompositeType compoType, String specName) {

		if (specName == null) return null;
		
		Selected selected = OBRMan.obr.lookFor(CST.CAPABILITY_COMPONENT, "(name=" + specName + ")", null, null);

		if (selected == null) return null ;

		if (! obr.getAttributeInCapability(selected.capability, CST.COMPONENT_TYPE).equals(CST.SPECIFICATION)) {
			System.err.println("ERROR : " + specName + " is found but is not a specification" );
			return null ;
		}

		Specification spec = installInstantiateSpec(selected.resource, specName);
		return spec;

	}

	@Override
	public void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl, Instance inst,
			Set<Instance> insts) {
		// Do not care
	}

}
