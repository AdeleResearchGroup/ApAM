package fr.imag.adele.apam.apammavenplugin;

import fr.imag.adele.apam.declarations.ComponentReference;
import org.apache.felix.bundlerepository.*;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.utils.version.VersionRange;
import org.apache.maven.plugin.logging.Log;
import org.osgi.framework.*;
import org.apache.felix.utils.log.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

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

    RepositoryAdmin repoAdmin;

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
                throw new Exception("No valid repository");
            }


        } else {
            throw new Exception("No repository URLs specified");
        }
    }

    public List<ApamCapability> getApAMCapabilities(String name, VersionRange versions) {

        List<ApamCapability> capabilities = new ArrayList<ApamCapability>();

        // TODO: add checking of the version,
        // si non spécifié explicitement, toutes les versions conviennent
        // (au contraire, il faudra les rajouter dans le filtre)


        Resource[] resources = repoAdmin.discoverResources(
                new Requirement[] {
                    repoAdmin.getHelper().requirement(
                    "apam-component", "(name="+name+")")
                }
        );

        if(resources == null || resources.length<1) {
            return null;
        }
        for(Resource res : resources) {
            logger.info("Resource found : "+res.getId());

            // TODO, zut, vu que l'on utilise le nom comme clef, on ne peux pas avoir plusieurs version du même composant


            return null;


            // switch sur le type de composant, ensuite création de la component declaration correspondantes
            // il manquera à priori les relations et les heritages de properties



            this.name = name;

            this.isInstantiable = true;
            this.isExclusive = false;
            this.isSingleton = false;
            this.isShared = true;
            this.isDefinedInstantiable = false;
            this.isDefinedExclusive = false;
            this.isDefinedSingleton = false;
            this.isDefinedShared = false;

            reference = generateReference();
            properties = new HashMap<String, String>();
            providedResources = new HashSet<ResourceReference>();
            relations = new HashSet<RelationDeclaration>();
            predefinedLinks = new HashSet<LinkDeclaration>();
            definitions = new ArrayList<PropertyDefinition>();


            //Does not try to call resolver because:
            // 1° There is no running OSGi platform and therefore it may fail
            // 2° There is no need for transitive dependency checking
            // (if an apam component is there, all its dependencies have been already verified)
//            Resolver resolver = repoAdmin.resolver();
//            resolver.add(res);
//            if(resolver.resolve()) {
//                logger.info("Resolve OK for this one");
//                return res;
//            } else {
//                Reason[] reasons = resolver.getUnsatisfiedRequirements();
//                for (int i = 0; i < reasons.length; i++) {
//                    logger.warn("Unable to resolve: " + reasons[i].getRequirement());
//                }
//            }
        }
        return null;
    }


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
