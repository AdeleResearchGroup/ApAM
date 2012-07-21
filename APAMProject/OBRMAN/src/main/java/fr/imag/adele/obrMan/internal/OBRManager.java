package fr.imag.adele.obrMan.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Filter;

import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.obrMan.filewatcher.internal.FileWatcher;
//import java.net.URI;
//import org.xml.sax.Attributes;
//import org.xml.sax.SAXException;
//import org.xml.sax.helpers.DefaultHandler;
//import org.osgi.framework.InvalidSyntaxException;

public class OBRManager {

    private RepositoryAdmin repoAdmin;
    private Resolver        resolver;
    private Repository      local;
    private Resource[]      allResources;
	private File user_home_file;
	private FileWatcher fileWatcherTask;
    private boolean repositoryModified=false;
	private Timer timer;
    /**
     * OBRMAN activated, register with APAM
     */


    public OBRManager(String defaultLocalRepo, RepositoryAdmin repoAdmin) {

        this.repoAdmin = repoAdmin;
        try {
            if (defaultLocalRepo == null) {
                // use Maven settings to find maven repository
                File settings = searchSettingsFromUserHome();
                if (settings == null) {
                    settings = searchSettingsFromM2Home();
                }
                System.out.println("used maven settings: " + settings);
                if (settings != null) {
                    defaultLocalRepo = searchMavenRepoFromSettings(settings);
                }
                if (defaultLocalRepo == null && user_home_file!=null){
                	File repositoryFile = new File(new File(new File(user_home_file, ".m2"), "repository"),"repository.xml");
                	if (repositoryFile.exists()){
                		defaultLocalRepo = "file:///" + repositoryFile.toString();	
                	}
                	System.out.println("Last chance :" + defaultLocalRepo);
                }
                
            }

            //System.out.println("Started OBRMAN " + defaultLocalRepo + "  repoAdmin: " + repoAdmin);
            if (defaultLocalRepo != null) {
                local = repoAdmin.addRepository(defaultLocalRepo);
            } else {
            	
            }
            if (local==null){
                local = repoAdmin.getLocalRepository();
            }

            //System.err.println("Local repo init = " + repoAdmin.getLocalRepository().getName() + " All repos = "
            // + repoAdmin.listRepositories().toString());
        } catch (Exception e) {
            System.err.println("Invalid repository address : " + defaultLocalRepo);
            e.printStackTrace();
        }
        System.out.println("local repo : " + local.getURI());
        
     
        resolver = repoAdmin.resolver();
        allResources = local.getResources(); // read once for each session, and cached.
        //        for (Resource res : allResources) {
        //            printRes(res);
        //        }
       
        
       
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
    // Filter f = ApamFilter.newInstance("(&(scope=LOCAL)(shared=TRUE))");
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
    // Filter f = ApamFilter.newInstance("(test=yes)");
    // constraints.add(f);
    // } catch (InvalidSyntaxException e) {
    // System.out.println("invalid filter (&(scope=LOCAL)(shared=TRUE))");
    // }
    //
    // selected = lookFor("apam-component", "(interfaces=*fr.imag.adele.apam.apamAPI.ApamComponent*)", constraints);
    // selected = lookFor("apam-component", "(interfaces=*fr.imag.adele.apam.test.s2.S2*)", constraints);
    // try {
    // Filter f = ApamFilter.newInstance("(X=Y)");
    // constraints.add(f);
    // } catch (InvalidSyntaxException e) {
    // System.out.println("invalid filter (&(scope=LOCAL)(shared=TRUE))");
    // }
    //
    // selected = lookFor("apam-interface", "(name=fr.imag.adele.apam.apamAPI.ApamComponent)", constraints);

    public void printCap(Capability aCap) {
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

    public String printProperties(Property[] props) {
        StringBuffer ret = new StringBuffer();
        for (Property prop : props) {
            ret.append(prop.getName() + "=" + prop.getValue() + ",  ");
        }
        return ret.toString();
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
        Set<Selected> allRes = new HashSet<Selected>();
        System.out.print("looking for all resources : " + capability + "; filter : " + filterStr);
        if ((constraints != null) && !constraints.isEmpty()) {
            System.out.print(". Constraints : ");
            for (Filter constraint : constraints) {
                System.out.print(constraint + ", ");
            }
        }
        System.out.println("");

        if (allResources == null)
            return null;
        try {
            ApamFilter filter = null;
            if (filterStr != null)
                filter = ApamFilter.newInstance(filterStr);
            for (Resource res : allResources) {
                Capability[] capabilities = res.getCapabilities();
                for (Capability aCap : capabilities) {
                    if (aCap.getName().equals(capability)) {
                        if ((filter == null) || filter.matchCase(aCap.getPropertiesAsMap())) {
                            if ((constraints == null) || matchConstraints(aCap, constraints)) {
                                System.out.println("   Found bundle : " + res.getSymbolicName() + " Component:  "
                                        + getAttributeInCapability(aCap, "impl-name"));
                                allRes.add(new Selected(res, aCap));
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
        //        Capability selectedCapability = null;
        int maxMatch = -1;
        int match = 0;
        for (Selected sel : candidates) {
            //            Capability[] capabilities = res.getCapabilities();
            //            for (Capability aCap : capabilities) {
            // if (sel.capabilityaCap.getName().equals(capability)) {
            match = matchPreferences(sel.capability, preferences);
            if (match > maxMatch) {
                maxMatch = match;
                winner = sel;
                // selectedCapability = aCap;
            }
        }
        //            }
        //        }
        if (winner == null)
            return null;
        System.out.println("   Found bundle : " + winner.resource.getSymbolicName() + " Component:  "
                + getAttributeInCapability(winner.capability, "impl-name"));
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
        System.out.print("looking for capability : " + capability + "; filter : " + filterStr);
        if (constraints != null) {
            System.out.print("maching constraints : ");
            for (Filter constraint : constraints) {
                System.out.print(constraint + ", ");
            }
        }
        System.out.println("");

        // Requirement req = repoAdmin.getHelper().requirement(capability, filterStr);
        if (allResources == null)
            return null;
        try {
            ApamFilter filter = null;
            if (filterStr != null)
                filter = ApamFilter.newInstance(filterStr);
            for (Resource res : allResources) {
                Capability[] capabilities = res.getCapabilities();
                for (Capability aCap : capabilities) {
                    if (aCap.getName().equals(capability)) {
                        if ((filter == null) || filter.matchCase(aCap.getPropertiesAsMap())) {
                            if ((constraints == null) || matchConstraints(aCap, constraints)) {
                                System.out.println("   Found bundle : " + res.getSymbolicName() + " Component:  "
                                        + getAttributeInCapability(aCap, "impl-name"));
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
        //        if (aCap.getName().equals("apam-implementation")) {
        Map map = aCap.getPropertiesAsMap();
        for (Filter constraint : constraints) {
            filter = ApamFilter.newInstance(constraint.toString());
            if (!filter.matchCase(map)) {
                return false;
            }
        }
        //        }
        return true;
    }

    /**
     * Deploys, installs and instantiate
     * 
     * @param res
     * @return
     */
    public boolean deployInstall(Resource res) {
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

    //    // Interface IOBRMAN
    //    @Override
    //    public Set<Selected> getResources(String capability, String filterStr, Set<Filter> constraints) {
    //        //        Set<Resource> allRes = new HashSet<Resource>();
    //        return lookForAll(capability, filterStr, constraints);
    //    }
    //
    //    @Override
    //    public Resource getResource(String capability, String filterStr, Set<Filter> constraints, List<Filter> preferences) {
    //        return lookFor(capability, filterStr, constraints, preferences).resource;
    //    }
    //
    //    @Override
    //    public boolean install(Resource resource) {
    //        deployInstall(resource);
    //        return false;
    //    }
    //
    //    private Selected getResourceImpl(String implName, Set<Filter> constraints) {
    //        Selected selected = null;
    //        String filterStr = null;
    //        if (implName != null)
    //            filterStr = "(impl-name=" + implName + ")";
    //
    //        if (selected == null) { // look by bundle name. First apam component by bundle name
    //            selected = lookFor("apam-implementation", filterStr, constraints, null);
    //        }
    //        if (selected == null) { // legacy iPOJO component
    //            selected = lookFor("component", filterStr, constraints, null);
    //        }
    //        if (selected == null) { // legacy OSGi component
    //            selected = lookFor("bundle", filterStr, constraints, null);
    //        }
    //        return selected;
    //    }

    public class Selected {
        public Resource   resource;
        public Capability capability;

        public Selected(Resource res, Capability cap) {
            resource = res;
            capability = cap;
        }
    }

    //

    private String searchMavenRepoFromSettings(File pathSettings) {
        // Look for <localRepository>
        try {

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            SaxHandler handler = new SaxHandler() ;

            saxParser.parse(pathSettings, handler);
            //            String settings = OBRManager.readFileAsString(pathSettings.toURL());
            //            int local = settings.indexOf("<localRepository>");
            //            if (local == -1)
            //                return null;
            //            // TODO eliminer les commentaires
            //            String localRepo;
            //            int localEnd = settings.indexOf("</localRepository>", local + 5);
            //            localRepo = "file:///" + settings.substring(local + 17, localEnd) + "\\repository.xml";
            //            // System.out.println("found local repos : " + localRepo);

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
        String user_home = System.getProperty("user.home");
        if (user_home == null) {
            user_home = System.getProperty("HOME");
            if (user_home == null) {
                return null;
            }
        }
        user_home_file = new File(user_home);
        File settings = new File(new File(user_home_file, ".m2"),
        "settings.xml");
        if (settings.exists()) {
            return settings;
        }
        return null;
    }
    
    public void stopWatchingRepository(){
    	if (fileWatcherTask!=null){
    		fileWatcherTask.cancel();
    	}
    	if (timer!=null){
    		timer.purge();
    		timer.cancel();
    		timer=null;
    	}
    }

	public void startWatchingRepository() {
		if (local == null) return;
		 // monitor a modification on repository file
     	fileWatcherTask = new FileWatcher( new File(URI.create(local.getURI()))) {
			  protected void onChange( File file , String date ) {
			    // Repository has been modified
			    System.out.println( "Repository "+ file +" have been changed the " + date );
			    setRepositoryModified(true);
			  }
			};
	    timer = new Timer();
		// repeat the check every 5 second
		timer.schedule( fileWatcherTask , new Date(), 5000 );
	}

	public boolean isRepositoryModified() {
		return repositoryModified;
	}

	private void setRepositoryModified(boolean repositoryModified) {
		this.repositoryModified = repositoryModified;
	}

}
