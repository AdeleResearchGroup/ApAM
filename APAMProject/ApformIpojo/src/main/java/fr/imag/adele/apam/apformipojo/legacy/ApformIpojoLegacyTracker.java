package fr.imag.adele.apam.apformipojo.legacy;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.IPojoFactory;
import org.apache.felix.ipojo.Pojo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apformipojo.ApformIpojoImplementation;
import fr.imag.adele.apam.apformipojo.ApformIpojoInstance;

/**
 * This class tracks iPojo legacy implementations and instances and register them in APAM
 * 
 * @author vega
 *
 */
public class ApformIpojoLegacyTracker implements ServiceTrackerCustomizer {

	/**
	 * The reference to the APAM platform
	 */
	private Apam apam;

    /** 
     * The instances service tracker.
     */
    private ServiceTracker      instancesServiceTracker;

    /**
     * The bundle context associated with this tracker
     */
    private final BundleContext	context;
    
    
    public ApformIpojoLegacyTracker(BundleContext context) {
    	this.context = context;
    }
    
	/**
	 * Callback to handle factory binding
	 */
	public void factoryBound(Factory factory) {
		
		if (factory instanceof ApformIpojoImplementation)
			return;
		
		if (factory instanceof IPojoFactory) {
			ApformImplementation implementation = new ApformIPojoLegacyImplementation((IPojoFactory)factory);
			Apform2Apam.newImplementation(implementation.getName(),implementation);
		}
	}
	
	/**
	 * Callback to handle factory unbinding
	 */
	public void factoryUnbound(Factory factory) {
		if (factory instanceof ApformIpojoImplementation)
			return;
		
		if (factory instanceof IPojoFactory) {
			Apform2Apam.vanishImplementation(factory.getName());
		}
		
	}
	
	/**
	 * Callback to handle instance binding
	 */
	public void instanceBound(ComponentInstance instance) {
		
	}
	
	/**
	 * Callback to handle instance unbinding
	 */
	public void instanceUnbound(ComponentInstance instance) {
	}


    /**
     * Starting.
     */
    public void start() {
 
    	try {
            Filter filter = context.createFilter("(instance.name=*)");
            instancesServiceTracker = new ServiceTracker(context, filter, this);
            instancesServiceTracker.open();

        } catch (InvalidSyntaxException e) {
        	e.printStackTrace(System.err);
        }
    }

    /**
     * Stopping.
     */
    public void stop() {
        instancesServiceTracker.close();
    }
    
    
	@Override
	public Object addingService(ServiceReference reference) {
		Object service = context.getService(reference);

		/*
		 * Ignore events while APAM is not available
		 */
		if (apam == null)
			return null;

		/*
		 * ignore services that are not iPojo
		 */
		if (!(service instanceof Pojo))
			return  null;
		
		ComponentInstance ipojoInstance = ((Pojo)service).getComponentInstance();
		
		/*
		 * ignore handler instances
		 */
		if (ipojoInstance.getFactory() instanceof HandlerFactory)
			return null;
		
		/*
		 * Ignore native APAM components
		 */
		if (ipojoInstance instanceof ApformIpojoInstance)
			return null;
		
		
		/*
		 * Ignore  hybrid components already registered by the dependency handler
		 */
		if (ipojoInstance.getInstanceDescription().getHandlerDescription("fr.imag.adele.apam:dependency") != null)
			return null;

		
		ApformIpojoLegacyInstance apformInstance = new ApformIpojoLegacyInstance(ipojoInstance);
		Apform2Apam.newInstance(apformInstance.getName(), apformInstance);
		return service;
	}

	@Override
	public void removedService(ServiceReference reference, Object service) {

		if (!(service instanceof Pojo))
			return;
		
		ComponentInstance ipojoInstance = ((Pojo) service).getComponentInstance();
		Apform2Apam.vanishInstance(ipojoInstance.getInstanceName());
		context.ungetService(reference);
	}

	@Override
	public void modifiedService(ServiceReference reference, Object service) {
	}



}
