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
import fr.imag.adele.apam.CompExInstImpl;
import fr.imag.adele.apam.CompExTypeImpl;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.ASMImpl.ASMImplImpl;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.CompExType;
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
            local = repoAdmin.addRepository("file:///F:/Maven/.m2/repository.xml");
        } catch (Exception e) {
            // TODO Auto-generated catch block
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
                                System.out.println("   Found : " + res.getSymbolicName());
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

    private Resource lookForPref(String capability, List<Filter> preferences, Set<Resource> candidates) {
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
                    }
                }
            }
        }
        System.out.println("   Found : " + winner.getSymbolicName());
        return winner;
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

    private Resource lookFor(String capability, String filterStr, Set<Filter> constraints, List<Filter> preferences) {
        if ((preferences != null) && !preferences.isEmpty()) {
            return lookForPref(capability, preferences, lookForAll(capability, filterStr, constraints));
        }
        return lookFor(capability, filterStr, constraints);
    }

    private Resource lookFor(String capability, String filterStr, Set<Filter> constraints) {
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
                                return res;
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
        resolver.add(res);
        if (resolver.resolve()) {
            resolver.deploy(Resolver.START);
            return true;
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
    private ASMInst installInstantiate(Resource res, Composite implComposite, Composite instComposite,
            boolean multiple, Set<ASMInst> allInst) {
        String implName = getAttributeInResource(res, "apam-component", "apam-implementation");
        String specName = getAttributeInResource(res, "apam-component", "apam-specification");

        String implNameExpected = res.getSymbolicName(); //the sam name
        if (implName == null)
            implName = implNameExpected;

        Implementation samImpl = null;
        Instance samInst = null;
        ASMImpl asmImpl = null;
        ASMInst asmInst = null;
        try {
            asmImpl = CST.ASMImplBroker.getImpl(implName);
            if (asmImpl != null) { // do not install twice. Bizarre, APMAN should have found it !
                System.err.println("ERROR : " + implName + " allready deployed.");
                return asmImpl.createInst(instComposite, null);
            }

            CST.implEventHandler.addExpected(implNameExpected);
            boolean deployed = deployInstall(res);
            if (!deployed) {
                System.err.print("could not install resource ");
                printRes(res);
                return null;
            }
            //waiting for the implementation to be ready in SAM.
            samImpl = CST.implEventHandler.getImplementation(implNameExpected);
            // instances may have been created by deploy or iPOJO
            //We have to wait for these instances to appear !! How long ? how to know they are all ready
            Thread.sleep(10);

            if ((samImpl.getProperty(CST.PROPERTY_COMPOSITE) != null) &&
                    ((Boolean) samImpl.getProperty(CST.PROPERTY_COMPOSITE) == true)) {
                //get the interfaces
                CompExType compType = CompExTypeImpl.createCompExType(implComposite, samImpl);
                if (compType == null)
                    return null;

                CompExInstImpl compInst = (CompExInstImpl) ((ASMImplImpl) compType).createInst(instComposite, null);
                if (compInst == null)
                    return null;
                if (multiple) {
                    allInst = new HashSet<ASMInst>();
                    allInst.add(compInst.getMainInst());
                }
                return compInst.getMainInst();
            }

            Set<Instance> allInstances = null;
            if (multiple) {
                allInstances = samImpl.getInstances();
                if (allInstances == null)
                    allInstances = new HashSet<Instance>();
                if (allInstances.isEmpty())
                    allInstances.add(samImpl.createInstance(null)); // should not be null
            } else {
                samInst = samImpl.getInstance();
                if (samInst == null) // An instance must be created 
                    samInst = samImpl.createInstance(null); // should not be null
            }
            if ((samInst == null) && ((allInstances == null) || allInstances.isEmpty())) {
                System.err.println("sam instance is null. in OBRMAN install instanciate");
                new Exception().printStackTrace();
            }

            asmImpl = CST.ASMImplBroker.addImpl(implComposite, implName, samImpl.getName(), specName, null);
            if (multiple) {
                if (allInst == null)
                    allInst = new HashSet<ASMInst>();
                for (Instance inst : allInstances) {
                    allInst.add(CST.ASMInstBroker.addInst(implComposite, instComposite, inst, implNameExpected,
                            specName, null));
                }
            } else
                asmInst = CST.ASMInstBroker.addInst(implComposite, instComposite, samInst, implNameExpected, specName,
                        null);
            // in case it is the main implementation

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        //null if multiple
        return asmInst;
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
    public List<Manager> getSelectionPathSpec(ASMInst from, Composite composite, String interfaceName, String specName,
            Set<Filter> constraints, List<Manager> involved) {
        involved.add(involved.size(), this);
        return involved;
    }

    @Override
    public List<Manager> getSelectionPathImpl(ASMInst from, Composite composite, String samImplName, String implName,
            Set<Filter> constraints, List<Manager> involved) {
        involved.add(involved.size(), this);
        return involved;
    }

    @Override
    public ASMInst resolveSpec(Composite implComposite, Composite instComposite, String interfaceName, String specName,
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
        //end

        Resource selected = null;
        ASMInst newInst = null;
        if (specName != null) {
            selected = lookFor("apam-component", "(apam-specification=" + specName + ")", constraints, preferences);
        }
        if ((selected == null) && (interfaceName != null)) {
            selected = lookFor("apam-interface", "(name=" + interfaceName + ")", constraints, preferences);
        }
        if (selected != null) {
            newInst = installInstantiate(selected, implComposite, instComposite, false, null);
            System.out.println("deployed :" + newInst.getASMName());
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
    public Set<ASMInst> resolveSpecs(Composite implComposite, Composite instComposite, String interfaceName,
            String specName, Set<Filter> constraints, List<Filter> preferences) {

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
        Resource selected = null;
        if (specName != null) {
            selected = lookFor("apam-component", "(apam-specification=" + specName + ")", constraints, preferences);
        }
        if ((selected == null) && (interfaceName != null)) {
            selected = lookFor("apam-interface", "(name=" + interfaceName + ")", constraints, preferences);
        }
        if (selected != null) {
            installInstantiate(selected, implComposite, instComposite, true, allInsts);

            System.out.print("deployed instances :");
            for (ASMInst inst : allInsts) {
                System.out.print(" " + inst.getASMName());
            }
            System.out.println("\n");
            //printRes(selected);
            return allInsts;
        }
        return null;
    }

    private Resource getResourceImpl(String samImplName, String implName, Set<Filter> constraints,
            List<Filter> preferences) {
        Resource selected = null;

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

        if (implName != null) {
            selected = lookFor(CST.CAPABILITY_COMPONENT, "(apam-implementation=" + implName + ")", constraints,
                    preferences);
        }
        if (selected == null) { //look by bundle name. First apam component by bundle name
            if (samImplName == null)
                samImplName = implName;
            selected = lookFor(CST.CAPABILITY_COMPONENT, "(name=" + samImplName + ")", constraints, preferences);
        }
        if (selected == null) { //legacy iPOJO component
            selected = lookFor("component", "(name=" + samImplName + ")", constraints, preferences);
        }
        if (selected == null) { //legacy OSGi component
            selected = lookFor("bundle", "(symbolicname=" + samImplName + ")", constraints, preferences);
        }
        return selected;
    }

    @Override
    public ASMInst resolveImpl(Composite implComposite, Composite instComposite, String samImplName, String implName,
            Set<Filter> constraints, List<Filter> preferences) {

        ASMInst newInst = null;
        Resource selected = getResourceImpl(samImplName, implName, constraints, preferences);
        if (selected != null) {
            newInst = installInstantiate(selected, implComposite, instComposite, false, null);
            System.out.println("deployed :" + newInst.getASMName());
            //printRes(selected);
            return newInst;
        }
        return null;
    }

    @Override
    public Set<ASMInst> resolveImpls(Composite implComposite, Composite instComposite, String samImplName,
            String implName, Set<Filter> constraints, List<Filter> preferences) {
        Set<ASMInst> allInsts = new HashSet<ASMInst>();
        Set<ASMInst> rets = new HashSet<ASMInst>();
        Resource selected = getResourceImpl(samImplName, implName, constraints, preferences);
        if (selected != null) {
            installInstantiate(selected, implComposite, instComposite, true, allInsts);

            System.out.print("deployed instances :");
            for (ASMInst inst : allInsts) {
                System.out.print(" " + inst.getASMName());
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
    public void newComposite(ManagerModel model, Composite composite) {
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
        return lookFor(capability, filterStr, constraints, preferences);
    }

    @Override
    public boolean install(Resource resource) {
        deployInstall(resource);
        return false;
    }

    @Override
    public ASMImpl resolveImplByName(Composite implComposite, Composite instComposite, String samImplName,
            String implName, Set<Filter> constraints, List<Filter> preferences) {
        //TODO
        ASMInst inst = resolveImpl(implComposite, instComposite, samImplName, implName, constraints, preferences);
        return inst.getImpl();
    }

    @Override
    public ASMImpl resolveSpecByName(Composite implComposite, Composite instComposite, String interfaceName,
            String specName, Set<Filter> constraints, List<Filter> preferences) {
        // TODO 
        ASMInst inst = resolveSpec(implComposite, instComposite, interfaceName, specName, constraints, preferences);
        return inst.getImpl();
    }

}
