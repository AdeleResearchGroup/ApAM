package fr.imag.adele.apam.apform.legacy.osgi;


import org.apache.felix.ipojo.Pojo;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;

/**
 * This class tracks osgi legacy implementations and instances and register
 * them in APAM
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
    public boolean instanceBound(ServiceReference reference) {

    	/*
    	 * Register instance
    	 */
        ApformOSGiInstance apformInstance = new ApformOSGiInstance(reference);
        Apform2Apam.newInstance(apformInstance);
        
        /*
         * Create implementation when the first instance is bound
         */
        Implementation implementation = CST.componentBroker.getImpl(apformInstance.getDeclaration().getImplementation().getName());
        if (implementation == null) {
        	factoryBound(apformInstance);
        }

        return true;
    }

    /**
     * Callback to handle instance unbinding
     */
    public void instanceUnbound(ServiceReference reference) {
    	
        Instance instance 					= CST.componentBroker.getInst(ApformOSGiInstance.getInstanceName(reference));
        ApformOSGiInstance apformInstance 	= (ApformOSGiInstance) instance.getApformInst();
        
    	/*
    	 * Unregister instance
    	 */
    	ComponentBrokerImpl.disappearedComponent(apformInstance.getDeclaration().getName()) ;

        /*
         * Unregister implementation when the last instance is unbound
         */
        Implementation implementation = CST.componentBroker.getImpl(apformInstance.getDeclaration().getImplementation().getName());
        if (implementation.getInsts().isEmpty()) {
        	factoryUnbound(apformInstance);
        }
    
    }

    /**
     * Callback to handle factory binding
     */
    public void factoryBound(ApformOSGiInstance prototype) {
        ApformImplementation implementation = new ApformOSGiImplementation(prototype);
        Apform2Apam.newImplementation(implementation);
    }

    /**
     * Callback to handle factory unbinding
     */
    public void factoryUnbound(ApformOSGiInstance prototype) {
    	ComponentBrokerImpl.disappearedComponent(prototype.getDeclaration().getImplementation().getName()) ;
    }
    
    /**
     * Starting.
     */
    @Validate
    public void start() {
        instancesServiceTracker = new ServiceTracker(context,"*", this);
        instancesServiceTracker.open();
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
         */
        Object service = context.getService(reference);
        if (service instanceof Pojo)
        	return null;
        
        
        if (instanceBound(reference))
            return service;

        /*
         * If service is not a recognized OSGi legacy instance, don't track it
         */
        context.ungetService(reference);
        return null;
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {

        instanceUnbound(reference);
        context.ungetService(reference);
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {

        /*
         * If the service is not reified in APAM, just ignore event
         */
        Instance inst = CST.componentBroker.getInst(ApformOSGiInstance.getInstanceName(reference));
        if (inst == null)
            return;

        /*
         * Otherwise propagate property changes to Apam
         */
        for (String key : reference.getPropertyKeys()) {
            if (!Apform2Apam.isPlatformPrivateProperty(key)) {
                String value = reference.getProperty(key).toString();
                if (value != inst.getProperty(key))
                    inst.setProperty(key, value);
            }
        }
    }
}
