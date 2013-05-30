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

import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.impl.BaseApformComponent;

/**
 * This class represents an OSGi service interface as an APAM instance
 *
 *
 */
public class ApformOSGiInstance extends BaseApformComponent<Instance,InstanceDeclaration> implements ApformInstance {

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
     * An apform instance to represent a legacy component discovered in the OSGi registry
     * 
     * @param ipojoInstance
     */
    public ApformOSGiInstance(ServiceReference reference) {

        super(new InstanceDeclaration(generateImplementationName(reference), generateInstanceName(reference), null));
        for (String key : reference.getPropertyKeys()) {
            if (!Apform2Apam.isPlatformPrivateProperty(key))
                this.declaration.getProperties().put(key, reference.getProperty(key).toString());
        }
    	
    	this.reference			= reference;
    	this.bundle				= reference.getBundle();
        this.service			= bundle.getBundleContext().getService(reference);
    	
		this.providedInterfaces = new HashSet<InterfaceReference>();
		for (String interfaceName : (String[]) reference.getProperty(Constants.OBJECTCLASS)) {
			this.providedInterfaces.add(new InterfaceReference(interfaceName));
		}

        
        
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
    public Object getServiceObject() {
        return service;
    }
    
    @Override
    public void setApamComponent(Component apamInstance) {
    	super.setApamComponent(apamInstance);
        if (apamInstance == null)
        	dispose();
    }

    public void dispose() {
    	if (bundle.getBundleContext() != null)
    		bundle.getBundleContext().ungetService(reference);
    }

    @Override
    public void setProperty(String attr, String value) {
    }

    /**
     * Generate the uniques name of the implementation associated with this instance in Apam
     */
    private static ImplementationReference<?> generateImplementationName(ServiceReference reference) {
    	String bundle = reference.getBundle().getSymbolicName();
    	
    	if (bundle == null)
    		bundle = (String)reference.getBundle().getHeaders().get("Bundle-Name");
    	
    	StringBuffer interfaces = new StringBuffer();
    	boolean first = true;
    	for (String interfaceName : (String[]) reference.getProperty(Constants.OBJECTCLASS)) {
			interfaces.append(first? "" : "+").append(interfaceName);
			first = false;
		}
    	
    	return new ApformOSGiImplementation.Reference(interfaces+"[provider.bundle="+bundle+"]");
    }

    /**
     * Generate the name of the instance in APAM
     */
    private static String generateInstanceName(ServiceReference reference) {
    	return generateImplementationName(reference).getName()+"-"+reference.getProperty(Constants.SERVICE_ID);
    }

}
