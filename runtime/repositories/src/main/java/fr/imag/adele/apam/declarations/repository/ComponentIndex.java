package fr.imag.adele.apam.declarations.repository;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.osgi.framework.Version;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.Versioned;

/**
 * This class handle a collection of {@link ComponentDeclaration component declarations} indexed by name
 * and version number.
 * 
 * Declarations are only weakly referenced, allowing this index to be used as a cache for frequently used
 * components. It is the responsibility of the client to reload garbage collected entries, the common pattern
 * is to delegate to the index and if no entry is found try to reload it from persistent storage.
 * 
 * @author vega
 *
 */
public class ComponentIndex implements Repository {

	private final ConcurrentMap<ComponentReference<?>, ConcurrentNavigableMap<Version, WeakReference<ComponentDeclaration>>> index;
	
	public ComponentIndex() {

		this.index = new ConcurrentSkipListMap<ComponentReference<?>, ConcurrentNavigableMap<Version,WeakReference<ComponentDeclaration>>>(new Comparator<ComponentReference<?>>() {
			@Override
			public int compare(ComponentReference<?> reference1, ComponentReference<?> reference2) {
				return reference1.getName().compareTo(reference2.getName());
			}
		});
	}
	
	public void clear() {
		index.clear();
	}
	
	@Override
	public <C extends ComponentDeclaration> C getComponent(ComponentReference<C> reference) {
		return getComponent(Versioned.any(reference));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <C extends ComponentDeclaration> C getComponent(Versioned<C> referenceRange) {
		ConcurrentNavigableMap<Version,WeakReference<ComponentDeclaration>> revisions = index.get(referenceRange.getComponent());
		return revisions != null ? (C) select(range(revisions,referenceRange.getRange()),referenceRange.getComponent().getKind()) : null;
	}

	/**
	 * Select one revision from the available versions in the passed map, and purges all garbage collected entries.
	 * 
	 * Notice that we select component declarations that match the specified kind, it may happen that there are 
	 * by mistake different component declaration with the same name and different kind.
	 * 
	 * TODO should we change the signature of the method to throw an exception in case of mismatching kind
	 */
	private static ConcurrentNavigableMap<Version,WeakReference<ComponentDeclaration>> range(ConcurrentNavigableMap<Version,WeakReference<ComponentDeclaration>> revisions, String range) {
	
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
	   	if (floor == null && ceiling == null) {
	   		return revisions;
	   	}
	   	else if (floor == null && ceiling != null) {
	   		return revisions.tailMap(ceiling, includeCeiling);
	   	}
	   	else if (floor != null && ceiling == null) {
	   		return revisions.headMap(floor, includeFloor);
	   	} 
	   	else {
	   		return revisions.subMap(floor, includeFloor, ceiling, includeCeiling);
	   	}
        
	}
	
	/**
	 * Adds a new component to the index. The version of the component is obtained from its declaration, however
	 * if it has not been specified is possible to give a default value to use.
	 * 
	 */
	public void put(ComponentDeclaration component) {
		
		ConcurrentNavigableMap<Version,WeakReference<ComponentDeclaration>> revisions = index.get(component.getReference());
		
		/*
		 * create the revision index on first access
		 */
		if (revisions == null) {
			revisions = new ConcurrentSkipListMap<Version, WeakReference<ComponentDeclaration>>();
			index.put(component.getReference(),revisions);
		}
		
		String encodedVersion = component.getProperty(CST.VERSION);
		
		Version version = encodedVersion != null ?  new Version(encodedVersion) : Version.emptyVersion;
		revisions.put(version, new WeakReference<ComponentDeclaration>(component));
	}

	
	/**
	 * Select one revision from the available versions in the index, and purges all garbage collected entries.
	 * 
	 * Notice that we select component declarations that match the specified kind, it may happen that there is 
	 * by mistake different component declaration with the same name and different kind.
	 * 
	 * TODO should we change the signature of the method to throw an exception in case of mismatching kind
	 */
	private static ComponentDeclaration select(ConcurrentNavigableMap<Version,WeakReference<ComponentDeclaration>> revisions, ComponentKind kind) {
		
		ComponentDeclaration selected = null;
		
		for (Map.Entry<Version,WeakReference<ComponentDeclaration>> revisionEntry : revisions.entrySet()) {
			
			ComponentDeclaration revision = revisionEntry.getValue().get();
			
			if (revision == null) {
				revisions.remove(revisionEntry.getKey());
			}
			else if (revision.getKind() == kind || ComponentKind.COMPONENT == kind) {
				selected = revision;
			}
			
		}
		
		return selected;
	}
}
