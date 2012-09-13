package fr.imag.adele.obrMan.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.MessageReference;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.obrMan.OBRManCommand;
import fr.imag.adele.obrMan.internal.OBRManager.Selected;

@Instantiate(name = "OBRMAN-Instance")
@Component(name = "OBRMAN")
@Provides(specifications = OBRManCommand.class)
public class OBRMan implements DependencyManager, OBRManCommand {

    // Link compositeType with it instance of obrManager
    private final Map<String, OBRManager> obrManagers;

    // iPOJO injected
    @Requires(proxy = false)
    private RepositoryAdmin               repoAdmin;

    private final Logger                  logger = LoggerFactory.getLogger(OBRMan.class);

    private final BundleContext           m_context;

    /**
     * OBRMAN activated, register with APAM
     */

    public OBRMan(BundleContext context) {
        m_context = context;
        obrManagers = new HashMap<String, OBRManager>();
    }

    @Validate
    public void start() {
        System.out.println(">>> OBRMAN starting");
        LinkedProperties obrModel = new LinkedProperties();
        // TODO lookFor root.OBRMAN.cfg and create obrmanager for the root composite
        // create obrmanager for the root composite

        try {
            obrModel.load(new FileInputStream(new File("conf/root.OBRMAN.cfg")));
        } catch (IOException e) {
            logger.error("Invalid OBRMAN Model. Cannot be read stream " + "conf/root.OBRMAN.cfg", e.getCause());
            obrModel.put(Util.LOCAL_MAVEN_REPOSITORY, "true");
            obrModel.put(Util.DEFAULT_OSGI_REPOSITORIES, "true");
        }
        OBRManager obrManager = new OBRManager(this, CST.ROOT_COMPOSITE_TYPE, repoAdmin, obrModel);
        obrManagers.put(CST.ROOT_COMPOSITE_TYPE, obrManager);

        ApamManagers.addDependencyManager(this, 3);
    }

    @Invalidate
    public void stop() {
        System.out.println(">>> OBRMAN stoping");
        ApamManagers.removeDependencyManager(this);
        obrManagers.clear();
    }

    /**
     * Given the res OBR resource, supposed to contain the implementation implName.
     * Install and start from the OBR repository.
     * 
     * @param res : OBR resource (to contain the implementation implName)
     * @param implName : the symbolic name of the implementation to deploy.
     * @return
     */
    private Implementation installInstantiateImpl(Selected selected, String implName) {

        Implementation asmImpl = CST.ImplBroker.getImpl(implName);
        // Check if already deployed
        if (asmImpl == null) {
            // deploy selected resource
            boolean deployed = selected.obrManager.deployInstall(selected);
            if (!deployed) {
                System.err.print("could not install resource ");
                Util.printRes(selected.resource);
                return null;
            }
            // waiting for the implementation to be ready in Apam.
            asmImpl = CST.ImplBroker.getImpl(implName, true);
        } else { // do not install twice.
            // It is a logical deployement. The allready existing impl is not visible !
            // System.out.println("Logical deployment of : " + implName + " found by OBRMAN but allready deployed.");
            // asmImpl = CST.ASMImplBroker.addImpl(implComposite, asmImpl, null);
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
    private Specification installInstantiateSpec(Selected selected, String specName) {

        Specification spec = CST.SpecBroker.getSpec(specName);
        // Check if already deployed
        if (spec == null) {
            // deploy selected resource
            boolean deployed = selected.obrManager.deployInstall(selected);
            if (!deployed) {
                System.err.print("could not install resource ");
                Util.printRes(selected.resource);
                return null;
            }
            // waiting for the implementation to be ready in Apam.
            spec = CST.SpecBroker.getSpec(specName, true);
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
    public void newComposite(ManagerModel model, CompositeType compositeType) {
        LinkedProperties obrModel = new LinkedProperties();
        if (model == null)
            return;
        try {
            obrModel.load(model.getURL().openStream());
        } catch (IOException e) {
            logger.error("Invalid OBRMAN Model. Cannot be read stream " + model.getURL(), e.getCause());
            obrModel.put(Util.LOCAL_MAVEN_REPOSITORY, "true");
            obrModel.put(Util.DEFAULT_OSGI_REPOSITORIES, "true");
        }
        OBRManager obrManager = new OBRManager(this, compositeType.getName(), repoAdmin, obrModel);
        obrManagers.put(compositeType.getName(), obrManager);
    }

    // interface manager
    private Implementation resolveSpec(CompositeType compoType, ResolvableReference resource,
            Set<Filter> constraints, List<Filter> preferences) {

        // Find the composite OBRManager
        OBRManager obrManager = searchOBRManager(compoType);
        if (obrManager == null)
            return null;

        // temporary ??
        if (preferences == null)
            preferences = new ArrayList<Filter>();
        Filter f = ApamFilter.newInstance("(apam-composite=true)");
        preferences.add(f);
        // end

        f = ApamFilter.newInstance("(" + CST.COMPONENT_TYPE + "=" + CST.IMPLEMENTATION + ")");
        constraints.add(f);

        fr.imag.adele.obrMan.internal.OBRManager.Selected selected = null;
        Implementation impl = null;
        if (resource instanceof SpecificationReference) {
            selected = obrManager.lookFor(CST.CAPABILITY_COMPONENT, "(provide-specification="
                    + resource.as(SpecificationReference.class).getName() + ")",
                    constraints, preferences);
        }
        if (resource instanceof InterfaceReference) {
            selected = obrManager.lookFor(CST.CAPABILITY_COMPONENT, "(provide-interfaces=*;"
                    + resource.as(InterfaceReference.class).getJavaType() + ";*)",
                    constraints, preferences);
        }
        if (resource instanceof MessageReference) {
            selected = obrManager.lookFor(CST.CAPABILITY_COMPONENT, "(provide-messages=*;"
                    + resource.as(MessageReference.class).getJavaType() + ";*)",
                    constraints, preferences);
        }
        if (selected != null) {
            String implName = obrManager.getAttributeInCapability(selected.capability, "impl-name");
            impl = installInstantiateImpl(selected, implName);
            // System.out.println("deployed :" + impl);
            // printRes(selected);
            return impl;
        }
        return null;
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
        }

        return obrManager;
    }

    @Override
    public Implementation resolveSpecByResource(CompositeType compoType, ResolvableReference resource,
            Set<Filter> constraints, List<Filter> preferences) {
        return resolveSpec(compoType, resource, constraints, preferences);
    }

    @Override
    public Implementation findImplByName(CompositeType compoType, String implName) {
        if (implName == null) {
            new Exception("parameter implName canot be null in findImplByName ").printStackTrace();
        }

        // Find the composite OBRManager
        OBRManager obrManager = searchOBRManager(compoType);
        if (obrManager == null)
            return null;

        Selected selected = obrManager.lookFor(CST.CAPABILITY_COMPONENT, "(name=" + implName + ")", null, null);

        if (selected == null)
            return null;
        if (!obrManager.getAttributeInCapability(selected.capability, CST.COMPONENT_TYPE).equals(CST.IMPLEMENTATION)) {
            System.err.println("ERROR : " + implName + " is found but is not an Implementation");
            return null;
        }
        return installInstantiateImpl(selected, implName);
    }

    @Override
    public Specification findSpecByName(CompositeType compoType, String specName) {

        if (specName == null)
            return null;

        // Find the composite OBRManager
        OBRManager obrManager = searchOBRManager(compoType);
        if (obrManager == null)
            return null;

        Selected selected = obrManager.lookFor(CST.CAPABILITY_COMPONENT, "(name=" + specName + ")", null, null);

        if (selected == null)
            return null;

        if (!obrManager.getAttributeInCapability(selected.capability, CST.COMPONENT_TYPE).equals(CST.SPECIFICATION)) {
            System.err.println("ERROR : " + specName + " is found but is not a specification");
            return null;
        }

        Specification spec = installInstantiateSpec(selected, specName);
        return spec;

    }

    public OBRManager getOBRManager(String compositeTypeName) {
        return obrManagers.get(compositeTypeName);
    }

    @Override
    public void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl,
            Instance inst,
            Set<Instance> insts) {
        // Do not care
    }

    public String getDeclaredOSGiOBR() {
        return m_context.getProperty(Util.OSGI_OBR_REPOSITORY_URL);
    }

    @Override
    public String printCompositeRepositories(String compositeTypeName) {
        String result = "";
        OBRManager obrmanager = getOBRManager(compositeTypeName);
        if (obrmanager == null)
            return result;

        result += (compositeTypeName + " (" + obrmanager.getRepositories().size() + ") : \n");
        for (Repository repository : obrmanager.getRepositories()) {
            result += ("    >> " + repository.getURI() + "\n");
        }

        return result;
    }
}
