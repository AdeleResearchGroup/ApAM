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
package fr.imag.adele.apam.apform.legacy.osgi;

import java.util.HashSet;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.DynamicManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;


/**
 * This class is in charge of reifying native OSGi services as Apam instances, so
 * that they can be used directly by Apam applications using the relation resolution
 * mechanisms.
 * 
 * This class uses a simple strategy to decide which services to reify in Apam. Applications
 * MUST define a specification to represent the required services, and this manager will try to
 * match native services to defined specifications.
 * 
 * Matching of a specification is based on the following criteria :
 * 
 * - The specification MUST NOT declare dependencies
 * - The specification MUST provide only interfaces, not messages
 * - The registered interfaces of the service MUST include the provided interfaces of
 *   the specification. 
 * - The defined properties of the specification MUST include the registered properties
 *   of the service 
 *    
 *    
 * TODO currently this class has an important performance overhead, as it reacts to every event
 * in the registry by trying to match all the declared Apam specifications. A more fine-grained
 * approach will be to build a service tracker for each Apam specification with the appropriate
 * registry filter, that reacts only to meaningfull events.    
 * @author vega
 *
 */

@org.apache.felix.ipojo.annotations.Component(name = "OSGiMan" , immediate=true)
@Provides(specifications={DynamicManager.class})
@Instantiate(name = "OSGiMan-Instance")

public class OSGiMan implements DynamicManager, ServiceTrackerCustomizer {


	/**
	 * A reference to the APAM machine
	 */
	@Requires(proxy = false)
	private Apam apam;

    /**
     * The instances service tracker.
     */
    private ServiceTracker      instancesServiceTracker;

    /**
     * The bundle context associated with this manager
     */
    private final BundleContext context;


	@Override
	public String getName() {
		return "OSGiMan";
	}

    public OSGiMan(BundleContext context) {
        this.context = context;
    }
    
	@Validate
	private synchronized void start() {
		ApamManagers.addDynamicManager(this);
		
    	Filter filter;
		try {
			filter = context.createFilter("(" + Constants.OBJECTCLASS + "=*)");
	        instancesServiceTracker = new ServiceTracker(context,filter, this);
	        instancesServiceTracker.open(true);
		} catch (InvalidSyntaxException ignored) {
		}
		
	}
	
	@Invalidate
	private synchronized void stop() {
		ApamManagers.removeDynamicManager(this);
        instancesServiceTracker.close();
	}

	/**
	 * Whether the specification is a possible candidate to be matched to an OSGi services
	 */
	private static boolean isMatchable(Specification specification) {
		
		if (!specification.getRelations().isEmpty())
			return false;
		
		if (! specification.getDeclaration().getProvidedResources(MessageReference.class).isEmpty())
			return false;

		if (specification.getDeclaration().getProvidedResources(InterfaceReference.class).isEmpty())
			return false;
		
		return true;
	}
	
	/**
	 * Whether the service is a possible candidate to be matched to APAM specifications
	 */
	private static boolean isMatchable(ServiceReference reference) {
		
        /*
         * ignore services that are iPojo, these are treated separately
         * 
         * In this tracker we avoid getting the service object, as this 
         * may interfere with delayed service creation when using the
         * service factory pattern
         *
         */
        
        if (reference.getProperty("factory.name") != null)
        	return false;

        if (reference.getProperty("instance.name") != null)
        	return false;
		
		return true;
	}

	
	/**
	 * Whether the service matches the specification
	 */
	private static boolean matches(Specification specification, ServiceReference reference) {
		
    	Set<InterfaceReference> providedServices = new HashSet<InterfaceReference>();
		for (String interfaceName : (String[]) reference.getProperty(Constants.OBJECTCLASS)) {
			providedServices.add(new InterfaceReference(interfaceName));
		}
		
		return providedServices.containsAll(specification.getDeclaration().getProvidedResources(InterfaceReference.class));
	}
	
    /**
     * Find all specifications matching the osgi service
     */
    private Set<Specification> getMatchingSpecifications(ServiceReference reference) {
    	

		Set<Specification> matches	= new HashSet<Specification>();
		
		for (Specification specification : CST.componentBroker.getSpecs()) {
			
			if (isMatchable(specification) && matches(specification,reference))
				matches.add(specification);
		}
		
		return matches; 
    	
    }

	/**
     * Get a representation of all the possible Apam instances that match the specified
     * reference.
     * 
     * Notice that we return apform objects that ARE NOT reified in Apam, this can be used
     * to decide if the instance has already be created or not. 
     */
    private Set<ApformOSGiInstance> getMatchingInstances(ServiceReference reference) {

		Set<Specification> specifications 	= getMatchingSpecifications(reference);
		Set<ApformOSGiInstance> instances	= new HashSet<ApformOSGiInstance>();

		for(Specification specification : specifications) {
			instances.add(new ApformOSGiInstance(specification, reference));
		}
		
		return instances;
    }
    
    @Override
    public Object addingService(ServiceReference reference) {

        /*
         * Ignore events while APAM is not available
         */
        if (apam == null)
            return null;

        /*
         * ignore services that are not matchable
         * 
         */
        if (! isMatchable(reference))
        	return null;
        
        /*
         * Create all instances and implementations required in APAM
         */
        Set<ApformOSGiInstance> instances = getMatchingInstances(reference);
    	for(ApformOSGiInstance instance : instances) {

			ApformImplementation implementation = new ApformOSGiImplementation(instance);

			if (CST.componentBroker.getImpl(implementation.getDeclaration().getName()) == null) {
				Apform2Apam.newImplementation(implementation);
			}

			if (CST.componentBroker.getInst(instance.getDeclaration().getName()) == null) {
				Apform2Apam.newInstance(instance);
			}

    	}

    	/*
    	 * Track all services to be sure to be informed when they are unregistered 
    	 */
        return reference;
    }

    @Override
    public void removedService(ServiceReference reference, Object tracked) {

    	/*
    	 * Destroy all Apam instances created, and garbage-collect implementations
    	 * if needed.
    	 */
        Set<ApformOSGiInstance> instances = getMatchingInstances(reference);
        if (instances.isEmpty())
            return;

    	for(ApformOSGiInstance instance : instances) {

    		Instance apamInstance = CST.componentBroker.getInst(instance.getDeclaration().getName());
    		
    		if (apamInstance == null)
    			continue;
    		
    		Implementation apamImplementation = apamInstance.getImpl();
    		
    		if (apamInstance != null)
    			((ComponentBrokerImpl)CST.componentBroker).disappearedComponent(apamInstance);
    		
    		if (apamImplementation.getInsts().isEmpty())
    			((ComponentBrokerImpl)CST.componentBroker).disappearedComponent(apamImplementation);
    	}

    }

    @Override
    public void modifiedService(ServiceReference reference, Object instance) {
    	
        ApformOSGiInstance osgiInstance	= (ApformOSGiInstance) instance;
    	Instance apamInstnstance 		= CST.componentBroker.getInst(osgiInstance.getDeclaration().getName());

    	if (apamInstnstance == null)
    		return;
    	

        for (String key : reference.getPropertyKeys()) {
            if (!Apform2Apam.isPlatformPrivateProperty(key)) {
                String value = reference.getProperty(key).toString();
                if (value != apamInstnstance.getProperty(key))
                	apamInstnstance.setProperty(key, value);
            }
        }

    }
        


	@Override
	public void addedComponent(Component component) {
		
		/*
		 * If a new specification is defined we need to try match existing services in the registry  
		 */
		if (!(component instanceof Specification))
			return;
		
		Specification specification = (Specification) component;

		if (!isMatchable(specification))
			return;
		
		StringBuilder filter = new StringBuilder();
		filter.append("(").append("&");
		for (InterfaceReference providedInterface : specification.getDeclaration().getProvidedResources(InterfaceReference.class)) {
			filter.append("(").append(Constants.OBJECTCLASS).append("=").append(providedInterface.getName()).append(")");
		}
		filter.append(")");
		
		String filterText = filter.toString();
		
		ServiceReference[] matchedServices = null;
		try {
			matchedServices = context.getAllServiceReferences(null,filterText);
		} catch (InvalidSyntaxException e) {
		}
		
		if (matchedServices == null)
			return;
			
		for (ServiceReference reference : matchedServices)  {
			
			if (!isMatchable(reference))
				continue;
				
			ApformOSGiInstance instance 		= new ApformOSGiInstance(specification, reference);
			ApformImplementation implementation = new ApformOSGiImplementation(instance);

			if (CST.componentBroker.getImpl(implementation.getDeclaration().getName()) == null) {
				Apform2Apam.newImplementation(implementation);
			}

			if (CST.componentBroker.getInst(instance.getDeclaration().getName()) == null) {
				Apform2Apam.newInstance(instance);
			}
				
			}
	}


	@Override
	public void removedComponent(Component component) {
	}

	@Override
	public void removedLink(Link wire) {
	}


	@Override
	public void addedLink(Link wire) {
	}

}
