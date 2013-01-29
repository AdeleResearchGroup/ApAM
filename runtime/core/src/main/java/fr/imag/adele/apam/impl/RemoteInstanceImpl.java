package fr.imag.adele.apam.impl;

import java.util.Map;
import java.util.Set;

import org.osgi.framework.Filter;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ResourceReference;

public class RemoteInstanceImpl extends InstanceImpl implements Instance {

	String url;
	Object serviceObject;
	
	public RemoteInstanceImpl(String url,Composite composite, ApformInstance apforminstance,Object serviceObject) throws InvalidConfiguration {
		
		super(composite,apforminstance);
		
		this.url=url;
		this.serviceObject=serviceObject;
	}

	@Override
	public Composite getAppliComposite() {
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public Implementation getImpl() {
		//throw new UnsupportedOperationException("method not available in root instance"); failed in ApamResolverImpl.java:297
		return null;
	}

	@Override
	public Object getServiceObject() {
		return serviceObject;
	}

	@Override
	public Specification getSpec() {
		//throw new UnsupportedOperationException("method not available in root instance");
		return null;
	}

	@Override
	public boolean isSharable() {
		return true;
	}

	@Override
	public Set<Instance> getWireDests(String depName) {
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public Set<Instance> getWireDests() {
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public Set<Wire> getWires() {
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public Set<Wire> getWires(String dependencyName) {
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public Set<Wire> getWires(Specification spec) {
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public boolean createWire(Instance to, String depName,
			boolean hasConstraints, boolean promotion) {
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public void removeWire(Wire wire) {
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public Set<Wire> getInvWires() {
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public Set<Wire> getInvWires(String depName) {
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public Wire getInvWire(Instance destInst) {
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public Wire getInvWire(Instance destInst, String depName) {
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public Set<Wire> getInvWires(Instance destInst) {
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public Set<Component> getMembers() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public Component getGroup() {
		throw new UnsupportedOperationException("method not available in root instance");
	}

	@Override
	public CompositeType getFirstDeployed() {
		return null;
	}

	@Override
	public boolean isInstantiable() {
		return false;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	@Override
	public boolean isShared() {
		return false;
	}

	@Override
	public boolean match(String goal) {
		return false;
	}

	@Override
	public boolean match(Filter goal) {
		return false;
	}

	@Override
	public boolean match(Set<Filter> goals) {
		return false;
	}

	@Override
	public String getProperty(String attribute) {
		return null;
	}

	@Override
	public boolean setProperty(String attr, String value) {
		return false;
	}

	@Override
	public Map<String, Object> getAllProperties() {
		return null;
	}

	@Override
	public boolean setAllProperties(Map<String, String> properties) {
		return false;
	}

	@Override
	public boolean removeProperty(String attr) {
		return false;
	}

	@Override
	public Map<String, String> getValidAttributes() {
		return null;
	}

	@Override
	public Set<ResourceReference> getAllProvidedResources() {
		return null;
	}
	
}
