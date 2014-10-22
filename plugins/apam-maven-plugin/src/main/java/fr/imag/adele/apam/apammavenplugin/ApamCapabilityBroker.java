package fr.imag.adele.apam.apammavenplugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Version;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.ConstrainedReference;
import fr.imag.adele.apam.declarations.FeatureReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.Versioned;
import fr.imag.adele.apam.declarations.repository.ComponentIndex;
import fr.imag.adele.apam.declarations.repository.Repository;
import fr.imag.adele.apam.declarations.repository.RepositoryChain;
import fr.imag.adele.apam.declarations.repository.acr.ApamComponentRepository;

/**
 * 
 * This class handles a list of APAM Capabilities that are loaded during the current build.
 * 
 * TODO we should try to eliminate ApamCapability and use directly component declarations, 
 * this will simplify considerably the plugin.
 * 
 * NOTE notice that this broker keeps strong references to loaded declarations, in order to
 * keep the association to apam capabilities through out the build 
 */
public class ApamCapabilityBroker {

	
    /**
     * The repository chain to lookup for components : internals, dependencies, externals
     */
    private final Repository repository;
    
    /**
     * The capabilities loaded in the current build
     */
    private final Map<ComponentDeclaration,ApamCapability> capabilities;


    public ApamCapabilityBroker(List<ComponentDeclaration> components, String version, List<ComponentDeclaration> dependencies, ApamComponentRepository acr) {
    	
    	this.capabilities	= new HashMap<ComponentDeclaration, ApamCapability>();
    	
    	/*
    	 * load internals cache
    	 * 
    	 */
    	Version internalVersion = Version.parseVersion(version);
    	ComponentIndex internalIndex = new ComponentIndex();
    	for (ComponentDeclaration internal : components) {
			internalIndex.put(internal,internalVersion);
			addCapability(internal,internalVersion);
		}

    	/*
    	 * load dependencies cache
    	 * 
    	 * TODO we should take the default version from the corresponding maven artifact
    	 */
    	ComponentIndex dependenciesIndex = new ComponentIndex();
    	for (ComponentDeclaration dependency : dependencies) {
    		dependenciesIndex.put(dependency);
    		addCapability(dependency);
		}
    
    	this.repository 	= new RepositoryChain(internalIndex,dependenciesIndex,acr);
    }

    /**
     * Look for an already loaded capability
     */
    public ApamCapability get(ComponentDeclaration declaration) {
    	return capabilities.get(declaration);
    }

    /**
     * associates a new capability for a recently loaded component declaration
     */
    private ApamCapability addCapability(ComponentDeclaration component, Version version) {
    	
    	/*
    	 * create a capability an map it to the declaration
    	 */
    	ApamCapability capability = version == null ? new ApamCapability(this,component) : new ApamCapability(this,component,version);
		capabilities.put(component,capability);
		
		/*
		 * special treatment for instance declarations that are embedded inside a composite
		 * 
		 * TODO this is a workaround to make validation uniform, however there are problems 
		 */
		if (component instanceof CompositeDeclaration) {
			Version compositeVersion = component.getProperty(CST.VERSION) != null ? Version.parseVersion(component.getProperty(CST.VERSION)) : version;
			for (InstanceDeclaration start : ((CompositeDeclaration)component).getInstanceDeclarations()) {
				addCapability(start,compositeVersion);
			}		
		}

		return capability;
    }

    private ApamCapability addCapability(ComponentDeclaration component) {
    	return addCapability(component,null);
    }
    
	public ApamCapability getGroup(ApamCapability apamCapability) {
		Versioned<?> groupReference = apamCapability.getDeclaration().getGroupVersioned();
		return groupReference != null ? getCapability(groupReference) : null;
	}

	public ApamCapability getTargetComponent(ConstrainedReference reference) {
		ComponentReference<?> target = reference.getTarget().as(ComponentReference.class);
		return target != null ? getCapability(Versioned.any(target)) : null;
	}

	public ApamCapability getDeclaringComponent(FeatureReference feature) {
		ComponentReference<?> declaring = feature.getDeclaringComponent();
		return declaring != null ? getCapability(Versioned.any(declaring)) : null;
	}
	
	public ApamCapability getByName(String name) {
		ComponentReference<?> reference = new ComponentReference<ComponentDeclaration>(name);
		return getCapability(Versioned.any(reference));
	}

	public ApamCapability getByReference(ComponentReference<?> reference) {
		return getCapability(Versioned.any(reference));
	}

    private ApamCapability getCapability(Versioned<?> referenceRange) {
    	
    	/*
    	 * Try to find one of the loaded capabilities
    	 */
    	for (ApamCapability loadedCapability : capabilities.values()) {
			
    		if (! loadedCapability.getDeclaration().getReference().equals(referenceRange.getComponent()))
    			continue;

    		if (match(referenceRange.getRange(),loadedCapability.getVersion()))
    			return loadedCapability;
		}
    	
    	
		/*
    	 * If not found, load the declaration from the repository chain and create the APAM capability
    	 */
		ComponentDeclaration component = repository.getComponent(referenceRange);
		
    	if (component != null) {
    		return addCapability(component);
    	}
    	
   		return null;
    }

    /*
     * TODO this should be in class Versioned, but this will introduce a dependency on OSGi classes
     */
    private static boolean match(String range, Version version) {

    	Version floor 			= null;
		Version ceiling			= null;
		
		boolean includeFloor	= true;
		boolean includeCeiling	= true;
		
		/*
		 * parse range specification
		 */
	   	if (range != null) {
	   		
	   		includeFloor	= !range.startsWith("(");
	   		includeCeiling	= !range.endsWith(")");
	   		
	        if (range.startsWith("(") || range.startsWith("["))
	        	range = range.substring(1).trim();

	        if (range.endsWith(")") || range.endsWith("]"))
	        	range = range.substring(0,range.length()-1).trim();

	        
	        int limitSeparator 	= range.indexOf(",");

	        if (limitSeparator == -1) {
	        	floor = ceiling = new Version(range);
	        }
	        else {
		       	String encodedFloor		= range.substring(0,limitSeparator).trim();
		       	String encodedCeiling	= range.substring(limitSeparator+1,range.length()).trim();
		        
		       	floor	= !encodedFloor.isEmpty() 	? new Version(encodedFloor) 	: null;
		       	ceiling	= !encodedCeiling.isEmpty() ? new Version(encodedCeiling) 	: null;
	        }
	        
	   	}

	   	/*
	   	 * Get the filtered view of the map to navigate the range
	   	 */
	   	
	   	boolean greaterThanFloor 	= true;
	   	boolean lessThanCeiling 	= true;
	   	
	   	if (ceiling != null) {
	   		int compare = version.compareTo(ceiling);
	   		lessThanCeiling =  includeCeiling ? compare <= 0 : compare < 0;
	   	}
	   	
	   	if (floor != null) {
	   		int compare = version.compareTo(floor);
	   		greaterThanFloor = includeFloor ? compare >= 0 : compare > 0;
	   	} 
	   	
	   	return greaterThanFloor && lessThanCeiling;
    	
    }


}
