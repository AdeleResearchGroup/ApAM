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
import org.apache.felix.utils.filter.FilterImpl;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Manager;
import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.apamImpl.ManagerModel;
import fr.imag.adele.apam.apform.Apform;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.MessageReference;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.OBR;

public class OBRMan implements Manager {

    private static OBRManager obr;
    // iPOJO injected
    private RepositoryAdmin   repoAdmin;

    /**
     * OBRMAN activated, register with APAM
     */

    // when in Felix.
    public void start() {
        System.out.println("OBRMAN started");
        ApamManagers.addManager(this, 3);
        OBRMan.obr = new OBRManager(null, repoAdmin);
    }

    public void stop() {
        ApamManagers.removeManager(this);
    }

    /**
     * Given the res OBR resource, supposed to match an Apam requirement when resolving a wire from "from".
     * Install and start from the OBR repository, and creates the associated ASM impl and inst.
     * 
     * @param res : OBR resource (supposed to match an Apam requirement)
     * @param from : the origin of the wire toward the resource.
     * @return
     */
    private Implementation installInstantiate(Resource res, String implName) {

        //        String specName = getAttributeInResource(res, "apam-implementation", "apam-specification");
        Implementation asmImpl = null;

        asmImpl = CST.ImplBroker.getImpl(implName);
        // samImpl = CST.SAMImplBroker.getImplementation(implName);
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

        // Activate implementation in APAM
        // asmImpl = CST.ASMImplBroker.addImpl(implComposite, implName, null);
        return asmImpl;
    }

    @Override
    public String getName() {
        return CST.OBRMAN;
    }

    // at the end
    @Override
    public void getSelectionPathSpec(CompositeType compTypeFrom, ResolvableReference resource,
            Set<Filter> constraints, List<Filter> preferences, List<Manager> involved) {
        involved.add(involved.size(), this);
    }

    @Override
    public void getSelectionPathImpl(CompositeType compTypeFrom, String implName, List<Manager> selPath) {
        selPath.add(selPath.size(), this);
    }

    @Override
    public void getSelectionPathInst(Composite compoFrom, Implementation impl,
            Set<Filter> constraints, List<Filter> preferences, List<Manager> selPath) {
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
    private Implementation resolveSpec(CompositeType compoType, ResolvableReference resource, // String interfaceName,
            // String specName,
            Set<Filter> constraints, List<Filter> preferences) {

        // temporary
        if (preferences == null)
            preferences = new ArrayList<Filter>();
        try {
            Filter f = FilterImpl.newInstance("(apam-composite=true)");
            preferences.add(f);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
        // end

        fr.imag.adele.obrMan.OBRManager.Selected selected = null;
        Implementation impl = null;
        if (resource instanceof SpecificationReference) {
            selected = OBRMan.obr.lookFor(OBR.CAPABILITY_IMPLEMENTATION, "(provide-specification="
                    + resource.as(SpecificationReference.class).getName() + ")",
                    constraints, preferences);
        }
        if (resource instanceof InterfaceReference) {
            selected = OBRMan.obr.lookFor(OBR.CAPABILITY_IMPLEMENTATION, "(provide-interfaces=*;" + resource.as(InterfaceReference.class).getJavaType() + ";*)",
                    constraints, preferences);
        }
        if (resource instanceof MessageReference) {
            selected = OBRMan.obr.lookFor(OBR.CAPABILITY_IMPLEMENTATION, "(provide-messages=*;" + resource.as(MessageReference.class).getJavaType() + ";*)",
                    constraints, preferences);
        }
        if (selected != null) {
            String implName = OBRMan.obr.getAttributeInCapability(selected.capability, "name");
            impl = installInstantiate(selected.resource, implName);
            // System.out.println("deployed :" + impl);
            // printRes(selected);
            return impl;
        }
        return null;
    }

    //    @Override
    //    public Implementation resolveSpecByName(CompositeType compoType, String specName,
    //            Set<Filter> constraints, List<Filter> preferences) {
    //        return resolveSpec(compoType, null, specName, constraints, preferences);
    //    }

    @Override
    public Implementation resolveSpecByResource(CompositeType compoType, ResolvableReference resource,
            Set<Filter> constraints, List<Filter> preferences) {
        return resolveSpec(compoType, resource, constraints, preferences);
    }

    @Override
    public Implementation findImplByName(CompositeType compoType, String implName) {
        // private Selected getResourceImpl(String implName, Set<Filter> constraints) {
        fr.imag.adele.obrMan.OBRManager.Selected selected = null;
        Implementation impl = null;
        String filterStr = null;
        if (implName != null)
            filterStr = "(name=" + implName + ")";

        if (selected == null) { // look by bundle name. First apam component by bundle name
            selected = OBRMan.obr.lookFor(OBR.CAPABILITY_IMPLEMENTATION, filterStr, null, null);
        }
        if (selected == null) { // legacy iPOJO component
            selected = OBRMan.obr.lookFor(OBR.CAPABILITY_COMPONENT, filterStr, null, null);
        }
        if (selected == null) { // legacy OSGi component
            selected = OBRMan.obr.lookFor("bundle", filterStr, null, null);
        }
        if (selected != null) {
            impl = installInstantiate(selected.resource, implName);
            // System.out.println("deployed :" + impl);
            // printRes(selected);
            return impl;
        }
        return null;
    }

    @Override
    public void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl, Instance inst,
            Set<Instance> insts) {
        // Do not care
    }
}
