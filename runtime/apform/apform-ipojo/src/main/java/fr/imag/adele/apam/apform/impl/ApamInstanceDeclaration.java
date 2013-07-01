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
package fr.imag.adele.apam.apform.impl;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.declarations.InstanceDeclaration;

/**
 * This class is used to represent an instance declaration
 * 
 * TODO Technically this class should not really be an iPOJO component factory. This is just an
 * implementation hack to reuse the iPOJO extender, as a base to handle statically defined APAM
 * instances.
 * 
 * @author vega
 *
 */
public class ApamInstanceDeclaration extends ApamComponentFactory {

    /**
     * A dynamic reference to the apform implementation
     */
    protected final ImplementationTracker implementationTracker;

    /**
     * The ipojo instance corresponding to this declaration
     */
    private ComponentInstance iPojoInstance;


    /**
     * Creates a new declaration
     */
    public ApamInstanceDeclaration(BundleContext context, Element element) throws ConfigurationException {
        super(context, element);
        try {
            String classFilter		= "(" + Constants.OBJECTCLASS + "=" + Factory.class.getName() + ")";
            String factoryFilter	= "(" + "factory.name" + "=" + getDeclaration().getImplementation().getName() + ")";
            String filter			= "(& "+classFilter+factoryFilter+")";

            implementationTracker = new ImplementationTracker(context, context.createFilter(filter));
        } catch (InvalidSyntaxException e) {
            throw new ConfigurationException(e.getLocalizedMessage());
        }
    }

    public InstanceDeclaration getDeclaration() {
		return (InstanceDeclaration) super.getDeclaration();
	}

	@Override
	protected boolean hasInstrumentedCode() {
        return false;
    }

    @Override
    protected boolean isInstantiable() {
        return false;
    }

	@Override
	protected ApformComponent createApform() {
		return null;
	}
    
   @Override
    protected void bindToApam(Apam apam) {
        implementationTracker.open();
    }

    @Override
    protected void unbindFromApam(Apam apam) {
        implementationTracker.close();
    }
	
    /**
     * Gets the class name.
     *
     * @return the class name.
     * @see org.apache.felix.ipojo.IPojoFactory#getClassName()
     */
    @Override
    public String getClassName() {
        return getDeclaration().getName();
    }

    @Override
    public ApamInstanceManager createApamInstance(IPojoContext context, HandlerManager[] handlers) {
        throw new UnsupportedOperationException("APAM instance declaration is not instantiable");
    }

     /**
     * A class to dynamically track the apform implementation. This allows to dynamically create the instance
     * represented by this declaration
     *
     */
    class ImplementationTracker extends ServiceTracker {

        public ImplementationTracker(BundleContext context, Filter filter) {
            super(context,filter,null);
        }

        @Override
        public Object addingService(ServiceReference reference) {

            if (iPojoInstance != null)
                return null;

            try {

                Factory factory 			= (Factory) this.context.getService(reference);
                Properties configuration	= new Properties();
                configuration.put(ApamInstanceManager.ATT_DECLARATION, ApamInstanceDeclaration.this.getDeclaration());
                iPojoInstance = factory.createComponentInstance(configuration);

                return factory;

            } catch (Exception instantiationError) {
                instantiationError.printStackTrace(System.err);
                return null;
            }

        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            if (iPojoInstance != null)
                iPojoInstance.dispose();

            this.context.ungetService(reference);
            iPojoInstance = null;
        }

    }

}