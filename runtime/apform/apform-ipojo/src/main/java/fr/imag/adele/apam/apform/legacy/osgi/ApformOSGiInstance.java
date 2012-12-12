package fr.imag.adele.apam.apform.legacy.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;

public class ApformOSGiInstance implements ApformInstance {

	/**
	 * The bundle  that registered the instance
	 */
	private final Bundle				bundle;
	
    /**
     * The osgi service reference represented by this object
     */
    private final ServiceReference		reference;

    /**
     * The service object
     */
    private final Object				service;
    
    /**
     * the corresponding APAM declaration
     */
    private final InstanceDeclaration	declaration;

    /**
     * The associated APAM instance
     */
    private Instance                  	apamInstance;

    /**
     * An apform instance to represent a legacy component discovered in the OSGi registry
     * 
     * @param ipojoInstance
     */
    public ApformOSGiInstance(ServiceReference reference) {
    	
    	this.reference	= reference;
    	this.bundle		= reference.getBundle();
        this.service	= bundle.getBundleContext().getService(reference);
        
        String clazz 	= service.getClass().getCanonicalName();

        ImplementationReference<?> implementation = new ApformOSGiImplementation.Reference(clazz);
        
        this.declaration = new InstanceDeclaration(implementation, getInstanceName(reference), null);
        
        for (String key : reference.getPropertyKeys()) {
            if (!Apform2Apam.isPlatformPrivateProperty(key))
                this.declaration.getProperties().put(key, reference.getProperty(key).toString());
        }
        
    }

    public static String getInstanceName(ServiceReference reference) {
    	return "service-"+ reference.getProperty(Constants.SERVICE_ID);
    }

    @Override
    public Bundle getBundle() {
    	return bundle;
    }
    
    @Override
    public InstanceDeclaration getDeclaration() {
        return declaration;
    }

    public ServiceReference getServiceReference() {
    	return reference;
    }
    
    @Override
    public Object getServiceObject() {
        return service;
    }
    
    @Override
    public void setInst(Instance apamInstance) {
        this.apamInstance = apamInstance;
        
        if (apamInstance == null)
        	dispose();
    }

    @Override
    public Instance getInst() {
    	return this.apamInstance;
    }
    

    private void dispose() {
    	if (bundle.getBundleContext() != null)
    		bundle.getBundleContext().ungetService(reference);
    }

    /**
     * Legacy implementations can not be injected with APAM dependencies, so they do not provide
     * injection information
     */
    @Override
    public boolean setWire(Instance destInst, String depName) {
        return false;
    }

    /**
     * Legacy implementations can not be injected with APAM dependencies, so they do not provide
     * injection information
     */
    @Override
    public boolean remWire(Instance destInst, String depName) {
        return false;
    }

    /**
     * Legacy implementations can not be injected with APAM dependencies, so they do not provide
     * injection information
     */
    @Override
    public boolean substWire(Instance oldDestInst, Instance newDestInst, String depName) {
        return false;
    }


    @Override
    public void setProperty(String attr, String value) {
    }

    
}
