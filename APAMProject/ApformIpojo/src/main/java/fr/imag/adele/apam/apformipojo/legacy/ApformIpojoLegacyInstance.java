package fr.imag.adele.apam.apformipojo.legacy;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.InstanceManager;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.InstanceDeclaration;

public class ApformIpojoLegacyInstance implements ApformInstance {

	/**
	 * The iPojo instance represented by this proxy
	 */
	private final ComponentInstance ipojoInstance;


	/**
	 * the corresponding APAM declaration 
	 */
	private final InstanceDeclaration declaration;

	/**
	 * The associated APAM instance
	 */
	@SuppressWarnings("unused")
	private Instance apamInstance;



	public ApformIpojoLegacyInstance(ComponentInstance ipojoInstance) {
		this.ipojoInstance	= ipojoInstance;
		ImplementationReference<?> implementation = new ApformIPojoLegacyImplementation.Reference(ipojoInstance.getFactory().getName());
		this.declaration	= new InstanceDeclaration(implementation,ipojoInstance.getInstanceName(),null);
	}
	
	@Override
	public void setInst(Instance apamInstance) {
		this.apamInstance = apamInstance;
	}


    /**
     * Apform: get the service object of the instance
     */
    @Override
    public Object getServiceObject() {
        return ((InstanceManager)ipojoInstance).getPojoObject();
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
	public InstanceDeclaration getDeclaration() {
		return declaration;
	}

}
