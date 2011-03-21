package fr.imag.adele.appam.dependency;

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler;
import org.apache.felix.ipojo.metadata.Element;

public class DependencyManager extends PrimitiveHandler {


	public void configure(Element metadata, Dictionary configuration)
			throws ConfigurationException {
	}

	public void start() {
 	}

	public void stop() {
	}

	public String toString() {
		return "APPAM Dependency manager for "+getInstanceManager().getInstanceName();
	}
	
}
