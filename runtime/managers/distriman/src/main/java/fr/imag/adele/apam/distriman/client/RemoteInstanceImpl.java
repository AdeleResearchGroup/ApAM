package fr.imag.adele.apam.distriman.client;

import org.osgi.framework.Bundle;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.InstanceDeclaration;

/**
 * Represents an apam machine
 * @author jnascimento
 *
 */
public class RemoteInstanceImpl implements ApformInstance {

	private String url;
	private String id;
	private Object serviceObject;
	
	private Instance apamInstance = null;
	
	private final InstanceDeclaration declaration;
	
	public RemoteInstanceImpl(String dependencyId,String url,Composite composite, Object serviceObject) {
		this.id=dependencyId;
		this.url=url;
		this.serviceObject=serviceObject;
		
		declaration=new InstanceDeclaration(new RemoteMachine.RemoteImplementationReference(getFullName()),getFullName(),null) ;
	}

	public String getFullName(){
		return String.format("%s_%s",this.id,this.url);
	}
	
	public String  getImplementation() {
		return declaration.getImplementation().getName();
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	@Override
	public void setProperty(String attr, String value) {
		//throw new UnsupportedOperationException("impossible to change remote instance properties");
	}
	@Override
	public Bundle getBundle() {
		return null;
	}
	@Override
	public InstanceDeclaration getDeclaration() {
		return declaration;
	}
	@Override
	public Object getServiceObject() {
		return serviceObject;
	}
	@Override
	public boolean setLink(Component destInst, String depName) {
		return false;
	}
	@Override
	public boolean remLink(Component destInst, String depName) {
		return false;
	}
	@Override
	public void setInst(Instance asmInstImpl) {
		this.apamInstance=asmInstImpl;
	}
	@Override
	public Instance getInst() {
		return this.apamInstance;
	}
	
}
