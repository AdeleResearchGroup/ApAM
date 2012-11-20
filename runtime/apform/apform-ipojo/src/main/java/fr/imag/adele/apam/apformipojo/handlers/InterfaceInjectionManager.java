package fr.imag.adele.apam.apformipojo.handlers;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apformipojo.ApformIpojoComponent;
import fr.imag.adele.apam.core.DependencyInjection;
import fr.imag.adele.apam.core.InterfaceReference;

/**
 * This class keeps track of an APAM interface dependency, it handles the calculation of the target 
 * services based on updates to the application model.
 * 
 * @author vega
 * 
 */
public class InterfaceInjectionManager implements DependencyInjectionManager {

	
	/**
	 * The associated resolver
	 */
	private final Resolver				resolver;
	
	/**
	 * The dependency injection managed by this dependency
	 */
	private final DependencyInjection 	injection;

	/**
	 * The metadata of the field that must be injected
	 */
    private final Class<?>				fieldClass;
    private final boolean 				isCollection;
	
    /**
     * The list of target APAM instances of this dependency.
     */
    private final Set<Instance> 		targetServices;

    /**
     * The last injected value.
     * 
     * This is a cached value that must be recalculated in case of update of the dependency.
     */
    private Object             			injectedValue;

    
    
    public InterfaceInjectionManager(ComponentFactory factory, Resolver resolver, DependencyInjection injection) throws ClassNotFoundException {
        
    	assert injection.getResource() instanceof InterfaceReference;
    	
        this.resolver 	= resolver;
        this.injection	= injection;
        
        /*
         * Get field metadata
         * 
         * TODO We keep a reference to the class of the field, this may prevent the class loader from
         * being garbage collected and the class from being updated. We need to verify what is the
         * behavior of OSGi when the class of a field is updated, and adjust this implementation
         * accordingly.
         */
        

        FieldMetadata field = factory.getPojoMetadata().getField(injection.getName());
        String fieldType	= FieldMetadata.getReflectionType(field.getFieldType());
        
        this.fieldClass 	= factory.loadClass(fieldType);
        this.isCollection	= injection.isCollection();

        /*
         * Initialize target services
         */
        targetServices 	= new HashSet<Instance>();
        injectedValue 	= null;
        
    	resolver.addInjection(this);
    }

    /**
     * The dependency injection associated to this manager
     */
    @Override
    public DependencyInjection getDependencyInjection() {
    	return injection;
    }
    
    /**
     * Get an XML representation of the state of this dependency
     */
    public Element getDescription() {
    	
		Element dependencyDescription = new Element("injection", ApformIpojoComponent.APAM_NAMESPACE);
		dependencyDescription.addAttribute(new Attribute("dependency", injection.getDependency().getIdentifier()));
		dependencyDescription.addAttribute(new Attribute("target", injection.getDependency().getTarget().toString()));
		dependencyDescription.addAttribute(new Attribute("name", injection.getName()));
		dependencyDescription.addAttribute(new Attribute("type", injection.getResource().toString()));
		dependencyDescription.addAttribute(new Attribute("isAggregate",	Boolean.toString(injection.isCollection())));
		
		/*
		 * show the current state of resolution. To avoid unnecessary synchronization overhead make a copy of the
		 * current target services and do not use directly the field that can be concurrently modified
		 */
		Set<Instance> resolutions = new HashSet<Instance>();
		synchronized (this) {
			resolutions.addAll(targetServices);
		}
		
		dependencyDescription.addAttribute(new Attribute("resolved",Boolean.toString(!resolutions.isEmpty())));
		for (Instance target : resolutions) {
			Element bindingDescription = new Element("binding", ApformIpojoComponent.APAM_NAMESPACE);
			bindingDescription.addAttribute(new Attribute("target", target.getName()));
			dependencyDescription.addElement(bindingDescription);
		}
		
		return dependencyDescription;
	
    }

    /**
     * Interface injection doesn't require any external service, so it is always available
     */
    @Override
    public boolean isValid() {
    	return true;
    }
    
    /* (non-Javadoc)
	 * @see fr.imag.adele.apam.apformipojo.handlers.DependencyInjectionManager#addTarget(fr.imag.adele.apam.Instance)
	 */
    @Override
	public void addTarget(Instance target) {

        /*
         * Add this target and invalidate cache
         */
        synchronized (this) {
            targetServices.add(target);
            injectedValue = null;
        }

    }

    /* (non-Javadoc)
	 * @see fr.imag.adele.apam.apformipojo.handlers.DependencyInjectionManager#removeTarget(fr.imag.adele.apam.Instance)
	 */
    @Override
	public void removeTarget(Instance target) {

        /*
         * Remove this target and invalidate cache
         */
        synchronized (this) {
            targetServices.remove(target);
            injectedValue = null;
        }
    }

    /* (non-Javadoc)
	 * @see fr.imag.adele.apam.apformipojo.handlers.DependencyInjectionManager#substituteTarget(fr.imag.adele.apam.Instance, fr.imag.adele.apam.Instance)
	 */
    @Override
	public void substituteTarget(Instance oldTarget, Instance newTarget) {

        /*
         * substitute the target atomically and invalidate the cache
         */
        synchronized (this) {

            if (!targetServices.contains(oldTarget))
                return;

            targetServices.remove(oldTarget);
            targetServices.add(newTarget);
            injectedValue = null;

        }
    }

 
    public void onSet(Object pojo, String fieldName, Object value) {
        /*
         * If the field is nullified we interpret this as an indication from the
         * component to release the currently bound instances and force a resolution
         */
    	if (value == null)
    		resolver.unresolve(this);
    }

    public Object onGet(Object pojo, String fieldName, Object value) {
    	
        synchronized (this) {
        	
        	/*
        	 * First try the fast path, use the cached value if still valid
        	 */
         	if (injectedValue != null)
        		return injectedValue;
         	
         	/*
         	 * Next handle the case in which we need to update the cached value, but the
         	 * dependency is resolved
         	 */
         	if (! targetServices.isEmpty()) {
            	injectedValue = getInjectedValue();
            	return injectedValue;
            }
            
            /*
             * The worst case is when we need to resolve the dependency.
             * 
             * IMPORTANT notice that resolution is performed outside the synchronization block. This is 
             * because resolution is a side-effect process that can trigger wire notifications for this
             * dependency. These notifications can originate in other threads (for example in the cases
             * when the resolution triggers a deployment) and that would lead to deadlocks if we keep
             * this object locked.
             */
        }
        
        /*
         * Ask APAM to resolve the dependency. Depending on the application policies this may throw
         * an error, or block the thread until the dependency is fulfilled, or do nothing.
         * 
         * Resolution has as side-effect a modification of the target services.
         */ 
       	resolver.resolve(this);
 
   		/*
		 * update cached values after resolution
		 */
         synchronized (this) {
    		injectedValue 	= !targetServices.isEmpty() ? getInjectedValue() : null;
    		return injectedValue;
        }

    }

    /**
     * Get the value to be injected in the field. The returned object depends on the cardinality of the dependency.
     * 
     * For scalar dependencies, returns directly the service object associated with the target instance. For unresolved
     * dependencies returns null.
     * 
     * For aggregate dependencies, returns a collection of service objects. The kind of collection is chosen to match
     * the declared class of the field. For unresolved dependencies returns null.
     * 
     * In principle the returned object should not be aliased (by keeping a reference to it or passing it in parameters
     * to other classes), as it is potentially dynamically recalculated every time the field is accessed.
     * 
     * In the case of aggregate dependencies, the returned collection should be immutable, as it is calculated by APAM
     * according to global application policies.
     * 
     * We suppose that components are well behaved and follow these restrictions, so we do not have a complex machinery
     * to enforce this. This method directly return the service object or a non thread-safe mutable collection of
     * service objects.
     * 
     * TODO if we want to support a less restrictive programming model (allowing aliasing), or to be more robust against
     * bad behaved components, we should use smart proxies and collections backed up by the information in this
     * dependency object.
     * 
     */
    private final Object getInjectedValue() {
    	
    	/*
    	 * For scalar dependencies return any of the service objects wired
    	 */
    	if (! isCollection)
    		return targetServices.iterator().next().getServiceObject();
    	
        /*
         * For arrays, we need to reflectively build a type conforming array initialized
         * to the list of service objects
         */
        if (fieldClass.isArray()) {
        	
            int index = 0;
        	Object array = Array.newInstance(fieldClass.getComponentType(),targetServices.size());
            for (Instance targetService : targetServices) {
            	Array.set(array, index++, targetService.getServiceObject());
            }
           return array;
        }
        
        /*
         * For collections, use an erased Object collection of the service objects, with
         * the type that that better fits the class of the field
         */
        Collection<Object> serviceObjects = null;

        if (Vector.class.isAssignableFrom(fieldClass)) {
        	serviceObjects = new Vector<Object>(targetServices.size());
        }
        else if (List.class.isAssignableFrom(fieldClass)) {
        	serviceObjects = new ArrayList<Object>(targetServices.size());
        }
        else if (Set.class.isAssignableFrom(fieldClass)) {
        	serviceObjects = new HashSet<Object>(targetServices.size());
        }
        else if (Collection.class.isAssignableFrom(fieldClass)) {
        	serviceObjects = new ArrayList<Object>(targetServices.size());
        }
        else 
        	return null;
        
        /*
         * fill the collection with the service objects
         */
        for (Instance targetService : targetServices) {
        	serviceObjects.add(targetService.getServiceObject());
        }
        
        return serviceObjects;

    }

}
