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
import org.apache.felix.ipojo.util.Logger;

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
	 * The factory of the source component of the dependency
	 */
	private final ComponentFactory 	factory;
	
	/**
	 * The associated resolver
	 */
	private final Resolver		resolver;
	

	/**
	 * The dependency injection managed by this dependency
	 */
	private final DependencyInjection injection;
	
    /**
     * The list of target services.
     */
    private final Set<Instance> targetServices;

    /**
     * The last injected value.
     * 
     * This is a cached value that must be recalculated in case of update of the dependency.
     */
    private Object             	injectedValue;

    /**
     * The last injected value type.
     * 
     * This is a cached value that must be recalculated in case of update of the dependency.
     */
    private String             injectedType;

    /**
     * Whether this dependency is satisfied by a target service.
     * 
     * This is a cached value that must be recalculated in case of update of the dependency.
     */
    private boolean            isResolved;

    public InterfaceInjectionManager(ComponentFactory factory, Resolver resolver, DependencyInjection injection) {
        
    	assert injection.getResource() instanceof InterfaceReference;
    	
    	this.factory	= factory;
        this.resolver 	= resolver;
        this.injection	= injection;
        
        targetServices 	= new HashSet<Instance>();
        injectedValue 	= null;
        injectedType 	= null;
        isResolved 		= false;
        
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
		dependencyDescription.addAttribute(new Attribute("resolved",Boolean.toString(isResolved())));

		if (isResolved()) {
			for (Instance target : targetServices) {
				Element bindingDescription = new Element("binding", ApformIpojoComponent.APAM_NAMESPACE);
				bindingDescription.addAttribute(new Attribute("target", target.getName()));
				dependencyDescription.addElement(bindingDescription);
			}
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

            /*
             * In the case of scalar dependencies we try to not unnecessarily invalidate the cache by keeping the
             * already used target.
             * 
             * TODO This is an anomalous case, the APAM should not add a new target to an scalar dependency without
             * removing the previous ones. This may happen only if there is an application model that is not coherent
             * with the dependency metadata.
             */
        	
        	/*
        	 * TODO  When substituting dependencies, the existing dependency is not removed before the new one is
        	 * added, so we have to replace anyway. Modify APAM code to use substituteDependency 
        	 * 
            if (isScalar() && (targetServices.size() != 0)) {
                return;
            }
            
        	 */
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

    /**
     * Whether this dependency is satisfied by a target service.
     * 
     */
    public boolean isResolved() {
        synchronized (this) {
            /*
             * Return the cached value, if it has not been invalidated
             */
            if (injectedValue != null)
                return isResolved;

            /*
             * update cached value
             */
            isResolved = !targetServices.isEmpty();

            return isResolved;
        }

    }

    public void onSet(Object pojo, String fieldName, Object value) {
        // Nothing to do, this should never happen as we exclusively handle the field's value
    }

    public Object onGet(Object pojo, String fieldName, Object value) {

        /*
         * Verify if there is a service fault (a required dependency is not present) and delegate to APAM handling of
         * this case.
         */
        if (!isResolved()) {

            /*
             * Ask APAM to resolve the dependency. Depending on the application policies this may throw an error, or
             * block the thread until the dependency is fulfilled, or keep the dependency unresolved in the case of
             * optional dependencies.
             * 
             * Resolution has as side-effect a modification of the target services.
             */ 
        	resolver.resolve(this);
        }

         return getFieldValue(fieldName);
    }

    /**
     * Get the value to be injected in the field. The returned object depends on the cardinality of the dependency.
     * 
     * For scalar dependencies, returns directly the service object associated with the target instance. For unresolved
     * optional dependencies it return null.
     * 
     * For aggregate dependencies, returns a collection of service objects. The kind of collection is chosen to match
     * the declared class of the field. For unresolved optional dependencies returns an empty collection.
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
    private Object getFieldValue(String fieldName) {

        synchronized (this) {
        	
            
            /*
             * Handle first the most common case of scalar dependencies.
             */
        	if (! injection.isCollection()) {
            	/*
                 * Return the cached value, if it has not been invalidated.
                 */ 
        		if (injectedValue != null)
        			return injectedValue;
        		
        		/*
        		 * update cached value
        		 */
                isResolved = !targetServices.isEmpty();
        		injectedValue = targetServices.isEmpty() ? null : targetServices.iterator().next().getServiceObject();
                return injectedValue;        		
        	}
        	
            /* For the aggregate dependencies, the type of the returned Collection depends on the type of the declared field.
             *  
             * TODO Currently we only cache a single value for a dependency. For different collection types we need to
             * evaluate if it is worth caching all accessed fields.
             */

            FieldMetadata field = factory.getPojoMetadata().getField(fieldName);
            String fieldType	= FieldMetadata.getReflectionType(field.getFieldType());

        	/*
             * Return the cached value, if it has not been invalidated.
             */ 
           if (injectedValue != null && injectedType.equals(fieldType))
                return injectedValue;

	   		/*
	   		 * update cached value to a private copy of the resolution target
	   		 */
            isResolved = !targetServices.isEmpty();
            injectedValue = getServiceObjectCollection(fieldType);
            injectedType = fieldType;

            return injectedValue;
        }

    }

    /**
     * Returns a collection of service objects that is compatible with the class of the field that will be injected.
     * 
     */
    private Object getServiceObjectCollection(String fieldType) {

        try {

            /*
             * return the collection that better fits the field declaration
             */
            Class<?> fieldClass = factory.loadClass(fieldType);

            /*
             * For arrays we need to reflectively build a type conforming array 
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
             * For collections use an erased Object collection
             */
            List<Object> serviceObjects = new ArrayList<Object>(targetServices.size());
            for (Instance targetService : targetServices) {
            	serviceObjects.add(targetService.getServiceObject());
            }

 
            if (Vector.class.isAssignableFrom(fieldClass)) {
                return new Vector<Object>(serviceObjects);
            }

            if (List.class.isAssignableFrom(fieldClass)) {
                return serviceObjects;
            }

            if (Set.class.isAssignableFrom(fieldClass)) {
                return new HashSet<Object>(serviceObjects);
            }

            if (Collection.class.isAssignableFrom(fieldClass)) {
                return serviceObjects;
            }

        } catch (ClassNotFoundException unexpected) {
            factory.getLogger().log(Logger.ERROR,"error accesing field for APAM dependency " + injection.getDependency().getIdentifier(),unexpected);
        }

        return null;
    }


}
