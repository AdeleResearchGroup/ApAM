package fr.imag.adele.apam.declarations.repository.acr;

import java.net.URL;
import java.text.ParseException;
import java.util.List;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.Version;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.encoding.Decoder;
import fr.imag.adele.apam.declarations.encoding.capability.CapabilityParser;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.VersionedReference;
import fr.imag.adele.apam.declarations.repository.ComponentIndex;
import fr.imag.adele.apam.declarations.repository.Repository;
import fr.imag.adele.apam.declarations.tools.Reporter;
import fr.imag.adele.apam.declarations.tools.Reporter.Severity;

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
    
     
    public ApamComponentRepository(RepositoryAdmin manager, List<URL> repositories, Reporter reporter) throws Exception {
    	
    	this.reporter 	= reporter;
        this.cache 		= new ComponentIndex();

    	
        if (repositories == null || repositories.isEmpty()) {
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
            	info("Repository location "+repository+ " added to ACR");
            } catch (Exception exc) {
                warning("Error when adding repository " + repository + ", reason " + exc.getMessage());
            }
        }

        if (manager.listRepositories().length == 0) {
            warning("No valid repository");
        }

    }

    /**
     * Get the bundle repository manager used by this component repository 
     */
    public RepositoryAdmin getManager() {
    	return manager;
    }
    
	/**
	 * Get the OBR filter corresponding to a given version range specification
	 */
	public static String filter(VersionedReference<?> referenceRange) throws ParseException {
        
		String name		= referenceRange.getName();
		String range	= referenceRange.getRange();
		
		StringBuilder filter	= new StringBuilder();
		
		
		if (range == null) {
			filter.append("(name=").append(name).append(")");
		}
		else {
			
			filter.append("(& ");
			
			filter.append("(name=").append(name).append(")");
			
			
	        int rangeSeparator 	= range.indexOf(",");
	        if(rangeSeparator != -1) {

	        	if ( !range.startsWith("(") && !range.startsWith("["))
	            	throw new ParseException("VersionedReference range does not start with a correct  delimiter",1);
	            	
	        	if ( !range.endsWith(")") && !range.endsWith("]"))
	        		throw new ParseException("VersionedReference range does not end with a correct  delimiter",range.length());

	        	String start 	= range.substring(1,rangeSeparator).trim();
	        	String end		= range.substring(rangeSeparator+1,range.length()-1).trim();
	            	
	        	version(start);
	        	version(end);
	            	
	            limit(filter,start,true,range.startsWith("["));
	            limit(filter,end,false,range.endsWith("]"));
	                
	        } else {
	        	version(range);
	        	filter.append("("+CST.VERSION+"=").append(range).append(")");
	        }
			
	        filter.append(")");
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
		return reference != null? getComponent(VersionedReference.any(reference)) : null;
	}

	@Override
	public <C extends ComponentDeclaration> C getComponent(VersionedReference<C> referenceRange) {
		
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
     * Loads in the cache all declarations found in resources of ACR that contain the specified 
     * component version.
     */
	private void loadResources(VersionedReference<?> referenceRange) throws Exception {

		
        info("loading resources containing apam-component : "+referenceRange.getName()+", version range :"+referenceRange.getRange());

        if(manager == null || manager.listRepositories() == null || manager.listRepositories().length == 0) {
            info("No valid repository, returning empty capability list");
            return;
        }

        info("requirement research (ldap filter) "+ filter(referenceRange));
        Resource[] resources = manager.discoverResources(new Requirement[] {
        							manager.getHelper().requirement("apam-component", filter(referenceRange))}
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

            	ComponentDeclaration component = parser.decode(capability,reporter);

        		/*
        		 * Components coming from the ACR have inherited properties and default values added
        		 * at build time, and the declaration stored in the repository is actually effective.
        		 * 
        		 * However, we must allow members of a group to define properties that are not valued
        		 * in the group, sowe remove then automatically the default values
        		 * 
        		 * TODO We need a way to distinguish default values added at build time, from explicitly
        		 * defined properties that happen to have the same value
        		 */
    			for (PropertyDefinition property : component.getPropertyDefinitions()) {
    				
    				String defaultValue = property.getDefaultValue();
    				String value		= component.getProperty(property.getName());
    				
    				if (value != null &&  defaultValue != null && defaultValue.equals(value)) {
    					component.getProperties().remove(property.getName());
    				}
    			}
            	
            	/*
            	 * Add version if not specified
            	 */
            	addProperty(component,CST.VERSION, "version", resource.getVersion().toString());
            	
            	
               	cache.put(component);
            }
            
        }
        
    }

	/**
	 * Add a property to an existing component
	 * 
	 * NOTE We may be modifying a component that has already property information attached (either because the
	 * component has already been built, and we are loading it as a dependency, or because the user has added
	 * the information manually) so we need to be careful not to override it
	 * 
	 */
	private static final void addProperty(ComponentDeclaration component, String property, String type, String value) {
		
		/*
		 */
		PropertyDefinition defintition = component.getPropertyDefinition(property);
		if (defintition == null) {
			defintition = new PropertyDefinition(component.getReference(), property, type, null);
			component.getPropertyDefinitions().add(defintition);
		}

		String currentValue = component.getProperty(property);
		if (currentValue == null) {
			component.getProperties().put(property, value);
		}
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
