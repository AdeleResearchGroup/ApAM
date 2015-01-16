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

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.extender.DeclarationBuilderService;
import org.apache.felix.ipojo.extender.DeclarationHandle;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.impl.APAMImpl;

/**
 * This class is used to represent an instance declaration
 * 
 * This automatically creates a corresponding iPOJO instance declaration, using the Declaration
 * Builder Service (available since iPOJO 1.11.2). The iPOJO instance declaration is published
 * as long as this factory remains valid.
 *  
 * TODO Technically this class should not really be an iPOJO component factory. This is just an
 * implementation hack to reuse the iPOJO extender, to process APAM metadata. In the future we
 * could use another mechanism to load APAM metadata at runtime, and create the appropriate iPOJO
 * declarations using the the Declaration Builder Service API, but we need to evaluate the impact
 * on build-time tools.
 * 
 * @author vega
 *
 */
public class ApamInstanceDeclaration extends ApamComponentFactory {

    /**
     * A dynamic reference to the iPOJO builder service
     */
    protected BuilderTracker builderTracker;

    /**
     *The APAM bundle context used to look for the iPOJO builder
     */
    protected BundleContext apamContext;

    /**
     * The iPOJO instance handle corresponding to this declaration
     */
    private DeclarationHandle iPojoInstance;


    /**
     * Creates a new declaration
     */
    public ApamInstanceDeclaration(BundleContext context, Element element) throws ConfigurationException {
        super(context, element);
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

    @Override
    public synchronized void start() {
    	
    	super.start();
    	
    	if (iPojoInstance != null) {
    		iPojoInstance.publish();
    	}
    }
    
    
    @Override
    public synchronized void stop() {
    	super.stop();

    	if (iPojoInstance != null) {
    		iPojoInstance.retract();
    	}

    }
    @Override
    protected void bindToApam(Apam apam) {
    	
    	/*
    	 * We have just bound to APAM for the first time, or it has been updated,
    	 * we force building a new instance declaration 
    	 */
    	if (this.apamContext != APAMImpl.context) {
    		
    		if (iPojoInstance != null) {
    			iPojoInstance.retract();
    		}
    		
    		if (builderTracker != null) {
    			builderTracker.close();
    		}

    		iPojoInstance 	= null;
    		apamContext 	= APAMImpl.context;
       		builderTracker	= new BuilderTracker(apamContext);
    	}

        builderTracker.open();

    }

    @Override
    protected void unbindFromApam(Apam apam) {
   		builderTracker.close();
    }

     /**
     * A class to dynamically track the iPOJO builder implementation.
     *
     */
    private class BuilderTracker extends ServiceTracker<DeclarationBuilderService,DeclarationBuilderService> {

        public BuilderTracker(BundleContext apamContext) {
        	super(apamContext,DeclarationBuilderService.class.getName(),null);
        }

        @Override
        public DeclarationBuilderService addingService(ServiceReference<DeclarationBuilderService> reference) {

            if (iPojoInstance != null)
                return null;

           	DeclarationBuilderService builder 	=  super.addingService(reference);

            /*
             * Create the iPOJo instance declaration as soon as the builder is available.
             */
           	iPojoInstance 	= builder.newInstance(getDeclaration().getImplementation().getName(),getDeclaration().getName()).
           									context(getBundleContext()).
           									configure().property(ApamInstanceManager.ATT_DECLARATION, getDeclaration()).
           									build();
           	
           	/*
           	 * Publish it, this is necessary if this event arrives after this factory has been started
           	 */
           	if (ApamInstanceDeclaration.this.getState() == Factory.VALID)
           		iPojoInstance.publish();

           	return builder;
        }


    }

}