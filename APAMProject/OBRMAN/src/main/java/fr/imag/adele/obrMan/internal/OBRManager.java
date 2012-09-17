package fr.imag.adele.obrMan.internal;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.util.ApamFilter;

public class OBRManager {

    private final Resolver       resolver;

    private List<Repository>     repositories;

    private final List<Resource> allResources;

    private final String         compositeTypeName;

    private final OBRMan         obrMan;

    private final Logger         logger = LoggerFactory.getLogger(OBRManager.class);

    public OBRManager(OBRMan obrman, String compositeTypeName, RepositoryAdmin repoAdmin, LinkedProperties obrModel) {
        allResources = new ArrayList<Resource>();
        repositories = new ArrayList<Repository>();
        this.compositeTypeName = compositeTypeName;
        obrMan = obrman;
        // First Read model if it exist
        if (obrModel != null) {
            repositories = getRepositoriesFromModel(obrModel, repoAdmin);
        }

        // Get resources from repositories and remove them from repoAdmin.
        for (Repository repository : repositories) {
            allResources.addAll(Arrays.asList(repository.getResources()));
            repoAdmin.removeRepository(repository.getURI());
        }

        // Add the system as repository
        repositories.add(0, repoAdmin.getLocalRepository());
        repositories.add(0, repoAdmin.getSystemRepository());
        resolver = repoAdmin.resolver(repositories.toArray(new Repository[repositories.size()]));

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

        if (allResources.isEmpty()) {
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
                                System.out.println("   Component " + getAttributeInCapability(aCap, CST.NAME)
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
            System.out.println("   Not Found in > " + repositoriesToString());
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
                + getAttributeInCapability(winner.capability, CST.IMPLNAME));
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

        if (allResources.isEmpty()) {
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
                                System.out.println("   Component " + getAttributeInCapability(aCap, CST.NAME)
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
        System.out.println("   Not Found in : " + repositoriesToString());
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

    protected List<Repository> getRepositoriesFromModel(LinkedProperties obrModel, RepositoryAdmin repoAdmin) {
        List<Repository> declaredRepositories = new ArrayList<Repository>();
        Enumeration<?> keys = obrModel.keys();
        while (keys.hasMoreElements()) {

            String key = (String) keys.nextElement();
            if (Util.LOCAL_MAVEN_REPOSITORY.equals(key)) {
                // Add the obr repository located in the local maven repository
                Boolean localMavenOBRRepo = new Boolean(obrModel.getProperty(key));
                if (localMavenOBRRepo) {
                    URL localMavenObrUrl = findLocalMavenRepository();
                    try {
                        declaredRepositories.add(repoAdmin.addRepository(localMavenObrUrl));
                    } catch (Exception e) {
                        logger.error("Error when adding default local repository to repoAdmin", e.getCause());
                    }
                }
            } else if (Util.DEFAULT_OSGI_REPOSITORIES.equals(key)) {
                // Add obr repositories declared in the osgi configuration file
                Boolean osgiRepo = new Boolean(obrModel.getProperty(key));
                if (osgiRepo) {
                    String repos = obrMan.getDeclaredOSGiOBR();
                    if (repos != null) {
                        declaredRepositories.addAll(getRepositoriesFromArray(repoAdmin, repos.split("\\s+")));
                    }
                }
            } else if (Util.REPOSITORIES.equals(key)) {
                // Add obr repositories declared in the composite
                declaredRepositories
                        .addAll(getRepositoriesFromArray(repoAdmin, obrModel.getProperty(key).split("\\s+")));

            } else if (Util.COMPOSITES.equals(key)) {
                // look for obr repositories in other composites
                String[] otherCompositesRepositories = obrModel.getProperty(key).split("\\s+");
                for (String compoTypeName : otherCompositesRepositories) {
                    OBRManager manager = obrMan.getOBRManager(compoTypeName);
                    if (manager != null) {
                        declaredRepositories.addAll(manager.getRepositories());
                    } else {
                        // If the compositeType is not present, do nothing
                        logger.debug("The composite " + compositeTypeName + " reference a missing compiste "
                                + compoTypeName);
                    }
                }
            }
        }

        return declaredRepositories;
    }

    protected Collection<Repository> getRepositoriesFromArray(RepositoryAdmin repoAdmin, String[] repos) {
        List<Repository> repoList = new ArrayList<Repository>();
        for (String repoUrlStr : repos) {
            try {
                URL url = new URL(repoUrlStr);
                repoList.add(repoAdmin.addRepository(url));
            } catch (Exception e) {
                logger.error("Invalid OBR repository address :" + repoUrlStr, e.getCause());
            }
        }
        return repoList;
    }

    public String getCompositeTypeName() {
        return compositeTypeName;
    }

    public class Selected {
        public Resource   resource;
        public Capability capability;
        public OBRManager obrManager;

        public Selected(OBRManager obrManager, Resource res, Capability cap) {
            this.obrManager = obrManager;
            resource = res;
            capability = cap;
        }

    }

    protected URL findLocalMavenRepository() {

        // try to find the maven settings.xml file
        File settings = Util.searchSettingsFromM2Home();
        if (settings == null) {
            settings = Util.searchSettingsFromUserHome();
        }
        logger.info("Maven settings location: " + settings);

        // Extract localRepository from settings.xml
        URL defaultLocalRepo = null;
        if (settings != null) {
            defaultLocalRepo = Util.searchMavenRepoFromSettings(settings);
        }

        if (defaultLocalRepo == null) {
            // Special case for Jenkins Server :
            defaultLocalRepo = Util.searchRepositoryFromJenkinsServer();
        }
        if (defaultLocalRepo != null) {
            return defaultLocalRepo;
        }
        return null;
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    //
    protected List<String> repositoriesToString() {
        List<String> repoString = new ArrayList<String>();
        for (Repository repo : repositories) {
            repoString.add(repo.getURI());
        }
        return repoString;
    }
}
