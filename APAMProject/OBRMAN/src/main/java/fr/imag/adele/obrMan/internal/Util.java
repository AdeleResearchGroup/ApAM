package fr.imag.adele.obrMan.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Util {

    public static final String LOCAL_MAVEN_REPOSITORY    = "LocalMavenRepository";
    public static final String DEFAULT_OSGI_REPOSITORIES = "DefaultOSGiRepositories";
    public static final String REPOSITORIES              = "Repositories";
    public static final String COMPOSITES                = "Composites";
    public static final String OSGI_OBR_REPOSITORY_URL   = "obr.repository.url";

    private static Logger      logger                    = LoggerFactory.getLogger(Util.class);

    public static void printCap(Capability aCap) {
        System.out.println("   Capability name: " + aCap.getName());
        for (Property prop : aCap.getProperties()) {
            System.out.println("     " + prop.getName() + " type= " + prop.getType() + " val= " + prop.getValue());
        }
    }

    public static void printRes(Resource aResource) {
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

    public static File searchSettingsFromM2Home() {
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

    public static File searchSettingsFromUserHome() {
        File m2Folder = getM2Folder();
        if (m2Folder == null)
            return null;
        File settings = new File(m2Folder, "settings.xml");
        if (settings.exists()) {
            return settings;
        }
        return null;
    }

    public static URL searchRepositoryFromJenkinsServer() {
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

    public static File getM2Folder() {
        String user_home = System.getProperty("user.home");
        if (user_home == null) {
            user_home = System.getProperty("HOME");
            if (user_home == null) {
                return null;
            }
        }
        File user_home_file = new File(user_home);
        File m2Folder = new File(user_home_file, ".m2");
        return m2Folder;
    }

    public static URL searchMavenRepoFromSettings(File pathSettings) {
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

    public static class SaxHandler extends DefaultHandler {
        boolean localRepo = false;

        String  localRepoPath;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equalsIgnoreCase("localRepository")) {
                localRepo = true;
            }
        }

        @Override
        public void endElement(String uri, String localName,
                String qName) throws SAXException {
            if (localRepo)
                localRepo = false;
        }

        @Override
        public void characters(char ch[], int start, int length) throws SAXException {
            if (localRepo) {
                localRepoPath = new String(ch, start, length);
            }
        }

        public URL getRepo() throws MalformedURLException {
            if (localRepoPath == null)
                return null;
            File file = new File(localRepoPath + File.separator + "repository.xml");
            if (!file.exists())
                return null;
            return file.toURI().toURL();
        }
    }

}
