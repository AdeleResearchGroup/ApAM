package fr.imag.adele.apam.declarations.repository.acr;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.utils.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.Version;

import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.encoding.Decoder;
import fr.imag.adele.apam.declarations.encoding.Reporter;
import fr.imag.adele.apam.declarations.encoding.Reporter.Severity;
import fr.imag.adele.apam.declarations.encoding.acr.CapabilityParser;

/**
 * Created by thibaud on 07/08/2014.
 * This class is designed to reproduce the RepositoryAdmin and Resolver behavior (from OBR BundleRepository)
 * WITHOUT OSGi running (BundleContext is mocked and OSGi services won't be provided)
 */
public class ApamComponentRepository {


    private final RepositoryAdmin repoAdmin;
    private Reporter reporter;

    public ApamComponentRepository(URL[] repositories, Reporter reporter) throws Exception {
    	
    	this.reporter = reporter;
    	
        if (repositories == null || repositories.length == 0) {
        	info("No repository URLs specified");
        	this.repoAdmin = null;
        	return;
        }
        
        repoAdmin = createRepositoryAdmin(repositories[0].toExternalForm());
        info("RepositoryAdmin created successfully (with mocked BundleContext)");
        for (URL repoURL : repositories) {
            try {
                repoAdmin.addRepository(repoURL);
            } catch (Exception exc) {
                warning("Error when adding repository " + repoURL + ", reason " + exc.getMessage());
            }
        }

        if (repoAdmin.listRepositories().length == 0) {
            warning("No valid repository");
        }

    }

    /**
     * Get all declarations found in ACR containing the specified component.
     * 
     */
	public List<ComponentDeclaration> getComponents(ComponentReference<?> reference) {
		return getComponents(reference.any());
	}
	
    /**
     * Get all declarations found in ACR containing the specified versions of the component.
     * 
     */
	public List<ComponentDeclaration> getComponents(ComponentReference<?>.Versioned reference) {

		String name 		= reference.getComponent().getName();
		String versionRange = reference.getRange();
		
        List<ComponentDeclaration> components = new ArrayList<ComponentDeclaration>();
        info("looking for apam-component : "+name+", version range :"+versionRange);


        if(repoAdmin == null || repoAdmin.listRepositories() == null || repoAdmin.listRepositories().length == 0) {
            info("No valid repository, returning empty capability list");
            return components;
        }

        String requirement;

        if(versionRange != null && ! versionRange.isEmpty()) {
            requirement= "(&(name="+name+")";
            requirement+=parseVersionRange(versionRange);
            requirement+=")";
        } else {
            requirement= "(name="+name+")";
        }

        info("requirement research (ldap filter) "+ requirement);
        Resource[] resources = repoAdmin.discoverResources(new Requirement[] {
        							repoAdmin.getHelper().requirement("apam-component", requirement)}
        						);

        if(resources == null || resources.length<1) {
            return components;
        }
        
        Decoder<Capability> parser = new CapabilityParser();
        
        for(Resource res : resources) {
            info("Resource found : "+res.getId());

            for(Capability capability : res.getCapabilities()) {
            	
            	if (!CapabilityParser.isComponent(capability))
            		continue;

            	/*
            	 *  A priori on va parser toutes les capabilities mêmes celles qu'on ne cherche pas
                 * afin d'économiser des performance et ne pas reparser n fois le même fichier ?
            	 *
            	 *
            	if (!CapabilityParser.getComponent(capability).equals(reference.getComponent()))
            		continue;
            	
            	*/
            	
               	components.add( parser.decode(capability,reporter));
            }
            
        }
        
        return components;
    }

	public String parseVersionRange(String versionRange) {
        try {
        	return filter(versionRange);
        } catch(Exception parseException) {
        	warning("Error when parsing the version "+versionRange+":"+parseException.getMessage());
        	return null;
        }
	}

	/**
	 * Get the OBR filter corresponding to a given version range specification
	 */
	public static String filter(String range) throws ParseException {
        
		StringBuilder filter = new StringBuilder();
        	
        int rangeSeparator 	= range.indexOf(",");
        if(rangeSeparator != -1) {

        	if ( !range.startsWith("(") && !range.startsWith("["))
            	throw new ParseException("Versioned range does not start with a correct  delimiter",1);
            	
        	if ( !range.endsWith(")") && !range.endsWith("]"))
        		throw new ParseException("Versioned range does not end with a correct  delimiter",range.length());

        	String start 	= range.substring(1,rangeSeparator);
        	String end		= range.substring(rangeSeparator+1,range.length()-1);
            	
        	validateFormat(start);
            validateFormat(end);
            	
            limit(filter,start,true,range.startsWith("["));
            limit(filter,end,false,range.endsWith("]"));
                
        } else {
        	validateFormat(range);
        	filter.append("(version=").append(range).append(")");
        }

        return filter.toString();
	}
		   
    private static void limit(StringBuilder filter, String limit, boolean inferior, boolean closed) throws ParseException {
        
    	filter.append("(version").append(inferior ? ">=" : "<=").append(limit).append(")");
        
    	if( !closed ) {
            filter.append("(!(version=").append(limit).append("))");
        }
        
    }

    private static void validateFormat(String version) throws ParseException {
        
    	if(version == null || version.isEmpty()) {
            throw new ParseException("Version is empty ",0);
        }
        
        try{// This one does the checking of a properly formated version
            Version.parseVersion(version);
        } catch (IllegalArgumentException exc) {
            throw new ParseException(exc.getMessage(),0);
        }
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
        when(systemBundle.getHeaders()).thenReturn(new Hashtable<String,String>());
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

	public final void error(String message) {
		reporter.report(Severity.ERROR, message);
	}
	
	public final void warning(String message) {
		reporter.report(Severity.WARNING, message);
	}

	public final void info(String message) {
		reporter.report(Severity.INFO, message);
	}

}
