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
package fr.imag.adele.apam.apform.legacy.ipojo;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.InstanceManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.impl.BaseApformComponent;

public class ApformIpojoInstance extends BaseApformComponent<Instance,InstanceDeclaration> implements ApformInstance {

    /**
     * The iPojo instance represented by this proxy
     */
    private final ComponentInstance   ipojoInstance;

    /**
     * An apform instance to represent a legacy component created using the APAM API
     * 
     * @param ipojoInstance
     */
    public ApformIpojoInstance(ComponentInstance ipojoInstance) {
    	super( new InstanceDeclaration(
    					new ApformIPojoImplementation.Reference(ipojoInstance.getFactory().getName()),
    					ipojoInstance.getInstanceName(),
    					null)
    	);

    	this.ipojoInstance = ipojoInstance;
    }

    /**
     * An apform instance to represent a legacy component discovered in the OSGi registry
     * 
     * @param ipojoInstance
     */
    public ApformIpojoInstance(ComponentInstance ipojoInstance, ServiceReference reference) {
    	
        this(ipojoInstance);

        /*
         * Propagate OSGI registry properties to APAM
         */
        for (String key : reference.getPropertyKeys()) {
            if (!Apform2Apam.isPlatformPrivateProperty(key))
                this.declaration.getProperties().put(key, reference.getProperty(key).toString());
        }

    }

    @Override
    public Bundle getBundle() {
    	return ipojoInstance.getContext().getBundle();
    }
    
    /**
     * Apform: get the service object of the instance
     */
    @Override
    public Object getServiceObject() {
        return ((InstanceManager) ipojoInstance).getPojoObject();
    }

    @Override
    public void setProperty(String attr, String value) {
    	Properties configuration = new Properties();
    	configuration.put(attr,value);
    	ipojoInstance.reconfigure(configuration);
    }
    
}
