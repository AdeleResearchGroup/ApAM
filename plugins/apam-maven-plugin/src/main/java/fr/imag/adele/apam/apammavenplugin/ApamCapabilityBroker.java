package fr.imag.adele.apam.apammavenplugin;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Version;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.encoding.Decoder;
import fr.imag.adele.apam.declarations.repository.acr.ApamComponentRepository;

/**
 * Created by thibaud on 11/08/2014.
 */
public class ApamCapabilityBroker {

	/**
	 * This class is used to represent a fully qualified component reference, including name and version.
	 * 
	 * Two component version references are considered equals if their component and versions both exactly match. 
	 * However, if the version is not specified, the reference matches any other version of the same component.
	 * 
	 * This class is intended to be used as the key of a map of component declarations (that are usually loaded
	 * incrementally from a repository) that can be searched for specific version or some version in a range.
	 * 
	 * @author vega
	 *
	 */
	private static class VersionReference {
	
		private final ComponentReference<?> component;
		private final Version version;

		public VersionReference(ComponentReference<?> component, Version version) {
			this.component	= component;
			this.version	= version;
		}

		public VersionReference(ComponentReference<?> component) {
			this(component, (Version) null);
		}
		
		public VersionReference(ComponentReference<?> component, String version) {
			this(component, version != null ? new Version(version) : null);
		}

		public VersionReference(ComponentDeclaration component) {
			this(component.getReference(), component.getProperties().get(CST.VERSION));
		}
		
		public VersionReference(ComponentDeclaration component, String version) {
			this(component.getReference(), version);
		}

		
		@Override
		public boolean equals(Object object) {
			
			if (object == null)
				return false;
			
			if (this == object)
				return true;
			
			if (! (object instanceof VersionReference))
				return false;

			/*
			 * Otherwise, compare name and version, take into account the special case in which a version
			 * is not specified
			 */
			VersionReference that = (VersionReference) object;
			return this.component.equals(that.component) && (this.version == null || that.version == null || this.version.equals(that.version));
		}

		/**
		 * Notice that the hash code only uses the component name. Although this may cause some reduced performance
		 * for hashtables, it enables the use of ranges of versions {@link VersionRange} and references without 
		 * explicit versions as keys to lookup maps indexed by objects of this class.
		 */
		@Override
		public int hashCode() {
			return component.hashCode();
		}
		
	}


	/**
	 * This class is a simple evaluator for a version range specification that can be used as key to lookup a map
	 * indexed by {@link VersionReference} keys.
	 * 
	 */
	private static class VersionRange {
	
		private final ComponentReference<?> component;
		
		private final Version low;
		private final boolean includeLow;
		
		private final Version up;
		private final boolean includeUp;
		
		public VersionRange(ComponentReference<?> component, Version low, boolean includeLow, Version up, boolean includeUp) {
			
			this.component = component;
			
			this.low		= low;
			this.includeLow	= includeLow;
			this.up			= up;
			this.includeUp	= includeUp;
		}
		
		/**
		 * We redefine equality to be able to use objects of this class as lookup keys for maps indexed
		 * by {@link VersionReference}
		 */
		public boolean equals(Object object) {
			
			if (object == null)
				return false;
						
			if (object == this)
				return true;
			
			
			if ( !(object instanceof VersionReference ) )
				return false;
			
			/*
			 * compare to a version reference
			 */
			VersionReference reference = (VersionReference) object;
			
			if (! this.component.equals(reference.component))
				return false;
			
			if (reference.version == null)
				return true;
			
			boolean greaterThanLower	= low != null ?	low.compareTo(reference.version) <= (includeLow ? 0 : -1) : true;
			boolean lessThanUppper 		= up != null ?	up.compareTo(reference.version) >= (includeUp ? 0 : 1) : true;
			
			return greaterThanLower && lessThanUppper;
		}

		/**
		 * Notice that the hash code only uses the component name. This is necessary to ensure equality is
		 * well defined in the case this {@link VersionRange} is compared to a {@link VersionReference}
		 */
		@Override
		public int hashCode() {
			return component.hashCode();
		}
	}
	
	/**
	 * Converts a versioned reference into a key that can be used to lookup in the different maps held by this
	 * broker
	 */
	private static final Object key(ComponentReference<?>.Versioned reference) {
	
		String range = reference.getRange();

		/*
		 * No range is specified e match any version
		 */
    	if (range == null)
    		return new VersionReference(reference.getComponent());
		
		boolean includeLow	= !range.startsWith("(");
		boolean includeUp	= !range.endsWith(")");
        
        if (range.startsWith("(") || range.startsWith("["))
        	range = range.substring(1);

        if (range.endsWith(")") || range.endsWith("]"))
        	range = range.substring(0,range.length()-1);

        
        int rangeSeparator 	= range.indexOf(",");
        
        /*
         * A single version is specified, we match an exact revision
         */
        if(rangeSeparator == -1)
        	return new VersionReference(reference.getComponent(),range);

        /*
         * A range is specified build a filter to evaluate
         */
       	String low	= range.substring(0,rangeSeparator).trim();
       	String up	= range.substring(rangeSeparator+1,range.length()).trim();
        
       	Version lowVersion	= !low.isEmpty() ? new Version(low) : null;
       	Version upVersion	= !up.isEmpty() ? new Version(up) : null;
       	
		return new VersionRange(reference.getComponent(),lowVersion,includeLow,upVersion,includeUp);
		
	}
	
    /**
     * internal capabilities are the Apam components declared within the current built
     */
    private  final Map<VersionReference, ApamCapability> internalCapabilities = new HashMap<VersionReference,ApamCapability>();

     /**
     * external capabilities are the Apam components found in maven dependencies
     * 
     */
    private  final Map<VersionReference, ApamCapability> externalCapabilities = new HashMap<VersionReference,ApamCapability>();

    /**
     * This is the cache of capabilities loaded from the repository
     * 
     */
    private  final Map<VersionReference, WeakReference<ApamCapability>> cache = new HashMap<VersionReference, WeakReference<ApamCapability>>();

    /**
     * The ACR resolver used to load the capabilities
     * 
     */
   private final ApamComponentRepository acrResolver;


    public ApamCapabilityBroker(List<ComponentDeclaration> components, String version, List<ComponentDeclaration> dependencies, ApamComponentRepository acrResolver) {
    	
    	this.acrResolver = acrResolver;
    	
        for (ComponentDeclaration component : components) {
            internalCapabilities.put(new VersionReference(component,version), new ApamCapability(this,component));
        }

        for (ComponentDeclaration component : dependencies) {
             externalCapabilities.put(new VersionReference(component),new ApamCapability(this,component));
        }
    }

 
    public ApamCapability get(ComponentReference<?> reference) {
        
    	if (reference == null) {
            return null;
        }
        
        return get(reference.any());
    }

    public ApamCapability get(ComponentReference<?>.Versioned reference) {
        
    	if (reference == null) {
            return null;
        }
        
    	if (reference.getComponent().getName().equals(Decoder.UNDEFINED)) {
            return null;
        }

        return getCapability(reference);

    }
    
    public ApamCapability get(String name) {
        return getCapability(new ComponentReference<ComponentDeclaration>(name).any());
    }

    public ApamCapability get(String name, String versionRange) {
    	return getCapability(new ComponentReference<ComponentDeclaration>(name).range(versionRange));
    }


    private ApamCapability getCapability(ComponentReference<?>.Versioned reference) {

    	/*
    	 * Get the key to lookup the table
    	 */
    	Object key = key(reference);
    	
    	// Step 1 : if the capability is declared inside the artifact being built
        ApamCapability capability = internalCapabilities.get(key);
        
        // Step 2 : if already declared outside (a dependency used several times)
        if(capability == null) {
        	capability = externalCapabilities.get(key);
        }

        // Step 3 : try to find in the cache of ACR capabilities
        if(capability == null) {
        	WeakReference<ApamCapability> cachedReference = cache.get(key); 
        	capability = cachedReference != null ? cachedReference.get() : null;
        }
        
        // Step 4 : just search the repository and load the declaration
        if(capability == null && acrResolver!= null) {
        	
        	ApamCapability loadedVersion = null;
            for (ComponentDeclaration declaration : acrResolver.getComponents(reference)) {
            	loadedVersion = new ApamCapability(this,declaration);
                cache.put(new VersionReference(declaration), new WeakReference<ApamCapability>(loadedVersion));
            }
            
            capability = loadedVersion;
        }

        return capability;
    }


    public ApamCapability getGroup(ApamCapability capability) {
        return get(capability.getDeclaration().getGroupVersioned());
    }

}
