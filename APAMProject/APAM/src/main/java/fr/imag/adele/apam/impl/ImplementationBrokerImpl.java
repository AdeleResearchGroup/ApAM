package fr.imag.adele.apam.impl;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.ImplementationBroker;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformCompositeType;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.util.ApamInstall;

public class ImplementationBrokerImpl implements ImplementationBroker {

	private Logger logger = LoggerFactory
			.getLogger(ImplementationBrokerImpl.class);

	private final Set<Implementation> implems = Collections
			.newSetFromMap(new ConcurrentHashMap<Implementation, Boolean>());

	@Override
	public Implementation addImpl(CompositeType composite, ApformImplementation apfImpl) {

		String implementationName = apfImpl.getDeclaration().getName();

		assert apfImpl != null;
		assert getImpl(implementationName) == null;

		if (apfImpl == null) {
			logger.error("Error adding implementation:  null Apform instance");
			return null;
		}

		Implementation implementation = getImpl(implementationName);
		if (implementation != null) {
			logger.error("Error adding implementation: already exists" + implementationName);
			return implementation;
		}

		if (composite == null)
			composite = CompositeTypeImpl.getRootCompositeType();

    	/*
    	 * Create and register the object in the APAM state model
    	 */
        try {

    		// create a primitive or composite implementation
    		if (apfImpl instanceof ApformCompositeType) {
    			implementation = new CompositeTypeImpl(composite,(ApformCompositeType)apfImpl);
    		} else {
    			implementation = new ImplementationImpl(composite, apfImpl);
    		}

    		((ImplementationImpl) implementation).register(null);
    		return implementation;

		} catch (InvalidConfiguration configurationError) {
			logger.error("Error adding implementation: exception in APAM registration",configurationError);
		}

		return null;
		
	}

	@Override
	public Implementation createImpl(CompositeType compo, String implName, URL url, Map<String, String> properties) {
		assert implName != null && url != null;

		Implementation impl = getImpl(implName);
		if (impl != null) { // do not create twice
			return impl;
		}
		impl = ApamInstall.intallImplemFromURL(url, implName);
		if (impl == null) {
			logger.error("deployment failed :" + implName + " at URL " + url);
			return null;
		}

		if (compo != null && !((CompositeTypeImpl) compo).isSystemRoot())
			((CompositeTypeImpl) compo).deploy(impl);

		return impl;
	}

	// Not in the interface. No control
	/**
	 * TODO change visibility, currently this method is public to be visible
	 * from Apform
	 */
	public void removeImpl(Implementation implementation) {
		removeImpl(implementation, true);
	}

	protected void removeImpl(Implementation implementation, boolean notify) {
		((ComponentImpl) implementation).unregister();
	}

	@Override
	public Set<Implementation> getImpls() {
		return Collections.unmodifiableSet(implems);
		// return new HashSet<ASMImpl> (implems) ;
	}

	@Override
	public Implementation getImpl(String name) {

		if (name == null)
			return null;

		for (Implementation impl : implems) {
			if (name.equals(impl.getName()))
				return impl;
		}
		return null;
	}

	@Override
	public Implementation getImpl(String name, boolean wait) {
		Implementation implementation = getImpl(name);
		if (implementation != null || !wait)
			return implementation;

		/*
		 * If not found wait and try again
		 */
		Apform2Apam.waitForComponent(name);
		implementation = getImpl(name);

		if (implementation == null) // should never occur
			logger.debug("wake up but implementation is not present " + name);

		return implementation;
	}

	@Override
	public Set<Implementation> getImpls(Filter goal) throws InvalidSyntaxException {
		if (goal == null)
			return getImpls();
		
		Set<Implementation> ret = new HashSet<Implementation>();
		for (Implementation impl : implems) {
			if (impl.match(goal))
				ret.add(impl);
		}
		return ret;
	}

	@Override
	public Set<Implementation> getImpls(Specification spec) {
		Set<Implementation> impls = new HashSet<Implementation>();
		for (Implementation impl : implems) {
			if (impl.getSpec() == spec)
				impls.add(impl);
		}
		return impls;
	}

	public void add(Implementation implementation) {
		assert implementation != null && !implems.contains(implementation);
		implems.add(implementation);
	}

	public void remove(Implementation implementation) {
		assert implementation != null && implems.contains(implementation);
		implems.remove(implementation);
	}

}
