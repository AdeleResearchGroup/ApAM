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

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;

public class ApformIpojoInstance implements ApformInstance {

    /**
     * The iPojo instance represented by this proxy
     */
    private final ComponentInstance   ipojoInstance;

    /**
     * the corresponding APAM declaration
     */
    private final InstanceDeclaration declaration;

    /**
     * The associated APAM instance
     */
    private Instance                  apamInstance;

    /**
     * An apform instance to represent a legacy component created using the APAM API
     * 
     * @param ipojoInstance
     */
    public ApformIpojoInstance(ComponentInstance ipojoInstance) {
        this.ipojoInstance = ipojoInstance;
        ImplementationReference<?> implementation = new ApformIPojoImplementation.Reference(ipojoInstance
                .getFactory().getName());
        this.declaration = new InstanceDeclaration(implementation, ipojoInstance.getInstanceName(), null);
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
    
    @Override
    public void setInst(Instance apamInstance) {
        this.apamInstance = apamInstance;
    }

    @Override
    public Instance getInst() {
    	return this.apamInstance;
    }
    
    /**
     * Apform: get the service object of the instance
     */
    @Override
    public Object getServiceObject() {
        return ((InstanceManager) ipojoInstance).getPojoObject();
    }

    /**
     * Legacy implementations can not be injected with APAM dependencies, so they do not provide
     * injection information
     */
    @Override
    public boolean setLink(Component destInst, String depName) {
        return false;
    }

    /**
     * Legacy implementations can not be injected with APAM dependencies, so they do not provide
     * injection information
     */
    @Override
    public boolean remLink(Component destInst, String depName) {
        return false;
    }

    /**
     * Legacy implementations can not be injected with APAM dependencies, so they do not provide
     * injection information
     */
//    @Override
//    public boolean substWire(Instance oldDestInst, Instance newDestInst, String depName) {
//        return false;
//    }

    @Override
    public InstanceDeclaration getDeclaration() {
        return declaration;
    }

    @Override
    public void setProperty(String attr, String value) {
    	Properties configuration = new Properties();
    	configuration.put(attr,value);
    	ipojoInstance.reconfigure(configuration);
    }


    
}
