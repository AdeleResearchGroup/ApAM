package fr.imag.adele.apam.declarations.repository.acr;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.text.ParseException;
import java.util.Hashtable;

import org.apache.felix.bundlerepository.Capability;
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

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.encoding.Decoder;
import fr.imag.adele.apam.declarations.encoding.Reporter;
import fr.imag.adele.apam.declarations.encoding.Reporter.Severity;
import fr.imag.adele.apam.declarations.encoding.capability.CapabilityParser;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.Versioned;
import fr.imag.adele.apam.declarations.repository.ComponentIndex;
import fr.imag.adele.apam.declarations.repository.Repository;

/**
 * This class handles a component repository backed up by the felix OSGi Bundle Repository extended
 * with APAM metadata. 
 * 
 * It is intended to be used both at build and runtime. It only relies on the OBR's ability to query
 * its index, and not on installation or runtime platform resolution.
 * 
 * 
 * TODO This class can be used at build-time by mocking-up some of the runtime OSGi dependencies,
 * we rather should try to directly reuse the felix OBR resolver, even if that means depending
 * on private classes. 
 */
public class ApamComponentRepository implements Repository {


    private final Reporter 			reporter;

    private final RepositoryAdmin 	manager;
    private final ComponentIndex	cache;
    
    public ApamComponentRepository(URL[] repositories, Reporter reporter) throws Exception {
    	this(mockManager(repositories), repositories, reporter);
        info("RepositoryAdmin created successfully (with mocked BundleContext)");
    }
    
    public ApamComponentRepository(RepositoryAdmin manager, URL[] repositories, Reporter reporter) throws Exception {
    	
    	this.reporter 	= reporter;
        this.cache 		= new ComponentIndex();

    	
        if (repositories == null || repositories.length == 0) {
        	info("No repository URLs specified");
        	this.manager = null;
        	return;
        }
        else {
        	this.manager = manager;
        }
        
        for (URL repository : repositories) {
            try {
            	manager.addRepository(repository);
            } catch (Exception exc) {
                warning("Error when adding repository " + repository + ", reason " + exc.getMessage());
            }
        }

        if (manager.listRepositories().length == 0) {
            warning("No valid repository");
        }

    }

	/**
	 * Get the OBR filter corresponding to a given version range specification
	 */
	public static String filter(Versioned<?> referenceRange) throws ParseException {
        
		String range			= referenceRange.getRange();
		
		if (range == null)
			throw new ParseException("Version is empty ",0);
		
		StringBuilder filter	= new StringBuilder();
        	
        int rangeSeparator 	= range.indexOf(",");
        if(rangeSeparator != -1) {

        	if ( !range.startsWith("(") && !range.startsWith("["))
            	throw new ParseException("Versioned range does not start with a correct  delimiter",1);
            	
        	if ( !range.endsWith(")") && !range.endsWith("]"))
        		throw new ParseException("Versioned range does not end with a correct  delimiter",range.length());

        	String start 	= range.substring(1,rangeSeparator);
        	String end		= range.substring(rangeSeparator+1,range.length()-1);
            	
        	version(start);
        	version(end);
            	
            limit(filter,start,true,range.startsWith("["));
            limit(filter,end,false,range.endsWith("]"));
                
        } else {
        	version(range);
        	filter.append("("+CST.VERSION+"=").append(range).append(")");
        }

        return filter.toString();
	}
	
    private static void limit(StringBuilder filter, String limit, boolean inferior, boolean closed) throws ParseException {
        
    	filter.append("(").append(CST.VERSION).append(inferior ? ">=" : "<=").append(limit).append(")");
        
    	if( !closed ) {
            filter.append("(!").append("(").append(CST.VERSION).append("=").append(limit).append(")").append(")");
        }
        
    }

    private static Version version(String version) throws ParseException {
        
    	if(version == null || version.isEmpty()) {
            throw new ParseException("Version is empty ",0);
        }
        
        try{
            return Version.parseVersion(version);
        } catch (IllegalArgumentException exc) {
            throw new ParseException(exc.getMessage(),0);
        }
    }
    
	@Override
	public <C extends ComponentDeclaration> C getComponent(ComponentReference<C> reference) {
		return getComponent(Versioned.any(reference));
	}

	@Override
	public <C extends ComponentDeclaration> C getComponent(Versioned<C> referenceRange) {
		
		/*
		 * Try to find a cached component declaration
		 */
		C component = cache.getComponent(referenceRange);
		
		if (component != null)
			return component;
		
		/*
		 * If not found, look directly in the repository and load the cache 
		 */
		try {
			loadResources(referenceRange);
		} catch (Exception error) {
			error("Error loading resources from repository: "+error.getMessage());
		}
		
		return cache.getComponent(referenceRange);
	}

    /**
     * Loads in the cache all declarations found in ACR containing the specified versions of the component.
     */
	private void loadResources(Versioned<?> referenceRange) throws Exception {

		String name 		= referenceRange.getName();
		String versionRange = referenceRange.getRange();
		
        info("loading resources containing apam-component : "+name+", version range :"+versionRange);

        if(manager == null || manager.listRepositories() == null || manager.listRepositories().length == 0) {
            info("No valid repository, returning empty capability list");
            return;
        }

        String requirement;

        if(versionRange != null && !versionRange.isEmpty()) {
            requirement= "(&(name="+name+")";
            requirement+=filter(referenceRange);
            requirement+=")";
        } else {
            requirement= "(name="+name+")";
        }

        info("requirement research (ldap filter) "+ requirement);
        Resource[] resources = manager.discoverResources(new Requirement[] {
        							manager.getHelper().requirement("apam-component", requirement)}
        						);

        if(resources == null || resources.length<1) {
            return;
        }
        
        Decoder<Capability> parser = new CapabilityParser();
        
        for(Resource resource : resources) {
            info("Resource found : "+resource.getId());

            for(Capability capability : resource.getCapabilities()) {
            	
            	if (!CapabilityParser.isComponent(capability))
            		continue;

               	cache.put(parser.decode(capability,reporter), resource.getVersion());
            }
            
        }
        
    }
	

    /**
     * Mock some of the OSGi context to allow using the repository at build time
     */

    private static RepositoryAdmin mockManager(URL[] repositories) throws Exception {
    	
    	if (repositories == null || repositories.length == 0)
    		return null;
    	
        BundleContext bundleContext = mock(BundleContext.class);
        Bundle systemBundle = mock(Bundle.class);

        // TODO: Change this one
        when(bundleContext.getProperty(RepositoryAdminImpl.REPOSITORY_URL_PROP))
                .thenReturn(repositories[0].toExternalForm());

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
        org.apache.felix.bundlerepository.Repository[] repos = repoAdmin.listRepositories();
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
