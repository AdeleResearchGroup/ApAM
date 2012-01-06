package fr.imag.adele.obrMan;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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

public class OBRManager implements IOBRMAN {

    // iPOJO injected
    private RepositoryAdmin repoAdmin;

    private Resolver        resolver;
    private Repository      local;
    private Resource[]      allResources;

    /**
     * OBRMAN activated, register with APAM
     */

    // When in maven plug-in
    public OBRManager(String defaultLocalRepo) {
        init(defaultLocalRepo);
    }

    public void init(String defaultLocalRepo) {
        System.out.println("Started OBRMAN");
        try {
            if (defaultLocalRepo != null) {
                local = repoAdmin.addRepository(defaultLocalRepo);
            } else {
                local = repoAdmin.getLocalRepository();
            }
            System.err.println("Local repo init = " + repoAdmin.getLocalRepository().getName() + " All repos = "
                    + repoAdmin.listRepositories().toString());
        } catch (Exception e) {
            System.err.println("Invalid repository address : " + defaultLocalRepo);
            e.printStackTrace();
        }
        System.out.println("local repo : " + local.getURI());
        resolver = repoAdmin.resolver();
        allResources = local.getResources(); // read once for each session, and cached.
    }

    // Resource selected;
    // selected = lookFor("bundle", "(symbolicname=ApamCommand)", null);
    // selected = lookFor("apam-component", "(name=S2Impl)", null);
    // selected = lookFor("apam-component", "(apam-implementation=S2ImplApamName)", null);
    // selected = lookFor("apam-component", "(apam-specification=S2)", null);
    // selected = lookFor("apam-component", "(scope=LOCAL)", null);
    // selected = lookFor("apam-component", "(interfaces=*fr.imag.adele.apam.apamAPI.ApamComponent*)", null);
    // selected = lookFor("apam-component", "(interfaces=*fr.imag.adele.apam.test.s2.S2*)", null);
    // selected = lookFor("apam-interface", "(name=fr.imag.adele.apam.apamAPI.ApamComponent)", null);
    // selected = lookFor("apam-interface", "(name=fr.imag.adele.apam.test.s2.S2)", null);
    // selected = lookFor("apam-component",
    // "(&(interfaces=*fr.imag.adele.apam.apamAPI.ApamComponent*)(scope=LOCAL))",
    // null);
    //
    // Set<Filter> constraints = new HashSet<Filter>();
    //
    // selected = lookFor("bundle", "(symbolicname=ApamCommand)", constraints);
    //
    // try {
    // Filter f = FilterImpl.newInstance("(&(scope=LOCAL)(shared=TRUE))");
    // constraints.add(f);
    // } catch (InvalidSyntaxException e) {
    // System.out.println("invalid filter (&(scope=LOCAL)(shared=TRUE))");
    // }
    //
    // selected = lookFor("apam-component", "(name=S2Impl)", constraints);
    // selected = lookFor("apam-component", "(apam-implementation=S2ImplApamName)", constraints);
    // selected = lookFor("apam-component", "(apam-specification=S2)", null);
    //
    // try {
    // Filter f = FilterImpl.newInstance("(test=yes)");
    // constraints.add(f);
    // } catch (InvalidSyntaxException e) {
    // System.out.println("invalid filter (&(scope=LOCAL)(shared=TRUE))");
    // }
    //
    // selected = lookFor("apam-component", "(interfaces=*fr.imag.adele.apam.apamAPI.ApamComponent*)", constraints);
    // selected = lookFor("apam-component", "(interfaces=*fr.imag.adele.apam.test.s2.S2*)", constraints);
    // try {
    // Filter f = FilterImpl.newInstance("(X=Y)");
    // constraints.add(f);
    // } catch (InvalidSyntaxException e) {
    // System.out.println("invalid filter (&(scope=LOCAL)(shared=TRUE))");
    // }
    //
    // selected = lookFor("apam-interface", "(name=fr.imag.adele.apam.apamAPI.ApamComponent)", constraints);

    private void printCap(Capability aCap) {
        System.out.println("   Capability name: " + aCap.getName());
        for (Property prop : aCap.getProperties()) {
            System.out.println("     " + prop.getName() + " type= " + prop.getType() + " val= " + prop.getValue());
        }
    }

    public void printRes(Resource aResource) {
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

    public String getAttributeInCapability(Capability aCap, String attr) {
        return (String) (aCap.getPropertiesAsMap().get(attr));
    }

    public Set<Resource>
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
                                System.out.println("   Found bundle : " + res.getSymbolicName() + " Component:  "
                                        + getAttributeInCapability(aCap, "name"));
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

        // trace
        System.out.print("preferences : ");
        for (Filter constraint : preferences) {
            System.out.print(constraint + ", ");
        }
        System.out.println("");
        // fin trace

        Resource winner = null;
        Capability selectedCapability = null;
        int maxMatch = -1;
        int match = 0;
        for (Resource res : candidates) {
            Capability[] capabilities = res.getCapabilities();
            for (Capability aCap : capabilities) {
                if (aCap.getName().equals(capability)) {
                    match = matchPreferences(aCap, preferences);
                    if (match > maxMatch) {
                        maxMatch = match;
                        winner = res;
                        selectedCapability = aCap;
                    }
                }
            }
        }
        if (winner == null)
            return null;
        System.out.println("   Found bundle : " + winner.getSymbolicName() + " Component:  "
                + getAttributeInCapability(selectedCapability, "name"));
        return new Selected(winner, selectedCapability);
    }

    private int matchPreferences(Capability aCap, List<Filter> preferences) {
        FilterImpl filter;
        // for (Capability aCap : capabilities) {
        // if (aCap.getName().equals("apam-component")) {
        Map map = aCap.getPropertiesAsMap();
        int match = 0;
        for (Filter constraint : preferences) {
            try {
                filter = FilterImpl.newInstance(constraint.toString());
                if (!filter.matchCase(map)) {
                    // System.out.println("contraint not matched : " + constraint);
                    return match;
                }
                match++;
            } catch (InvalidSyntaxException e) {
                System.err.println("invalid syntax in filter : " + constraint.toString());
            }
        }
        return match;
    }

    public Selected lookFor(String capability, String filterStr, Set<Filter> constraints, List<Filter> preferences) {
        if ((preferences != null) && !preferences.isEmpty()) {
            return lookForPref(capability, preferences, lookForAll(capability, filterStr, constraints));
        }
        return lookFor(capability, filterStr, constraints);
    }

    public Selected lookFor(String capability, String filterStr, Set<Filter> constraints) {
        System.out.println("looking for capability : " + capability + "; filter : " + filterStr);
        // Requirement req = repoAdmin.getHelper().requirement(capability, filterStr);
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
                                System.out.println("   Found bundle : " + res.getSymbolicName() + " Component:  "
                                        + getAttributeInCapability(aCap, "name"));
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

        // trace
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
                            System.out.println("constraint not matched : " + constraint);
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
        // TODO we should put in a global and synchronized variable that we are currently deploying res.
        // first check if res is not under deployment by another thread.
        // and remove when the deployment is done.

        boolean deployed = false;
        // the events sent by iPOJO for the previous deployed bundle may interfere and
        // change the state of the local repository, which produces the IllegalStateException.
        while (!deployed) {
            try {
                resolver = repoAdmin.resolver();
                resolver.add(res);
                // printRes(res);
                if (resolver.resolve()) {
                    resolver.deploy(Resolver.START);
                    return true;
                }
                deployed = true;
            } catch (IllegalStateException e) {
                System.out.println("OBR changed state. Resolving again " + res.getSymbolicName());
                // Thread.sleep(10) ;
            }
        }

        Reason[] reqs = resolver.getUnsatisfiedRequirements();
        for (Reason req : reqs) {
            System.out.println("Unable to resolve: " + req);
        }
        return false;
    }

    public void newModel(String obrModel, String composite) {
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
            // System.out.println("new local repo : " + local.getURI());
            resolver = repoAdmin.resolver();
            allResources = local.getResources(); // read once for each session, and cached.
            // for (Resource res : allResources) {
            // printRes(res);
            // }
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

    // Interface IOBRMAN
    @Override
    public Set<Resource> getResources(String capability, String filterStr, Set<Filter> constraints) {
//        Set<Resource> allRes = new HashSet<Resource>();
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

    private Selected getResourceImpl(String implName, Set<Filter> constraints) {
        Selected selected = null;
        String filterStr = null;
        if (implName != null)
            filterStr = "(name=" + implName + ")";

        if (selected == null) { // look by bundle name. First apam component by bundle name
            selected = lookFor("apam-implementation", filterStr, constraints, null);
        }
        if (selected == null) { // legacy iPOJO component
            selected = lookFor("component", filterStr, constraints, null);
        }
        if (selected == null) { // legacy OSGi component
            selected = lookFor("bundle", filterStr, constraints, null);
        }
        return selected;
    }

    public class Selected {
        public Resource   resource;
        public Capability capability;

        public Selected(Resource res, Capability cap) {
            resource = res;
            capability = cap;
        }
    }

}
