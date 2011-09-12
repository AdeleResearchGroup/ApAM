package fr.imag.adele.obrMan;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.utils.filter.FilterImpl;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.CompositeType;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.apam.apamAPI.ManagersMng;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Instance;

public class OBRMan implements Manager, IOBRMAN {

    // iPOJO injected
    private RepositoryAdmin repoAdmin;
    private ManagersMng     apam;

    private Resolver        resolver;
    private Repository      local;
    private Resource[]      allResources;

    /**
     * OBRMAN activated, register with APAM
     */
    public void start() {
        System.out.println("Started OBRMAN");
        apam.addManager(this, 3);
        // TODO should take its default model : its repositories.

        // to test only
        try {
            //local = repoAdmin.addRepository("file:///C:/Program%20Files/Apache%20Software%20Foundation/apache-maven-2.2.1/repository/repository.xml");
            local = repoAdmin.addRepository("file:///F:/Maven/.m2/repository.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("local repo : " + local.getURI());
        resolver = repoAdmin.resolver();
        allResources = local.getResources(); // read once for each session, and cached.
        // for (Resource res : allResources) {
        // printRes(res);
        // }
        //        Resource selected;
        //        selected = lookFor("bundle", "(symbolicname=ApamCommand)", null);
        //        selected = lookFor("apam-component", "(name=S2Impl)", null);
        //        selected = lookFor("apam-component", "(apam-implementation=S2ImplApamName)", null);
        //        selected = lookFor("apam-component", "(apam-specification=S2)", null);
        //        selected = lookFor("apam-component", "(scope=LOCAL)", null);
        //        selected = lookFor("apam-component", "(interfaces=*fr.imag.adele.apam.apamAPI.ApamComponent*)", null);
        //        selected = lookFor("apam-component", "(interfaces=*fr.imag.adele.apam.test.s2.S2*)", null);
        //        selected = lookFor("apam-interface", "(name=fr.imag.adele.apam.apamAPI.ApamComponent)", null);
        //        selected = lookFor("apam-interface", "(name=fr.imag.adele.apam.test.s2.S2)", null);
        //        selected = lookFor("apam-component", "(&(interfaces=*fr.imag.adele.apam.apamAPI.ApamComponent*)(scope=LOCAL))",
        //                null);
        //
        //        Set<Filter> constraints = new HashSet<Filter>();
        //
        //        selected = lookFor("bundle", "(symbolicname=ApamCommand)", constraints);
        //
        //        try {
        //            Filter f = FilterImpl.newInstance("(&(scope=LOCAL)(shared=TRUE))");
        //            constraints.add(f);
        //        } catch (InvalidSyntaxException e) {
        //            System.out.println("invalid filter (&(scope=LOCAL)(shared=TRUE))");
        //        }
        //
        //        selected = lookFor("apam-component", "(name=S2Impl)", constraints);
        //        selected = lookFor("apam-component", "(apam-implementation=S2ImplApamName)", constraints);
        //        selected = lookFor("apam-component", "(apam-specification=S2)", null);
        //
        //        try {
        //            Filter f = FilterImpl.newInstance("(test=yes)");
        //            constraints.add(f);
        //        } catch (InvalidSyntaxException e) {
        //            System.out.println("invalid filter (&(scope=LOCAL)(shared=TRUE))");
        //        }
        //
        //        selected = lookFor("apam-component", "(interfaces=*fr.imag.adele.apam.apamAPI.ApamComponent*)", constraints);
        //        selected = lookFor("apam-component", "(interfaces=*fr.imag.adele.apam.test.s2.S2*)", constraints);
        //        try {
        //            Filter f = FilterImpl.newInstance("(X=Y)");
        //            constraints.add(f);
        //        } catch (InvalidSyntaxException e) {
        //            System.out.println("invalid filter (&(scope=LOCAL)(shared=TRUE))");
        //        }
        //
        //        selected = lookFor("apam-interface", "(name=fr.imag.adele.apam.apamAPI.ApamComponent)", constraints);

    }

    public void stop() {
        apam.removeManager(this);
    }

    private void printCap(Capability aCap) {
        System.out.println("   Capability name: " + aCap.getName());
        for (Property prop : aCap.getProperties()) {
            System.out.println("     " + prop.getName() + " type= " + prop.getType() + " val= " + prop.getValue());
        }
    }

    private void printRes(Resource aResource) {
        System.out.println("\n\nRessource SymbolicName : " + aResource.getSymbolicName());
        for (Capability aCap : aResource.getCapabilities()) {
            printCap(aCap);
        }
    }

    private String printProperties(Property[] props) {
        StringBuffer ret = new StringBuffer();
        for (Property prop : props) {
            ret.append(prop.getName() + "=" + prop.getValue() + ",  ");
        }
        return ret.toString();
    }

    // serious stuff now !
    private String getAttributeInResource(Resource res, String capability, String attr) {
        for (Capability aCap : res.getCapabilities()) {
            if (aCap.getName().equals(capability)) {
                return (String) (aCap.getPropertiesAsMap().get(attr));
            }
        }
        return null;
    }

    private String getAttributeInCapability(Capability aCap, String attr) {
        return (String) (aCap.getPropertiesAsMap().get(attr));
    }

    private Set<Resource>
            lookForAll(String capability, String filterStr, Set<Filter> constraints) {
        Set<Resource> allRes = new HashSet<Resource>();
        System.out.println("looking for all resources : " + capability + "; filter : " + filterStr);
        if (allResources == null)
            return null;
        try {
            FilterImpl filter = null;
            if (filterStr != null)
                filter = FilterImpl.newInstance(filterStr);
            for (Resource res : allResources) {
                Capability[] capabilities = res.getCapabilities();
                for (Capability aCap : capabilities) {
                    if (aCap.getName().equals(capability)) {
                        if ((filter == null) || filter.matchCase(aCap.getPropertiesAsMap())) {
                            if ((constraints == null) || matchConstraints(capabilities, constraints)) {
                                System.out.println("   Found : " + getAttributeInCapability(aCap, "name"));
                                allRes.add(res);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (allRes.isEmpty())
            System.out.println("   Not Found");
        return allRes;
    }

    private Selected lookForPref(String capability, List<Filter> preferences, Set<Resource> candidates) {
        if (candidates.isEmpty())
            return null;

        //trace
        System.out.print("preferences : ");
        for (Filter constraint : preferences) {
            System.out.print(constraint + ", ");
        }
        System.out.println("");
        //fin trace

        Resource winner = null;
        Capability selectedCapability = null;
        int maxMatch = -1;
        int match = 0;
        for (Resource res : candidates) {
            Capability[] capabilities = res.getCapabilities();
            for (Capability aCap : capabilities) {
                if (aCap.getName().equals(capability)) {
                    match = matchPreferences(capabilities, preferences);
                    if (match > maxMatch) {
                        maxMatch = match;
                        winner = res;
                        selectedCapability = aCap;
                    }
                }
            }
        }
        System.out.println("   Found : " + getAttributeInCapability(selectedCapability, "name"));
        if (winner == null)
            return null;
        return new Selected(winner, selectedCapability);
    }

    private int matchPreferences(Capability[] capabilities, List<Filter> preferences) {
        FilterImpl filter;
        for (Capability aCap : capabilities) {
            if (aCap.getName().equals("apam-component")) {
                Map map = aCap.getPropertiesAsMap();
                int match = 0;
                for (Filter constraint : preferences) {
                    try {
                        filter = FilterImpl.newInstance(constraint.toString());
                        if (!filter.matchCase(map)) {
                            System.out.println("contraint not matched : " + constraint);
                            return match;
                        }
                        match++;
                    } catch (InvalidSyntaxException e) {
                        System.err.println("invalid syntax in filter : " + constraint.toString());
                    }
                }
                return match;
            }
        }
        return 0;
    }

    private Selected lookFor(String capability, String filterStr, Set<Filter> constraints, List<Filter> preferences) {
        if ((preferences != null) && !preferences.isEmpty()) {
            return lookForPref(capability, preferences, lookForAll(capability, filterStr, constraints));
        }
        return lookFor(capability, filterStr, constraints);
    }

    private Selected lookFor(String capability, String filterStr, Set<Filter> constraints) {
        System.out.println("looking for capability : " + capability + "; filter : " + filterStr);
        //Requirement req = repoAdmin.getHelper().requirement(capability, filterStr);
        if (allResources == null)
            return null;
        try {
            FilterImpl filter = null;
            if (filterStr != null)
                filter = FilterImpl.newInstance(filterStr);
            for (Resource res : allResources) {
                Capability[] capabilities = res.getCapabilities();
                for (Capability aCap : capabilities) {
                    if (aCap.getName().equals(capability)) {
                        if ((filter == null) || filter.matchCase(aCap.getPropertiesAsMap())) {
                            if ((constraints == null) || matchConstraints(capabilities, constraints)) {
                                System.out.println("   Found : " + res.getSymbolicName());
                                return new Selected(res, aCap);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("   Not Found");
        return null;
    }

    private boolean matchConstraints(Capability[] capabilities, Set<Filter> constraints) {
        if ((constraints == null) || constraints.isEmpty())
            return true;
        if (capabilities == null)
            return true;
        FilterImpl filter;

        //trace
        System.out.print("maching constraints : ");
        for (Filter constraint : constraints) {
            System.out.print(constraint + ", ");
        }
        System.out.println("");

        for (Capability aCap : capabilities) {
            if (aCap.getName().equals("apam-component")) {
                Map map = aCap.getPropertiesAsMap();
                for (Filter constraint : constraints) {
                    try {
                        filter = FilterImpl.newInstance(constraint.toString());
                        if (!filter.matchCase(map)) {
                            System.out.println("contraint not matched : " + constraint);
                            return false;
                        }
                    } catch (InvalidSyntaxException e) {
                        System.err.println("invalid syntax in filter : " + constraint.toString());
                    }
                }
            }
        }
        return true;
    }

    /**
     * Deploys, installs and instantiate
     * 
     * @param res
     * @return
     */
    public boolean deployInstall(Resource res) {
        boolean deployed = false;
        //the events sent by iPOJO for the previous deployed bundle may interfere and 
        //change the state of the local repository, which produces the IllegalStateException. 
        while (!deployed) {
            try {
                resolver = repoAdmin.resolver();
                resolver.add(res);
                if (resolver.resolve()) {
                    resolver.deploy(Resolver.START);
                    return true;
                }
                deployed = true;
            } catch (IllegalStateException e) {
                System.out.println("OBR changed state. Resolving again " + res.getSymbolicName());
                //Thread.sleep(10) ;
            }
        }

        Reason[] reqs = resolver.getUnsatisfiedRequirements();
        for (Reason req : reqs) {
            System.out.println("Unable to resolve: " + req);
        }
        return false;
    }

    /**
     * Given the res OBR resource, supposed to match an Apam requirement when resolving a wire from "from".
     * Install and start from the OBR repository, and creates the associated ASM impl and inst.
     * 
     * @param res : OBR resource (supposed to match an Apam requirement)
     * @param from : the origin of the wire toward the resource.
     * @return
     */
    private ASMInst installInstantiate(Resource res, String implName, CompositeType implComposite,
            Composite instComposite, boolean multiple, Set<ASMInst> allInst) {

        String specName = getAttributeInResource(res, "apam-component", "apam-specification");
        Implementation samImpl = null;
        ASMImpl asmImpl = null;
        ASMInst asmInst = null;

        try {
            asmImpl = CST.ASMImplBroker.getImpl(implName);
            samImpl = CST.SAMImplBroker.getImplementation(implName);
            //Check if allready deployed
            if ((asmImpl == null) && (samImpl == null)) {
                // deploy selected resource
                CST.implEventHandler.addExpected(implName);
                boolean deployed = deployInstall(res);
                if (!deployed) {
                    System.err.print("could not install resource ");
                    printRes(res);
                    return null;
                }

                //waiting for the implementation to be ready in SAM.
                samImpl = CST.implEventHandler.getImplementation(implName);

                // Activate implementation in APAM
                asmImpl = CST.ASMImplBroker.addImpl(implComposite, implName, specName, null);

            } else { // do not install twice. Bizarre, APMAN or SAMMAN should have found it !
                System.err.println("ERROR : " + implName + " found by OBRMAN but allready deployed.");
                // proceed anyway
            }

            // instances may have been created by deploy or iPOJO
            //We have to wait for these instances to appear !! How long ? how to know they are all ready
            Thread.sleep(10);

            // Return already deployed instances if found
            Set<Instance> existingInstances = samImpl.getInstances();
            if ((existingInstances != null) && !existingInstances.isEmpty()) {
                if (allInst == null)
                    allInst = new HashSet<ASMInst>();
                for (Instance inst : existingInstances) {
                    asmInst = CST.ASMInstBroker.addInst(instComposite, inst, implName, specName, null);
                    allInst.add(asmInst);
                    if (!multiple)
                        return asmInst;
                }
                return null;
            }

            // If no instances were deployed then create a new instance and return it
            asmInst = asmImpl.createInst(instComposite, null);
            if (multiple) {
                if (allInst == null)
                    allInst = new HashSet<ASMInst>();
                allInst.add(asmInst);
                return null;
            } else
                return asmInst;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getName() {
        return CST.OBRMAN;
    }

    @Override
    public List<Filter> getConstraintsSpec(String interfaceName, String specName, String depName,
            List<Filter> initConstraints) {
        return initConstraints;
    }

    //at the end
    @Override
    public List<Manager> getSelectionPathSpec(ASMInst from, CompositeType composite, String interfaceName,
            String specName,
            Set<Filter> constraints, List<Filter> preferences, List<Manager> involved) {
        involved.add(involved.size(), this);
        return involved;
    }

    @Override
    public List<Manager> getSelectionPathImpl(ASMInst from, CompositeType composite, String implName,
            Set<Filter> constraints, List<Filter> preferences, List<Manager> involved) {
        involved.add(involved.size(), this);
        return involved;
    }

    @Override
    public ASMInst resolveSpec(Composite instComposite, String interfaceName, String specName,
            Set<Filter> constraints, List<Filter> preferences) {

        CompositeType implComposite = null;
        if (instComposite != null)
            implComposite = instComposite.getCompType();
        // temporary 
        if (preferences == null)
            preferences = new ArrayList<Filter>();
        try {
            Filter f = FilterImpl.newInstance("(apam-composite=true)");
            preferences.add(f);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
        //end

        Selected selected = null;
        ASMInst newInst = null;
        if (specName != null) {
            selected = lookFor(CST.CAPABILITY_COMPONENT, "(apam-specification=" + specName + ")", constraints,
                    preferences);
        }
        if ((selected == null) && (interfaceName != null)) {
            selected = lookFor(CST.CAPABILITY_COMPONENT, "(interfaces=*" + interfaceName + "*)", constraints,
                    preferences);
        }
        if (selected != null) {
            String implName = getAttributeInCapability(selected.capability, "name");
            newInst = installInstantiate(selected.resource, implName, implComposite, instComposite, false, null);
            System.out.println("deployed :" + newInst.getName());
            //printRes(selected);
            return newInst;
        }
        return null;
    }

    /**
     * return only the first one. It is not planned to return resources, and it is not reasonnable to install all
     * matching resources
     */
    @Override
    public Set<ASMInst> resolveSpecs(Composite instComposite, String interfaceName,
            String specName, Set<Filter> constraints, List<Filter> preferences) {

        CompositeType implComposite = null;
        if (instComposite != null)
            implComposite = instComposite.getCompType();

        // temporary 
        if (preferences == null)
            preferences = new ArrayList<Filter>();
        try {
            Filter f = FilterImpl.newInstance("(apam-composite=true)");
            preferences.add(f);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
        //end

        Set<ASMInst> allInsts = new HashSet<ASMInst>();
        Selected selected = null;
        String implName = null;

        if (specName != null) {
            selected = lookFor(CST.CAPABILITY_COMPONENT, "(apam-specification=" + specName + ")", constraints,
                    preferences);
        }
        if ((selected == null) && (interfaceName != null)) {
            selected = lookFor(CST.CAPABILITY_COMPONENT, "(interfaces=*" + interfaceName + "*)", constraints,
                    preferences);
        }
        if (selected != null) {
            implName = getAttributeInCapability(selected.capability, "name");
            installInstantiate(selected.resource, implName, implComposite, instComposite, true, allInsts);

            System.out.print("deployed instances :");
            for (ASMInst inst : allInsts) {
                System.out.print(" " + inst.getName());
            }
            System.out.println("\n");
            //printRes(selected);
            return allInsts;
        }
        return null;
    }

    private Selected getResourceImpl(String implName, Set<Filter> constraints, List<Filter> preferences) {
        Selected selected = null;

        // temporary 
        if (preferences == null)
            preferences = new ArrayList<Filter>();
        try {
            Filter f = FilterImpl.newInstance("(apam-composite=true)");
            preferences.add(f);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
        //end

        String filterStr = null;
        if (implName != null)
            filterStr = "(name=" + implName + ")";

        //            selected = lookFor(CST.CAPABILITY_COMPONENT, "(apam-implementation=" + implName + ")", constraints,
        //                    preferences);
        //        }
        if (selected == null) { //look by bundle name. First apam component by bundle name
            selected = lookFor(CST.CAPABILITY_COMPONENT, filterStr, constraints, preferences);
        }
        if (selected == null) { //legacy iPOJO component
            selected = lookFor("component", filterStr, constraints, preferences);
        }
        if (selected == null) { //legacy OSGi component
            selected = lookFor("bundle", filterStr, constraints, preferences);
        }
        return selected;
    }

    @Override
    public ASMInst resolveImpl(CompositeType implComposite, Composite instComposite, String implName,
            Set<Filter> constraints, List<Filter> preferences) {

        ASMInst newInst = null;
        Capability cap = null;
        Selected selected = getResourceImpl(implName, constraints, preferences);
        if (selected != null) {
            newInst = installInstantiate(selected.resource, implName, implComposite, instComposite, false, null);
            System.out.println("deployed :" + newInst.getName());
            //printRes(selected);
            return newInst;
        }
        return null;
    }

    @Override
    public Set<ASMInst> resolveImpls(CompositeType implComposite, Composite instComposite,
            String implName, Set<Filter> constraints, List<Filter> preferences) {
        Set<ASMInst> allInsts = new HashSet<ASMInst>();
        Set<ASMInst> rets = new HashSet<ASMInst>();
        Capability cap = null;
        Selected selected = getResourceImpl(implName, constraints, preferences);
        if (selected != null) {
            installInstantiate(selected.resource, implName, implComposite, instComposite, true, allInsts);
            System.out.print("deployed instances :");
            for (ASMInst inst : allInsts) {
                System.out.print(" " + inst.getName());
            }
            System.out.println("\n");

            return allInsts;
        }
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
        StringTokenizer st = new StringTokenizer(obrModel);
        String repoUrlStr = null;
        while (st.hasMoreElements()) {
            try {
                repoUrlStr = st.nextToken("\n");
                System.out.println("new repository :" + repoUrlStr);
                local = repoAdmin.addRepository(repoUrlStr);
            } catch (Exception e) {
                System.err.println("Invalid OBR repository address :" + repoUrlStr);
                return;
            }
            System.out.println("new local repo : " + local.getURI());
            resolver = repoAdmin.resolver();
            allResources = local.getResources(); // read once for each session, and cached.
            //            for (Resource res : allResources) {
            //                printRes(res);
            //            }
        }
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

    //Interface IOBRMAN
    @Override
    public Set<Resource> getResources(String capability, String filterStr, Set<Filter> constraints) {
        Set<Resource> allRes = new HashSet<Resource>();
        return lookForAll(capability, filterStr, constraints);
    }

    @Override
    public Resource getResource(String capability, String filterStr, Set<Filter> constraints, List<Filter> preferences) {
        return lookFor(capability, filterStr, constraints, preferences).resource;
    }

    @Override
    public boolean install(Resource resource) {
        deployInstall(resource);
        return false;
    }

    @Override
    public ASMImpl resolveImplByName(Composite instComposite, String implName) {
        ASMInst inst = resolveImpl(null, instComposite, implName, null, null);
        return inst.getImpl();
    }

    @Override
    public ASMImpl resolveSpecByName(Composite instComposite,
            String specName, Set<Filter> constraints, List<Filter> preferences) {
        ASMInst inst = resolveSpec(instComposite, null, specName, constraints, preferences);
        return inst.getImpl();
    }

    @Override
    public ASMImpl resolveSpecByInterface(Composite composite, String interfaceName, String[] interfaces,
            Set<Filter> constraints, List<Filter> preferences) {
        return null;
    }

    private class Selected {
        public Resource   resource;
        public Capability capability;

        public Selected(Resource res, Capability cap) {
            resource = res;
            capability = cap;
        }
    }
}
