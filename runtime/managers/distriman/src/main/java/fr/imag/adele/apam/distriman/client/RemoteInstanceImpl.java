package fr.imag.adele.apam.distriman.client;

import org.osgi.framework.Bundle;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.references.components.Versioned;

/**
 * Represents an apam machine
 * 
 * @author jnascimento
 * 
 */
public class RemoteInstanceImpl implements ApformInstance {

    private String url;
    private String id;
    private Object serviceObject;

    private Instance apamInstance = null;

    private final InstanceDeclaration declaration;

    public RemoteInstanceImpl(String dependencyId, String url, Composite composite, Object serviceObject) {
		this.id = dependencyId;
		this.url = url;
		this.serviceObject = serviceObject;
	
		declaration = new InstanceDeclaration(Versioned.any(new RemoteMachine.RemoteImplementationReference(getFullName())),getFullName());
    }

    @Override
    public Instance getApamComponent() {
		// TODO Auto-generated method stub
		return apamInstance;
    }

    @Override
    public Bundle getBundle() {
    	return null;
    }

    @Override
    public InstanceDeclaration getDeclaration() {
    	return declaration;
    }

    public String getFullName() {
    	return String.format("%s_%s", this.id, this.url);
    }

    public String getImplementation() {
    	return declaration.getImplementation().getName();
    }

    @Override
    public Object getServiceObject() {
    	return serviceObject;
    }

    public String getUrl() {
    	return url;
    }

    @Override
    public boolean remLink(Component destInst, String depName) {
    	return false;
    }

    /**
     * @Override public void setInst(Instance asmInstImpl) {
     *           this.apamInstance=asmInstImpl; }
     * @Override public Instance getInst() { return this.apamInstance; }
     **/

    @Override
    public void setApamComponent(Component apamComponent) {
    	apamInstance = (Instance) apamComponent;
    }

    @Override
    public boolean setLink(Component destInst, String depName) {
    	return false;
    }
    
    @Override
    public boolean checkLink(Component destInst, String depName) {
    	return false;
    }    

    @Override
    public void setProperty(String attr, String value) {
    	// throw new UnsupportedOperationException("impossible to change remote instance properties");
    }

    public void setUrl(String url) {
    	this.url = url;
    }

}
