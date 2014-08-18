package fr.imag.adele.apam.apammavenplugin;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.apammavenplugin.helpers.DummyCodeReflection;
import fr.imag.adele.apam.declarations.*;
import fr.imag.adele.apam.util.Attribute;
import fr.imag.adele.apam.util.Util;
import org.apache.felix.bundlerepository.*;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.utils.version.VersionRange;
import org.apache.maven.plugin.logging.Log;
import org.osgi.framework.*;
import org.apache.felix.utils.log.Logger;

import java.net.URL;
import java.text.ParseException;
import java.util.*;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by thibaud on 07/08/2014.
 * This class is designed to reproduce the
 * RepositoryAdmin and Resolver behavior (from OBR BundleRepository)
 * WITHOUT OSGi running (BundleContext is mocked and OSGi services won't be provided)
 */
public class StandaloneACRParser {

    private static Log logger;

    static RepositoryAdmin repoAdmin;


    public StandaloneACRParser(URL[] repositories, Log logger) throws Exception {
        if (repositories != null && repositories.length > 0) {
            this.logger = logger;
            repoAdmin = createRepositoryAdmin(repositories[0].toExternalForm());
            logger.info("RepositoryAdmin created successfully (with mocked BundleContext)");
            for (URL repoURL : repositories) {
                try {
                    repoAdmin.addRepository(repoURL);
                } catch (Exception exc) {
                    logger.warn("Error when adding repository " + repoURL + ", reason " + exc.getMessage());
                }
            }
            if (repoAdmin.listRepositories().length < 1) {
                logger.warn("No valid repository");
            }


        } else {
            logger.info("No repository URLs specified");
        }
    }

    public static List<ApamCapability> getApAMCapabilitiesFromACR(String name, String versionRange) {

        List<ApamCapability> capabilities = new ArrayList<ApamCapability>();
        logger.info("looking for apam-component : "+name+", version range :"+versionRange);


        if(repoAdmin == null || repoAdmin.listRepositories() == null || repoAdmin.listRepositories().length<1) {
            logger.info("No valid repository, returning empty capability list");
            return capabilities;
        }

        // TODO: add checking of the version,
        // si non spécifié explicitement, toutes les versions conviennent
        // (au contraire, il faudra les rajouter dans le filtre)

        String requirement;

        if(versionRange != null && versionRange.length() >0) {
            requirement= "(&(name="+name+")";
            requirement+=parseVersionRange(versionRange);
            requirement+=")";
        } else {
            requirement= "(name="+name+")";
        }

        logger.info("requirement research (ldap filter) "+ requirement);
        Resource[] resources = repoAdmin.discoverResources(
                new Requirement[] {
                        repoAdmin.getHelper().requirement(
                                "apam-component", requirement)
                }
        );

        if(resources == null || resources.length<1) {
            return capabilities;
        }
        for(Resource res : resources) {
            logger.info("Resource found : "+res.getId());

            for(Capability cap : res.getCapabilities()) {
                ApamCapability apacap = parseOBRCapability(cap);
                if(apacap != null) {
                    capabilities.add(apacap);
                }
            }
            // A priori on va parser toutes les capabilities mêmes celles qu'on ne cherche pas afin d'économiser des performance et ne pas reparser n fois le même fichier ?
        }
        return capabilities;
    }


    public static List<ApamCapability> getApAMCapabilitiesFromBundles(String name, String versionRange) {
        List<ApamCapability> capabilities = new ArrayList<ApamCapability>();
        logger.info("looking for apam-component : "+name+", version range :"+versionRange);

        if(repoAdmin == null || repoAdmin.listRepositories() == null || repoAdmin.listRepositories().length<1) {
            logger.warn("No valid repository, returning empty capability list");
            return capabilities;
        }

        Resource[] resources = repoAdmin.discoverResources(
                new Requirement[] {
                        repoAdmin.getHelper().requirement(
                                "apam-component", "(name="+name+")")
                }
        );

        if(resources == null || resources.length<1) {
            return capabilities;
        }
        for(Resource res : resources) {
            logger.info("Resource found : " + res.getId());

            // TODO : A big one (and not a priority)
            // download the resource and use the JarHelper to get the apam components from it

        }
        return capabilities;

    }


    public static ApamCapability parseOBRCapability(Capability cap) {

        if(cap != null && CST.CAPABILITY_COMPONENT.equals(cap.getName()) ) {
            Map<String, Property> props = new HashMap<String, Property>();

            // the Capability.getPropertiesAsMap seems to get only properties values (type is skipped)
            // We have to build the map from the real Property array
            Property[] listPropr = cap.getProperties();
            if(listPropr == null ||listPropr.length<1) {
                logger.warn("No properties found for the capability : "+cap.getName());
                return null;
            }

            for(Property prop : listPropr) {
                props.put(prop.getName(), prop);
            }

            ComponentDeclaration dcl;
            String componentName 	= props.get(CST.NAME).getValue();
            String componentkind	= props.get(CST.COMPONENT_TYPE).getValue();
            logger.info("parsing OBR for : " + componentName + " of type " + componentkind);

            if (CST.SPECIFICATION.equals(componentkind)) {
                dcl = new SpecificationDeclaration(componentName);


            } else if (CST.IMPLEMENTATION.equals(componentkind)) {
                SpecificationReference componentSpec = null;
                if(props.containsKey(CST.PROVIDE_SPECIFICATION)) {
                    componentSpec = new SpecificationReference(props.get(CST.PROVIDE_SPECIFICATION).getValue());
                }

                if(props.containsKey(CST.APAM_COMPOSITE) && CST.V_TRUE.equals(props.get(CST.APAM_COMPOSITE).getValue())) {
                    // This is a composite
                    ComponentReference componentMain = null;
                    if(props.containsKey(CST.APAM_MAIN_COMPONENT)) {
                        componentMain = new ComponentReference(props.get(CST.APAM_MAIN_COMPONENT).getValue());
                    }
                    dcl = new CompositeDeclaration(componentName, componentSpec, componentMain);

                } else {
                    // this is an atomic implementation
                    if(props.containsKey(CST.PROVIDE_CLASSNAME)) {
                        AtomicImplementationDeclaration.CodeReflection className =  new DummyCodeReflection(props.get(CST.PROVIDE_CLASSNAME).getValue());
                        dcl = new AtomicImplementationDeclaration(componentName, componentSpec, className);
                    } else {
                        logger.warn("atomic Implementation with no classname : "+componentName);
                        return null;
                    }
                }
            } else if (CST.INSTANCE.equals(componentkind)) {
                ImplementationReference componentImplementation = new ImplementationReference(props.get(CST.IMPLNAME).getValue());
                dcl = new InstanceDeclaration(componentImplementation, componentName, null);

            } else {
                logger.warn("Unknown apam component type : "+componentkind+" for "+componentName);
                return null;
            }

            // Still there, the declaration is correct, adding the rest of the properties
            if(props.containsKey(CST.SINGLETON)) {
                dcl.setDefinedSingleton(Boolean.valueOf(props.get(CST.SINGLETON).getValue()));
            }
            if(props.containsKey(CST.SHARED)) {
                dcl.setDefinedShared(Boolean.valueOf(props.get(CST.SHARED).getValue()));
            }
            if(props.containsKey(CST.INSTANTIABLE)) {
                dcl.setDefinedInstantiable(Boolean.valueOf(props.get(CST.INSTANTIABLE).getValue()));
            }

            // Check the provides interfaces and messages
            if(props.containsKey(CST.PROVIDE_INTERFACES)) {
                for(String ref : Util.split(props.get(CST.PROVIDE_INTERFACES).getValue())) {
                    dcl.getProvidedResources().add(new InterfaceReference(ref));
                }
            }
            if(props.containsKey(CST.PROVIDE_MESSAGES)) {
                for(String ref : Util.split(props.get(CST.PROVIDE_MESSAGES).getValue())) {
                    dcl.getProvidedResources().add(new MessageReference(ref));
                }
            }

            for (String propertyName : props.keySet()) {
                if(!Attribute.isFinalAttribute(propertyName)
                        && !Attribute.isInheritedAttribute(propertyName)
                        && !Attribute.isBuiltAttribute(propertyName) ) {
                    if(Attribute.isReservedAttributePrefix(propertyName)) {
                        if(propertyName.startsWith(CST.DEFINITION_PREFIX)) {
                            logger.info("Property "+propertyName+" is a definition of property");
                            dcl.getPropertyDefinitions().add(new PropertyDefinition(dcl,
                                    propertyName.substring(CST.DEFINITION_PREFIX.length()),
                                    props.get(propertyName).getType(),
                                    props.get(propertyName).getValue(), null, null, null));
                        }
                        if(propertyName.startsWith(CST.RELATION_PREFIX)) {
                            logger.info("Relation "+propertyName+" is a definition of relation");
                            dcl.getDependencies().add(new RelationDeclaration(dcl.getReference(),
                                    propertyName.substring(CST.RELATION_PREFIX.length()),
                                    new ComponentReference(props.get(propertyName).getValue()),
                                    false));
                        }

                    } else {
                        logger.info("Property " + propertyName + " is not a reserved keyword, it must be an user defined property");
                        dcl.getProperties().put(propertyName, props.get(propertyName).getValue());
                    }
                }

            }

            if(props.containsKey(CST.VERSION)) { // get the OSGi Version of the component
                dcl.getProperties().put(CST.VERSION, props.get(CST.VERSION).getValue());
            }
            logger.info("Capability matching found : "+dcl.getName());

            return new ApamCapability(dcl);



            //
        } else {
            logger.info("Capability " + (cap == null ? null : cap.getName()) + " is not an apam component");
            return null;

        }
    }

    public static String parseVersionRange(String versionRange) {
        String versionReq="";

        try {
            if(versionRange.startsWith("[") ||
                    versionRange.startsWith("(")) {
                versionReq+=parseFloor(versionRange.substring(0,
                        versionRange.indexOf(",")));
                versionReq+=parseCeiling(versionRange.substring(versionRange.indexOf(",")+1));
            } else {
                return parseRequirementVersion(versionRange);
            }
        } catch(Exception exc) {
            logger.warn("Error when parsing the version"+exc.getMessage());
        }

        return versionReq;
    }

    private static String parseFloor(String beginRange) throws ParseException {
        String part = "";
        part+=parseRequirementVersion(beginRange.substring(1));
        if(beginRange.startsWith("(")) {
            part+="(!(version="+beginRange.substring(1)+"))";
        }
        return part;
    }
    private static String parseCeiling(String endRange) throws ParseException {
        String part = "";
        if(endRange.endsWith(")")) {
            part+="(!(version>="+endRange.substring(0, endRange.length()-1)+"))";
            return part;
        } else if (endRange.endsWith("]")){
            part+=parseRequirementVersion(endRange.substring(0, endRange.length()-1));
            part+="(version<="+endRange.substring(0, endRange.length()-1)+")";
            return part;
        } else {
            throw new ParseException("Version range does not stop with a correct ceiling delimiter",
                    endRange.length());
        }
    }

    private static String parseRequirementVersion(String version) throws ParseException {
        if(version == null || version.length()<1) {
            throw new ParseException("Version is empty ",0);
        }
        try{// This one does the checking of a properly formated version
            Version.parseVersion(version);
        } catch (IllegalArgumentException exc) {
            throw new ParseException(exc.getMessage(),0);
        }
        return "(version>="+version+")";
    }


    /**
     * This one is ugly (mockito should not be used to fake BundleContext
     * Bundle context should not be used at all (as we mostly build upon the parser of capabilities)
     * @param defaultRepo
     * @return
     * @throws Exception
     */

    private RepositoryAdmin createRepositoryAdmin(String defaultRepo) throws Exception {
        BundleContext bundleContext = mock(BundleContext.class);
        Bundle systemBundle = mock(Bundle.class);

        // TODO: Change this one
        when(bundleContext.getProperty(RepositoryAdminImpl.REPOSITORY_URL_PROP))
                .thenReturn(defaultRepo);

        when(bundleContext.getProperty(anyString())).thenReturn(null);
        when(bundleContext.getBundle(0)).thenReturn(systemBundle);
        when(systemBundle.getHeaders()).thenReturn(new Hashtable());
        when(systemBundle.getRegisteredServices()).thenReturn(null);
        when(new Long(systemBundle.getBundleId())).thenReturn(new Long(0));
        when(systemBundle.getBundleContext()).thenReturn(bundleContext);
        bundleContext.addBundleListener((BundleListener) anyObject());
        bundleContext.addServiceListener((ServiceListener) anyObject());
        when(bundleContext.getBundles()).thenReturn(new Bundle[]{systemBundle});

        RepositoryAdminImpl repoAdmin = new RepositoryAdminImpl(bundleContext, new Logger(bundleContext));

        // force initialization && remove all initial repositories
        Repository[] repos = repoAdmin.listRepositories();
        for (int i = 0; repos != null && i < repos.length; i++) {
            repoAdmin.removeRepository(repos[i].getURI());
        }

        return repoAdmin;
    }


}
