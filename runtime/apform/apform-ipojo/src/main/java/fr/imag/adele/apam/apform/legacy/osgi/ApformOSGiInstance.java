/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.apform.legacy.osgi;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.references.components.ImplementationReference;
import fr.imag.adele.apam.declarations.references.components.Versioned;
import fr.imag.adele.apam.impl.BaseApformComponent;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;

/**
 * This class represents an OSGi service interface reified as an APAM instance.
 * 
 * The same OSGi service can be reified as an instance of different specifications (if
 * it matches the interfaces and properties defined in the specification) so several 
 * ApformOSGiInstances can in fact point to the same service reference.
 *
 *
 */
public class ApformOSGiInstance extends BaseApformComponent<Instance,InstanceDeclaration> implements ApformInstance {

	/**
	 * The APAM specification implemented by this service
	 */
	private final Specification 		specification;
	
    /**
     * The osgi service reference represented by this object
     */
    private final ServiceReference		reference;
    
	/**
	 * The bundle  that registered the instance
	 */
	private final Bundle				bundle;

	/**
	 * The bundle context that registered the instance
	 */
	private final BundleContext			bundleContext;

    /**
     * The cached service object
     */
    private  Object						service;

	/**
     * An apform instance to represent a legacy component discovered in the OSGi registry
     * 
     * @param ipojoInstance
     */
    public ApformOSGiInstance(Specification specification, ServiceReference reference) {

        super(new InstanceDeclaration(Versioned.any(generateImplementationName(specification,reference)), generateInstanceName(specification,reference), null));
        
        this.specification		= specification;
        
        for (PropertyDefinition property : specification.getDeclaration().getPropertyDefinitions()) {
        	Object value = reference.getProperty(property.getName());

        	if (value != null)
        		this.declaration.getProperties().put(property.getName(), value.toString());

        }
    	
    	this.reference			= reference;
    	this.bundle				= reference.getBundle();
    	this.bundleContext		= AccessController.doPrivileged(new PrivilegedAction<BundleContext>() {
										public BundleContext run() { return bundle.getBundleContext();}
								  });
    	
        this.service			= null;
    	
    }

    /**
     * The underlying OSGi service reference
     */
    public ServiceReference getServiceReference() {
    	return reference;
    }

    /**
     * The APAM specification implemented
     */
    public Specification getSpecification() {
    	return specification;
    }
    
    @Override
    public Bundle getBundle() {
    	return bundle;
    }
    
    
    @Override
    public Object getServiceObject() {
    	
    	/*
    	 * return cached value if available
    	 */
    	
    	if (service != null)
    		return service;
    	
    	/*
    	 * Get the service reference, this may activate delayed component in the case of
    	 * declarative services
    	 */
    	synchronized (this) {
    		service = bundleContext.getService(reference);
		}
    	
        return service;
    }
    
    @Override
    public void setApamComponent(Component apamInstance) throws InvalidConfiguration {
    	super.setApamComponent(apamInstance);
        if (apamInstance == null)
        	dispose();
    }

    public void dispose() {
    	service = null;
    	
    	if (bundleContext != null)
    		bundleContext.ungetService(reference);
    }

    @Override
    public void setProperty(String attr, String value) {
    }

    /**
     * Generate the uniques name of the implementation associated with this instance in Apam
     */
    private static ImplementationReference<?> generateImplementationName(Specification specification, ServiceReference reference) {
    	String bundle = reference.getBundle().getSymbolicName();
    	
    	if (bundle == null)
    		bundle = (String)reference.getBundle().getHeaders().get("Bundle-Name");
    	
    	return new ApformOSGiImplementation.Reference(specification.getName()+"[provider.bundle="+bundle+"]");
    }

    /**
     * Generate the name of the instance in APAM
     */
    private static String generateInstanceName(Specification specification, ServiceReference reference) {
    	return generateImplementationName(specification,reference).getName()+"-"+reference.getProperty(Constants.SERVICE_ID);
    }

}
