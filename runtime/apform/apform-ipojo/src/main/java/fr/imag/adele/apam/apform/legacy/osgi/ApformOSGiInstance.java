package fr.imag.adele.apam.apform.legacy.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;

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
     * The registered interface
     */
    private final String				registeredInterface;
    
    /**
     * The service object
     */
    private final Object				service;
    
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
    public ApformOSGiInstance(ServiceReference reference, String registeredInterface) {
    	
    	this.reference				= reference;
    	this.registeredInterface	= registeredInterface;
    	
    	this.bundle					= reference.getBundle();
        this.service				= bundle.getBundleContext().getService(reference);
        
        ImplementationReference<?> implementation = new ApformOSGiImplementation.Reference(generateImplementationName());
        
        this.declaration = new InstanceDeclaration(implementation, generateInstanceName(), null);
        
        for (String key : reference.getPropertyKeys()) {
            if (!Apform2Apam.isPlatformPrivateProperty(key))
                this.declaration.getProperties().put(key, reference.getProperty(key).toString());
        }
        
    }

    /**
     * Generate the name of the implementation associated with this instance in Apam
     */
    private String generateImplementationName() {
    	return generateImplementationName(reference,registeredInterface);
    }

    private static String generateImplementationName(ServiceReference reference, String registeredInterface) {
    	String bundle = reference.getBundle().getSymbolicName();
    	
    	if (bundle == null)
    		bundle = (String)reference.getBundle().getHeaders().get("Bundle-Name");
    	
    	return registeredInterface+"[provider="+bundle+"]";
    }

    /**
     * Generate the name of the instance in APAM
     */
    private String generateInstanceName() {
    	return generateInstanceName(reference, registeredInterface);
    }
    
    private static String generateInstanceName(ServiceReference reference, String registeredInterface) {
    	return generateImplementationName(reference,registeredInterface)+"-"+reference.getProperty(Constants.SERVICE_ID);
    }

    
    /**
     * Get the name of all instances that must be created in APAM for this OSGi reference.
     * 
     * Currently we create one instance in Apam for each registered interface.
     * 
     * TODO find a naming convention to register only a single instance for each service
     */
    public static String[] getInstanceNames(ServiceReference reference) {
    	
    	String[] registeredInterfaces	= (String[]) reference.getProperty(Constants.OBJECTCLASS);
    	String[] instanceNames			= new String[registeredInterfaces.length];
    	
    	for (int i = 0; i < registeredInterfaces.length; i++) {
			String registeredInterface	= registeredInterfaces[i];
			instanceNames[i]			= generateInstanceName(reference, registeredInterface);
		}
    	
    	return instanceNames;
    }
    

    @Override
    public Bundle getBundle() {
    	return bundle;
    }
    
    @Override
    public InstanceDeclaration getDeclaration() {
        return declaration;
    }

    public String getRegisteredInterface() {
		return registeredInterface;
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
