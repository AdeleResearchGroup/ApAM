package fr.imag.adele.obrMan.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.util.ApamFilter;

public class OBRManager {

    Logger                                   logger = LoggerFactory.getLogger(OBRManager.class);

    /**
     * Associate compositeType name and it repositories
     */
    private Map<String, CompositeRepostiory> compositeRepositories;

    private File                             user_home_file;

    private RepositoryAdmin                  repoAdmin;

    /**
     * OBRMAN activated, register with APAM
     */

    public OBRManager(URL defaultLocalRepo, RepositoryAdmin repoAdmin) {
        this.repoAdmin = repoAdmin;
        compositeRepositories = new HashMap<String, CompositeRepostiory>();
        initCommonRepositories(true, repoAdmin);
    }

    /**
     * Save the list of default repositories.
     * 
     * @param searchLocalRepo
     *            if true Search for the local repository from M2_HOME and
     *            USER_HOME
     */
    private void initCommonRepositories(boolean searchLocalRepo,
            RepositoryAdmin repoAdmin) {
        try {

            if (searchLocalRepo) {
                // try to find the maven settings.xml file
                File settings = searchSettingsFromM2Home();
                if (settings == null) {
                    settings = searchSettingsFromUserHome();
                }
                logger.info("Maven settings location: " + settings);

                // Extract localRepository from settings.xml
                URL defaultLocalRepo = null;
                if (settings != null) {
                    defaultLocalRepo = searchMavenRepoFromSettings(settings);
                }

                if (defaultLocalRepo == null) {
                    // Special case for Jenkins Server :
                    defaultLocalRepo = searchRepositoryFromJenkinsServer();
                } else {
                    // Add the founded repository to RepoAdmin
                    repoAdmin.addRepository(defaultLocalRepo);
                }
            }

            // save the common repositories and clean repoAdmin
            CompositeRepostiory rootComposite = new CompositeRepostiory(
                    repoAdmin);
            compositeRepositories.put(CST.ROOT_COMPOSITE_TYPE, rootComposite);

        } catch (Exception e) {
            logger.error(e.getMessage(), e.getCause());
        }

    }

    public void printCap(Capability aCap) {
        System.out.println("   Capability name: " + aCap.getName());
        for (Property prop : aCap.getProperties()) {
            System.out.println("     " + prop.getName() + " type= "
                    + prop.getType() + " val= " + prop.getValue());
        }
    }

    public void printRes(Resource aResource) {
        System.out.println("\n\nRessource SymbolicName : "
                + aResource.getSymbolicName());
        for (Capability aCap : aResource.getCapabilities()) {
            printCap(aCap);
        }
    }

    public String printProperties(Property[] props) {
        StringBuffer ret = new StringBuffer();
        for (Property prop : props) {
            ret.append(prop.getName() + "=" + prop.getValue() + ",  ");
        }
        return ret.toString();
    }

    // serious stuff now !
    public String getAttributeInResource(Resource res, String capability,
            String attr) {
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

//    public Set<Selected> lookForAll(String capability, String filterStr,
//            Set<Filter> constraints) {
//        if (filterStr == null)
//            new Exception("no filter in lookfor all").printStackTrace();
//
//        Set<Selected> allRes = new HashSet<Selected>();
//        System.out.print("OBR: looking for all components matching "
//                + filterStr);
//        if ((constraints != null) && !constraints.isEmpty()) {
//            System.out.print(". Constraints : ");
//            for (Filter constraint : constraints) {
//                System.out.print(constraint + ", ");
//            }
//        }
//        System.out.println("");
//
//        if (allResources == null) {
//            System.err.println("no resources in OBR");
//            return null;
//        }
//        try {
//            ApamFilter filter = ApamFilter.newInstance(filterStr);
//            for (Resource res : allResources) {
//                Capability[] capabilities = res.getCapabilities();
//                for (Capability aCap : capabilities) {
//                    if (aCap.getName().equals(capability)) {
//                        if (filter.matchCase(aCap.getPropertiesAsMap())) {
//                            if ((constraints == null)
//                                    || matchConstraints(aCap, constraints)) {
//                                System.out.println("   Component "
//                                        + getAttributeInCapability(aCap,
//                                                OBR.A_NAME)
//                                        + " found in bundle : "
//                                        + res.getSymbolicName());
//                                allRes.add(new Selected(res, aCap));
//                            }
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        if (allRes.isEmpty())
//            System.out.println("   Not Found");
//        return allRes;
//    }

    public Selected lookForPref(String capability, List<Filter> preferences,
            Set<Selected> candidates) {
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
        System.out.println("   Found bundle : "
                + winner.resource.getSymbolicName() + " Component:  "
                + getAttributeInCapability(winner.capability, CST.A_IMPLNAME));
        return winner;
    }

    private int matchPreferences(Capability aCap, List<Filter> preferences) {
        ApamFilter filter;
        Map map = aCap.getPropertiesAsMap();
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
     * @param capability : an OBR capability
     * @param filterStr : a single constraint like "(impl-name=xyz)" Should not be
     *            null
     * @param constraints
     *            : the other constraints. can be null
     * @param preferences
     *            : the preferences. can be null
     * @return the pair capability,
     */
//    public Selected lookFor(String capability, String filterStr, Set<Filter> constraints, List<Filter> preferences) {
//        if ((preferences != null) && !preferences.isEmpty()) {
//            return lookForPref(capability, preferences, lookForAll(capability, filterStr, constraints));
//        }
//        return lookFor(capability, filterStr, constraints);
//    }

    public Selected lookFor(CompositeType compoType, String capability, String filterStr, Set<Filter> constraints,
            Object object) {
        if (filterStr == null) {
            System.err.println("No filter for lookFor");
            return null ;
        }
        System.out.print("OBR: looking for component " + filterStr);
        if (constraints != null) {
            System.out.print(". Constraints to match: ");
            for (Filter constraint : constraints) {
                System.out.print(constraint + ", ");
            }
        }
        System.out.println("");

       CompositeRepostiory compositeRepostiory = compositeRepositories
                .get(compoType.getName());

        if (compositeRepostiory == null){
            compositeRepostiory = compositeRepositories
                    .get(CST.ROOT_COMPOSITE_TYPE);
        }
        else return null;
        try {
            ApamFilter filter = null;
            if (filterStr != null)
                filter = ApamFilter.newInstance(filterStr);
            for (Resource res : compositeRepostiory.getResources()) {
                Capability[] capabilities = res.getCapabilities();
                for (Capability aCap : capabilities) {
                    if (aCap.getName().equals(capability)) { // apam-component
                        if (filter.matchCase(aCap.getPropertiesAsMap())) {
                            if ((constraints == null) || matchConstraints(aCap, constraints)) {
                                System.out.println("   Component " + getAttributeInCapability(aCap, CST.A_NAME) + " found in bundle " + res.getSymbolicName() );
                                return new Selected(compositeRepostiory,res, aCap);
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
     * return true if the provided capability has an implementation that
     * satisfies the constraints
     * 
     * @param aCap
     * @param constraints
     * @return
     */
    private boolean matchConstraints(Capability aCap, Set<Filter> constraints) {
        if ((constraints == null) || constraints.isEmpty() || (aCap == null))
            return true;

        ApamFilter filter;
        // if (aCap.getName().equals("apam-implementation")) {
        Map map = aCap.getPropertiesAsMap();
        for (Filter constraint : constraints) {
            filter = ApamFilter.newInstance(constraint.toString());
            if (!filter.matchCase(map)) {
                return false;
            }
        }
        // }
        return true;
    }

    /**
     * Deploys, installs and instantiate
     * 
     * @param compoType
     * 
     * @param res
     * @return
     */
    public boolean deployInstall(Selected selected) {
        // first check if res is not under deployment by another thread.
        // and remove when the deployment is done.

        boolean deployed = false;
        // the events sent by iPOJO for the previous deployed bundle may
        // interfere and
        // change the state of the local repository, which produces the
        // IllegalStateException.
        Resolver resolver = selected.compositeRepostiory.getResolver();
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
                System.out.println("OBR changed state. Resolving again "
                        + selected.resource.getSymbolicName());
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
                // URI uri = URI.create(repoUrlStr);
                URL url = new URL(repoUrlStr);
                repoAdmin.addRepository(url);
            } catch (Exception e) {
                System.err.println("Invalid OBR repository address :"
                        + repoUrlStr);
                return;
            }

        }
        CompositeRepostiory compositeRepository = new CompositeRepostiory(
                repoAdmin);
        compositeRepositories.put(composite, compositeRepository);
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

    public class Selected {
        public Resource            resource;
        public Capability          capability;
        public CompositeRepostiory compositeRepostiory;

        public Selected(CompositeRepostiory compoRepo, Resource res,
                Capability cap) {
            resource = res;
            capability = cap;
            compositeRepostiory = compoRepo;
        }
    }

    //

    private URL searchMavenRepoFromSettings(File pathSettings) {
        // Look for <localRepository>
        try {

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            SaxHandler handler = new SaxHandler();

            saxParser.parse(pathSettings, handler);

            return handler.getRepo();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private File searchSettingsFromM2Home() {
        String m2_home = System.getenv().get("M2_HOME");

        if (m2_home == null) {
            return null;
        }
        File m2_Home_file = new File(m2_home);
        File settings = new File(new File(m2_Home_file, "conf"), "settings.xml");
        if (settings.exists()) {
            return settings;
        }
        return null;
    }

    private File searchSettingsFromUserHome() {
        File m2Folder = getM2Folder();
        if (m2Folder == null)
            return null;
        File settings = new File(m2Folder, "settings.xml");
        if (settings.exists()) {
            return settings;
        }
        return null;
    }

    private URL searchRepositoryFromJenkinsServer() {
        try {
            File m2Folder = getM2Folder();
            if (m2Folder == null)
                return null;
            File repositoryFile = new File(new File(m2Folder, "repository"),
                    "repository.xml");
            if (repositoryFile.exists()) {
                URL repo = repositoryFile.toURI().toURL();
                logger.info("Jenkins server repository :" + repo);
                return repo;
            }
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    private File getM2Folder() {
        String user_home = System.getProperty("user.home");
        if (user_home == null) {
            user_home = System.getProperty("HOME");
            if (user_home == null) {
                return null;
            }
        }
        user_home_file = new File(user_home);
        File m2Folder = new File(user_home_file, ".m2");
        return m2Folder;
    }

}
