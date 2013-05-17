/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.apform.impl.handlers;

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

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.impl.ApformComponentImpl;
import fr.imag.adele.apam.declarations.RelationInjection;
import fr.imag.adele.apam.declarations.InterfaceReference;

/**
 * This class keeps track of an APAM interface relation, it handles the
 * calculation of the target services based on updates to the application model.
 * 
 * @author vega
 * 
 */
public class InterfaceInjectionManager implements RelationInjectionManager {

	
	/**
	 * The associated resolver
	 */
	private final Resolver				resolver;
	
	/**
	 * The relation injection managed by this relation
	 */
	private final RelationInjection		injection;

	/**
	 * The metadata of the field that must be injected
	 */
    private final Class<?>				fieldClass;
    private final boolean 				isCollection;
	
    /**
	 * The list of target APAM instances of this relation.
	 */
    private final Set<Component> 		targetServices;

    /**
	 * The last injected value.
	 * 
	 * This is a cached value that must be recalculated in case of update of the
	 * relation.
	 */
    private Object             			injectedValue;

    
    
    public InterfaceInjectionManager(ComponentFactory factory, Resolver resolver, RelationInjection injection) throws ClassNotFoundException {
        
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
        

        FieldMetadata field 		= factory.getPojoMetadata().getField(injection.getName());
        String fieldType			= FieldMetadata.getReflectionType(field.getFieldType());
        
        this.fieldClass 			= factory.loadClass(fieldType);
        this.isCollection			= injection.isCollection();
 
        /*
         * Initialize target services
         */
        targetServices 				= new HashSet<Component>();
        injectedValue 				= null;
        
    	resolver.addInjection(this);
    }

    /**
	 * The relation injection associated to this manager
	 */
    @Override
	public RelationInjection getRelationInjection() {
    	return injection;
    }
    
    /**
	 * Get an XML representation of the state of this relation
	 */
    @Override
	public Element getDescription() {
    	
		Element relationDescription = new Element("injection",
				ApformComponentImpl.APAM_NAMESPACE);
		relationDescription.addAttribute(new Attribute("relation", injection
				.getRelation().getIdentifier()));
		relationDescription.addAttribute(new Attribute("target", injection
				.getRelation().getTarget().toString()));
		relationDescription.addAttribute(new Attribute("name", injection
				.getName()));
		relationDescription.addAttribute(new Attribute("type", injection
				.getResource().toString()));
		relationDescription.addAttribute(new Attribute("isAggregate", Boolean
				.toString(injection.isCollection())));
		
		/*
		 * show the current state of resolution. To avoid unnecessary synchronization overhead make a copy of the
		 * current target services and do not use directly the field that can be concurrently modified
		 */
		Set<Component> resolutions = new HashSet<Component>();
		synchronized (this) {
			resolutions.addAll(targetServices);
		}
		
		relationDescription.addAttribute(new Attribute("resolved", Boolean
				.toString(!resolutions.isEmpty())));
		for (Component target : resolutions) {
			Element bindingDescription = new Element("binding", ApformComponentImpl.APAM_NAMESPACE);
			bindingDescription.addAttribute(new Attribute("target", target.getName()));
			relationDescription.addElement(bindingDescription);
		}
		
		return relationDescription;
	
    }

    /**
     * Interface injection doesn't require any external service, so it is always available
     */
    @Override
    public boolean isValid() {
    	return true;
    }
    
    /*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.imag.adele.apam.apform.impl.handlers.relationInjectionManager#addTarget
	 * (fr.imag.adele.apam.Instance)
	 */
    @Override
	public void addTarget(Component target) {

        /*
         * Add this target and invalidate cache
         */
        synchronized (this) {
            targetServices.add(target);
            injectedValue = null;
        }

    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see
	 * fr.imag.adele.apam.apform.impl.handlers.relationInjectionManager#removeTarget
	 * (fr.imag.adele.apam.Instance)
	 */
    @Override
	public void removeTarget(Component target) {

        /*
         * Remove this target and invalidate cache
         */
        synchronized (this) {
            targetServices.remove(target);
            injectedValue = null;
        }
    }

    @Override
	public void onSet(Object pojo, String fieldName, Object value) {
        /*
         * If the field is nullified we interpret this as an indication from the
         * component to release the currently bound instances and force a resolution
         */
    	if (value == null)
    		resolver.unresolve(this);
    }

    @Override
	public Object onGet(Object pojo, String fieldName, Object value) {
    	
        synchronized (this) {
        	
        	/*
        	 * First try the fast path, use the cached value if still valid
        	 */
         	if (injectedValue != null)
        		return injectedValue;
         	
         	/*
			 * Next handle the case in which we need to update the cached value,
			 * but the relation is resolved
			 */
         	if (! targetServices.isEmpty()) {
            	injectedValue = getInjectedValue();
            	return injectedValue;
            }
            
            /*
			 * The worst case is when we need to resolve the relation.
			 * 
			 * IMPORTANT notice that resolution is performed outside the
			 * synchronization block. This is because resolution is a
			 * side-effect process that can trigger wire notifications for this
			 * relation. These notifications can originate in other threads (for
			 * example in the cases when the resolution triggers a deployment)
			 * and that would lead to deadlocks if we keep this object locked.
			 */
        }
        
        /*
		 * Ask APAM to resolve the relation. Depending on the application
		 * policies this may throw an error, or block the thread until the
		 * relation is fulfilled, or do nothing.
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
	 * Get the value to be injected in the field. The returned object depends on
	 * the cardinality of the relation.
	 * 
	 * For scalar dependencies, returns directly the service object associated
	 * with the target instance. For unresolved dependencies returns null.
	 * 
	 * For aggregate dependencies, returns a collection of service objects. The
	 * kind of collection is chosen to match the declared class of the field.
	 * For unresolved dependencies returns null.
	 * 
	 * In principle the returned object should not be aliased (by keeping a
	 * reference to it or passing it in parameters to other classes), as it is
	 * potentially dynamically recalculated every time the field is accessed.
	 * 
	 * In the case of aggregate dependencies, the returned collection should be
	 * immutable, as it is calculated by APAM according to global application
	 * policies.
	 * 
	 * We suppose that components are well behaved and follow these
	 * restrictions, so we do not have a complex machinery to enforce this. This
	 * method directly return the service object or a non thread-safe mutable
	 * collection of service objects.
	 * 
	 * TODO if we want to support a less restrictive programming model (allowing
	 * aliasing), or to be more robust against bad behaved components, we should
	 * use smart proxies and collections backed up by the information in this
	 * relation object.
	 * 
	 */
    private final Object getInjectedValue() {
    	
    	/*
    	 * TODO change injection to better handle this new case 
    	 */
    	String injectionClass	= injection.getResource().as(InterfaceReference.class).getJavaType();
    	boolean injectComponent = injectionClass.equals(Instance.class.getName()) ||
    							  injectionClass.equals(Implementation.class.getName()) ||
    							  injectionClass.equals(Specification.class.getName()) ||
    							  injectionClass.equals(Component.class.getName()) ;
    	
    	/*
    	 * For scalar dependencies return any of the target objects wired
    	 */
    	if (! isCollection) {
    		Component target = targetServices.iterator().next();
    		return injectComponent ? target : ((Instance)target).getServiceObject();
    	}
    	
        /*
         * For arrays, we need to reflectively build a type conforming array initialized
         * to the list of target objects
         */
        if (fieldClass.isArray()) {
        	
            int index = 0;
        	Object array = Array.newInstance(fieldClass.getComponentType(),targetServices.size());
            for (Component targetService : targetServices) {
            	Array.set(array, index++, injectComponent ? targetService :((Instance)targetService).getServiceObject() );
            }
           return array;
        }
        
        /*
         * For collections, use an erased Object collection of the target objects, with
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
         * fill the collection with the target objects
         */
        for (Component targetService : targetServices) {
        	serviceObjects.add(injectComponent ? targetService :((Instance)targetService).getServiceObject() );
        }
        
        return serviceObjects;

    }

}
