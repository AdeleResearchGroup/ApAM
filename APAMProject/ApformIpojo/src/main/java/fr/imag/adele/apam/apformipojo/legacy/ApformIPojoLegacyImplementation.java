package fr.imag.adele.apam.apformipojo.legacy;

import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.IPojoFactory;

import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.ApformSpecification;

/**
 * This class allow integrating legacy iPojo components in the APAM runtime

 *
 */
public class ApformIPojoLegacyImplementation implements ApformImplementation {


	/**
	 * The associated iPojo factory
	 */
	private final IPojoFactory factory;
	
	public ApformIPojoLegacyImplementation(IPojoFactory factory) {
		this.factory = factory;
	}
	
	@Override
	public String getName() {
		return factory.getName();
	}

	@Override
	public String[] getInterfaceNames() {
		return factory.getComponentDescription().getprovidedServiceSpecification();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> getProperties() {
		return (Map<String, Object> )factory.getComponentDescription().getPropertiesToPublish();
	}

	@Override
	public Object getProperty(String key) {
		return factory.getComponentDescription().getPropertiesToPublish().get(key);
	}

	@Override
	public ApformInstance createInstance(Map<String, Object> initialproperties) {
		
		try {
			Properties configuration = new Properties();
			if (initialproperties != null)
				configuration.putAll(initialproperties);
			
			ComponentInstance ipojoInstance = factory.createComponentInstance(configuration);
			return new ApformIpojoLegacyInstance(ipojoInstance);
			
		} catch (Exception cause) {
			throw new IllegalArgumentException(cause);
		}
	}

	@Override
	public ApformSpecification getSpecification() {
		return null;
	}

}
