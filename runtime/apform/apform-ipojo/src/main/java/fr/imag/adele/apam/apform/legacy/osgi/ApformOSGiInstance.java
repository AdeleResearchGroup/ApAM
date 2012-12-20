package fr.imag.adele.apam.apform.legacy.osgi;

import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.InterfaceReference;

/**
 * This class represents an OSGi service intereface as an APAM instance
 *
 *
 */
public class ApformOSGiInstance implements ApformInstance {

    /**
     * the corresponding APAM declaration
     */
    private final InstanceDeclaration	declaration;
	
    /**
     * The osgi service reference represented by this object
     */
    private final ServiceReference		reference;

    /**
     * The service object
     */
    private final Object				service;
    
    /**
     * The list of provided interfaces
     */
    private final Set<InterfaceReference> providedInterfaces;
    
	/**
	 * The bundle  that registered the instance
	 */
	private final Bundle				bundle;

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
    	
    	this.reference			= reference;
    	this.bundle				= reference.getBundle();
        this.service			= bundle.getBundleContext().getService(reference);
    	
		this.providedInterfaces = new HashSet<InterfaceReference>();
		for (String interfaceName : (String[]) reference.getProperty(Constants.OBJECTCLASS)) {
			this.providedInterfaces.add(new InterfaceReference(interfaceName));
		}

        
        this.declaration 	= new InstanceDeclaration(generateImplementationName(), generateInstanceName(), null);
        for (String key : reference.getPropertyKeys()) {
            if (!Apform2Apam.isPlatformPrivateProperty(key))
                this.declaration.getProperties().put(key, reference.getProperty(key).toString());
        }
        
    }

    /**
     * Generate the name of the implementation associated with this instance in Apam
     */
    private ImplementationReference<?> generateImplementationName() {
    	String bundle = reference.getBundle().getSymbolicName();
    	
    	if (bundle == null)
    		bundle = (String)reference.getBundle().getHeaders().get("Bundle-Name");
    	
    	StringBuffer interfaces = new StringBuffer();
    	boolean first = true;
    	for (InterfaceReference providedInterface : providedInterfaces) {
			interfaces.append(first? "" : "+").append(providedInterface.getJavaType());
			first = false;
		}
    	return new ApformOSGiImplementation.Reference(interfaces+"[bundle.provider="+bundle+"]");
    }

    /**
     * Generate the name of the instance in APAM
     */
    private String generateInstanceName() {
    	return generateImplementationName().getName()+"-"+reference.getProperty(Constants.SERVICE_ID);
    }
    
    /**
     * The underlying OSGi service reference
     */
    public ServiceReference getServiceReference() {
    	return reference;
    }

    /**
     * The list of provided interfaces of this instance
     */
    public Set<InterfaceReference> getProvidedResources() {
    	return providedInterfaces;
    }
    
    @Override
    public Bundle getBundle() {
    	return bundle;
    }
    
    @Override
    public InstanceDeclaration getDeclaration() {
        return declaration;
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
    

    public void dispose() {
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
