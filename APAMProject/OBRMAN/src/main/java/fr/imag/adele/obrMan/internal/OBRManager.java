package fr.imag.adele.obrMan.internal;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Filter;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.util.ApamFilter;

public class OBRManager {

    private final Resolver     resolver;

    private final Repository[] repositories;

    private final Resource[]   allResources;

    private final String       compositeTypeName;

    public OBRManager(String name, RepositoryAdmin repoAdmin) {
        this(name, repoAdmin, null);
    }

    public OBRManager(String name, RepositoryAdmin repoAdmin, String obrModel) {
        if (obrModel != null) {
            newModel(obrModel, repoAdmin);
        }
        compositeTypeName = name;
        List<Resource> resourcesTemp = new ArrayList<Resource>();
        repositories = repoAdmin.listRepositories();
        resolver = repoAdmin.resolver();
        for (Repository repository : repositories) {
            resourcesTemp.addAll(Arrays.asList(repository.getResources()));
            repoAdmin.removeRepository(repository.getURI());
        }
        allResources = resourcesTemp.toArray(new Resource[0]);
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

    public Set<Selected> lookForAll(String capability, String filterStr, Set<Filter> constraints) {
        if (filterStr == null)
            new Exception("no filter in lookfor all").printStackTrace();

        Set<Selected> allRes = new HashSet<Selected>();
        System.out.print("OBR: looking for all components matching " + filterStr);
        if ((constraints != null) && !constraints.isEmpty()) {
            System.out.print(". Constraints : ");
            for (Filter constraint : constraints) {
                System.out.print(constraint + ", ");
            }
        }
        System.out.println("");

        if (allResources == null) {
            System.err.println("no resources in OBR");
            return null;
        }
        try {
            ApamFilter filter = ApamFilter.newInstance(filterStr);
            for (Resource res : allResources) {
                Capability[] capabilities = res.getCapabilities();
                for (Capability aCap : capabilities) {
                    if (aCap.getName().equals(capability)) {
                        if (filter.matchCase(aCap.getPropertiesAsMap())) {
                            if ((constraints == null) || matchConstraints(aCap, constraints)) {
                                System.out.println("   Component " + getAttributeInCapability(aCap, CST.A_NAME)
                                        + " found in bundle : " + res.getSymbolicName());
                                allRes.add(new Selected(this, res, aCap));
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

    public Selected lookForPref(String capability, List<Filter> preferences, Set<Selected> candidates) {
        if (candidates.isEmpty())
            return null;

        // trace
        System.out.print("preferences : ");
        for (Filter constraint : preferences) {
            System.out.print(constraint + ", ");
        }
        System.out.println("");
        // fin trace

        Selected winner = null;
        int maxMatch = -1;
        int match = 0;
        for (Selected sel : candidates) {
            match = matchPreferences(sel.capability, preferences);
            if (match > maxMatch) {
                maxMatch = match;
                winner = sel;
            }
        }

        if (winner == null)
            return null;
        System.out.println("   Found bundle : " + winner.resource.getSymbolicName() + " Component:  "
                + getAttributeInCapability(winner.capability, CST.A_IMPLNAME));
        return winner;
    }

    private int matchPreferences(Capability aCap, List<Filter> preferences) {
        ApamFilter filter;
        Map<?, ?> map = aCap.getPropertiesAsMap();
        int match = 0;
        for (Filter constraint : preferences) {
            filter = ApamFilter.newInstance(constraint.toString());
            if (!filter.matchCase(map)) {
                // System.out.println("contraint not matched : " + constraint);
                return match;
            }
            match++;
        }
        return match;
    }

    /**
     * 
     * @param capability: an OBR capability
     * @param filterStr: a single constraint like "(impl-name=xyz)" Should not be null
     * @param constraints: the other constraints. can be null
     * @param preferences: the preferences. can be null
     * @return the pair capability,
     */
    public Selected lookFor(String capability, String filterStr, Set<Filter> constraints, List<Filter> preferences) {
        if ((preferences != null) && !preferences.isEmpty()) {
            return lookForPref(capability, preferences, lookForAll(capability, filterStr, constraints));
        }
        return lookFor(capability, filterStr, constraints);
    }

    public Selected lookFor(String capability, String filterStr, Set<Filter> constraints) {
        if (filterStr == null) {
            System.err.println("No filter for lookFor");
            return null;
        }
        System.out.print("OBR: looking for component " + filterStr);
        if (constraints != null) {
            System.out.print(". Constraints to match: ");
            for (Filter constraint : constraints) {
                System.out.print(constraint + ", ");
            }
        }
        System.out.println("");

        if (allResources == null) {
            System.err.println("no resources in OBR");
            return null;
        }
        try {
            ApamFilter filter = ApamFilter.newInstance(filterStr);
            for (Resource res : allResources) {
                Capability[] capabilities = res.getCapabilities();
                for (Capability aCap : capabilities) {
                    if (aCap.getName().equals(capability)) { // apam-component
                        if (filter.matchCase(aCap.getPropertiesAsMap())) {
                            if ((constraints == null) || matchConstraints(aCap, constraints)) {
                                System.out.println("   Component " + getAttributeInCapability(aCap, CST.A_NAME)
                                        + " found in bundle " + res.getSymbolicName());
                                return new Selected(this, res, aCap);
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

    /**
     * return true if the provided capability has an implementation that satisfies the constraints
     * 
     * @param aCap
     * @param constraints
     * @return
     */
    private boolean matchConstraints(Capability aCap, Set<Filter> constraints) {
        if ((constraints == null) || constraints.isEmpty() || (aCap == null))
            return true;

        ApamFilter filter;
        Map<?, ?> map = aCap.getPropertiesAsMap();
        for (Filter constraint : constraints) {
            filter = ApamFilter.newInstance(constraint.toString());
            if (!filter.matchCase(map)) {
                return false;
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
                System.out.println("OBR changed state. Resolving again " + selected.resource.getSymbolicName());
            }
        }

        Reason[] reqs = resolver.getUnsatisfiedRequirements();
        for (Reason req : reqs) {
            System.out.println("Unable to resolve: " + req);
        }
        return false;
    }

    public void newModel(String obrModel, RepositoryAdmin repoAdmin) {
        StringTokenizer st = new StringTokenizer(obrModel);
        String repoUrlStr = null;
        while (st.hasMoreElements()) {
            try {
                repoUrlStr = st.nextToken("\n");
                System.out.println("new repository :" + repoUrlStr);
                // URI uri = URI.create(repoUrlStr);
                URL url = new URL(repoUrlStr);
                repoAdmin.addRepository(url);
            } catch (Exception e) {
                System.err.println("Invalid OBR repository address :"
                        + repoUrlStr);
                return;
            }
        }
    }

    public String getCompositeTypeName() {
        return compositeTypeName;
    }

    public class Selected {
        public Resource   resource;
        public Capability capability;
        public OBRManager obrManager;

        public Selected(OBRManager obrMan, Resource res, Capability cap) {
            obrManager = obrMan;
            resource = res;
            capability = cap;
        }

    }

    //

}
