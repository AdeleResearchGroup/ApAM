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


import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
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
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;

/**
 * This class tracks services dynamically registered in the OSGi registry for implementation managed by the
 * OSGI Manager @see OSGiMan
 * 
 * @author vega
 * 
 */
@org.apache.felix.ipojo.annotations.Component(name = "ApformOSGiTracker" , immediate=true)
@Instantiate(name = "ApformOSGiTracker-Instance")

public class ApformOSGiTracker implements ServiceTrackerCustomizer {

    /**
     * The reference to the APAM platform
     */
	@Requires
    private Apam                apam;

    /**
     * The instances service tracker.
     */
    private ServiceTracker      instancesServiceTracker;

    /**
     * The bundle context associated with this tracker
     */
    private final BundleContext context;


    public ApformOSGiTracker(BundleContext context) {
        this.context = context;
    }

    /**
     * Callback to handle instance binding
     */
    public synchronized boolean instanceBound(ApformOSGiInstance osgiInstance) {

   		/*
   		 * Ignore service registration if its corresponding implementation is not managed in APAM (by the OSGi Manager)
   		 */
   		Implementation implementation = CST.componentBroker.getImpl(osgiInstance.getDeclaration().getImplementation().getName());
   		if (implementation == null)
   			return false;

   		/*
   		 * Ignore service registrations already lazily reified by the manager
   		 */
   		Instance instance = CST.componentBroker.getInst(osgiInstance.getDeclaration().getName());
   		if (instance != null)
   			return false;
   		
   		/*
   		 * Add to APAM all other dynamic service registrations 
   		 */
   		Apform2Apam.newInstance(osgiInstance);
        return true;
    }

    /**
     * Callback to handle instance unbinding
     */
    public synchronized void instanceUnbound(ApformOSGiInstance osgiInstance) {
    	
    	Instance instance = CST.componentBroker.getInst(osgiInstance.getDeclaration().getName());
    	if (instance != null)
    		((ComponentBrokerImpl)CST.componentBroker).disappearedComponent(instance.getName()) ;
    }

    /**
     * Starting.
     */
    @Validate
    public void start() {
    	
    	Filter filter;
		try {
			filter = context.createFilter("(" + Constants.OBJECTCLASS + "=*)");
	        instancesServiceTracker = new ServiceTracker(context,filter, this);
	        instancesServiceTracker.open(true);
		} catch (InvalidSyntaxException ignored) {
		}
    }

    /**
     * Stopping.
     */
    @Invalidate
    public void stop() {
        instancesServiceTracker.close();
    }

    @Override
    public Object addingService(ServiceReference reference) {

        /*
         * Ignore events while APAM is not available
         */
        if (apam == null)
            return null;

        /*
         * ignore services that are iPojo, these are treated separately
         * 
         * In this tracker we avoid getting the service object, as this 
         * may interfere with delayed service creation when using the
         * service factory pattern
         *
         */
        
        if (reference.getProperty("factory.name") != null)
        	return null;

        if (reference.getProperty("instance.name") != null)
        	return null;
        
        ApformOSGiInstance osgiInstance	= new ApformOSGiInstance(reference);
        if (instanceBound(osgiInstance))
            return osgiInstance;

        /*
         * If service is not a recognized OSGi legacy instance, don't track it
         */
        return null;
    }

    @Override
    public void removedService(ServiceReference reference, Object instance) {

       ApformOSGiInstance osgiInstance	= (ApformOSGiInstance) instance;
       instanceUnbound(osgiInstance);
       osgiInstance.dispose();
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
        

}