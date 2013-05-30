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

import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandlerDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandlerDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Callback;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.Relation;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.impl.handlers.RelationInjectionManager;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.CallbackMethod;
import fr.imag.adele.apam.declarations.CallbackMethod.CallbackTrigger;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.impl.BaseApformComponent;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;

public class ApamInstanceManager extends InstanceManager implements RelationInjectionManager.Resolver {

    /**
     * The property used to configure this instance with its declaration
     */
    public final static String                       		ATT_DECLARATION = "declaration";

    /**
     * Whether this instance was created directly using the APAM API
     */
    private final boolean									isApamCreated;

    /**
     * The corresponding APAM declaration
     */
    private InstanceDeclaration								declaration;
    
	/**
	 * The associated Apform component
	 */
	protected Apform 									apform;
	

    public ApamInstanceManager(ApamImplementationFactory implementation, boolean isApamCreated, BundleContext context,
                               HandlerManager[] handlers) {

        super(implementation, context, handlers);

        this.isApamCreated 	= isApamCreated;
    }

    @Override
    public ApamImplementationFactory getFactory() {
        return (ApamImplementationFactory) super.getFactory();
    }

    @Override
    public Object getPojoObject() {
        if (getFactory().hasInstrumentedCode())
            return super.getPojoObject();

        return null;
    }

    public ApformInstance getApform() {
    	return apform;
    }
    
    public Instance getApamComponent() {
    	return getApform().getApamComponent() ;
    }
    
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {

        String instanceName = (String) configuration.get("instance.name");
        
        declaration = (InstanceDeclaration) configuration.get(ApamInstanceManager.ATT_DECLARATION);
        if (isApamCreated || (declaration == null)) {
            declaration = new InstanceDeclaration(getFactory().getApform().getDeclaration().getReference(), instanceName, null);
            for (Enumeration<String> properties = configuration.keys(); properties.hasMoreElements();) {
                String property = properties.nextElement();
                if (!Apform2Apam.isPlatformPrivateProperty(property))
                    declaration.getProperties().put(property, configuration.get(property).toString());
            }
        }

        /*
         * Create the associated apform component
         * 
         * IMPORTANT This must be done before invoking super.configure() because APAM handler configuration
         * requires that the apform be initialized.
         * 
         * TODO refactor responsibilities between the apform and the instance manager to avoid such fragile
         * ordering problems
         */
        apform 	= this.new Apform();
        
        configuration.put("instance.name", declaration.getName());
        super.configure(metadata, configuration);
        


    }

    /**
     * Notify instance activation/deactivation
     */
    @Override
    public void setState(int state) {
        super.setState(state);

        /*
         * Copy ipojo properties to declaration on validation
         */
        if (state == ComponentInstance.VALID) {
            ConfigurationHandlerDescription configuration = (ConfigurationHandlerDescription) getInstanceDescription()
                    .getHandlerDescription("org.apache.felix.ipojo:properties");
            ProvidedServiceHandlerDescription provides = (ProvidedServiceHandlerDescription) getInstanceDescription()
                    .getHandlerDescription("org.apache.felix.ipojo:provides");

            if (configuration != null) {
                for (PropertyDescription configurationProperty : configuration.getProperties()) {
                    declaration.getProperties().put(configurationProperty.getName(),
                            configurationProperty.getValue());
                }
            }

            if (provides != null) {
                for (ProvidedServiceDescription providedServiceDescription : provides.getProvidedServices()) {
                    for (Object key : providedServiceDescription.getProperties().keySet()) {
                        declaration.getProperties().put((String) key,
                                providedServiceDescription.getProperties().get(key).toString());
                    }
                }
            }
        }

        /*
         * For instances that are not created using the Apam API, register instance with APAM on validation
         */
        if (state == ComponentInstance.VALID && !isApamCreated)
            Apform2Apam.newInstance(getApform());

        if (state == ComponentInstance.INVALID)
            // Apform2Apam.vanishInstance(getInstanceName());
            ComponentBrokerImpl.disappearedComponent(getInstanceName());
    }

    
    /**
     * This class represents the mediator between APAM and this instance manager
     */
    private class Apform extends BaseApformComponent<Instance,InstanceDeclaration> implements ApformInstance {

        /**
         * The list of injected fields handled by this instance
         */
        private final Set<RelationInjectionManager>				injectedFields;

        /**
         * The list of callbacks to notify when a property is set
         */
        private final Map<String, Callback>						propertyCallbacks;

        /**
         * The list of callbacks to notify when onInit, onRemove
         */
        private final Map<CallbackTrigger, Set<Callback>>		lifeCycleCallbacks;

        /**
         * The list of callbacks to notify when bind, Unbind
         */
    	Map<CallbackTrigger, Map<String, Set<Callback>>>		relationCallback;
    	
    	public Apform() {
			
    		super(ApamInstanceManager.this.declaration);
			
	        injectedFields 		= new HashSet<RelationInjectionManager>();
	        propertyCallbacks 	= new HashMap<String, Callback>();
			relationCallback 	= new HashMap<CallbackTrigger, Map<String, Set<Callback>>>();
	        lifeCycleCallbacks 	= new HashMap<CallbackTrigger, Set<Callback>>();
	        
	        
	        if (ApamInstanceManager.this.getFactory().hasInstrumentedCode()) {
	            AtomicImplementationDeclaration primitive = (AtomicImplementationDeclaration) ApamInstanceManager.this.getFactory().getDeclaration();
	            loadCallbacks(primitive, CallbackTrigger.onInit);
	            loadCallbacks(primitive, CallbackTrigger.onRemove);

	        }
			
		}

        @Override
        public Object getServiceObject() {
            return ApamInstanceManager.this.getPojoObject();
        }

    	@Override
    	public void setApamComponent(Component apamComponent) {
    		
    		super.setApamComponent(apamComponent);

            /*
	         * Invoke the execution platform instance callback on the pojo object
	         */
    		
            Object pojo = getServiceObject();
	        if (pojo == null)
	        	return;

	        
            if (apamComponent != null) { // starting the instance
                if (pojo instanceof ApamComponent) {
                    ApamComponent serviceComponent = (ApamComponent) pojo;
                    serviceComponent.apamInit(this.apamComponent);
                }
                // call backs methods
                fireCallbacks(CallbackTrigger.onInit, lifeCycleCallbacks);
                
                return;
            }
            
            if (apamComponent == null) {  // stopping the instance
                if (pojo instanceof ApamComponent) {
                    ApamComponent serviceComponent = (ApamComponent) pojo;
                    serviceComponent.apamRemove();
                }
                // call back methods
                fireCallbacks(CallbackTrigger.onRemove,lifeCycleCallbacks);
            	
            } 
    	}
    	
        @Override
        public boolean setLink(Component destination, String depName) {
            // System.err.println("Native instance set wire " + depName + " :" + getInstanceName() + "->" + destInst);

            /*
             * Validate all the injections can be performed
             */

            for (RelationInjectionManager injectedField : injectedFields) {
                if (!injectedField.isValid())
                    return false;
            }

            /*
             * perform injection update
             */
            for (RelationInjectionManager injectedField : injectedFields) {
                if (injectedField.getRelationInjection().getRelation().getIdentifier().equals(depName)) {
                    injectedField.addTarget(destination);
                }
            }

            /*
             * perform callback bind
             */
    		fireCallbacks(destination, depName,	relationCallback.get(CallbackTrigger.Bind));

            return true;
        }

        @Override
        public boolean remLink(Component destination, String depName) {
            // System.err.println("Native instance rem wire " + depName + " :" + getInstanceName() + "->" + destInst);

            /*
             * Validate all the injections can be performed
             */

            for (RelationInjectionManager injectedField : injectedFields) {
                if (!injectedField.isValid())
                    return false;
            }

            /*
             * perform injection update
             */
            for (RelationInjectionManager injectedField : injectedFields) {
                if (injectedField.getRelationInjection().getRelation().getIdentifier().equals(depName)) {
                    injectedField.removeTarget(destination);
                }
            }

            /*
             * perform callback unbind
             */
    		fireCallbacks(destination, depName, relationCallback.get(CallbackTrigger.Unbind));

            return true;
        }
        
        
        @Override
        public void setProperty(String attr, String value) {

            Object pojo = getPojoObject();
            Callback callback = propertyCallbacks.get(attr);

            if (pojo == null || callback == null)
                return;

            try {
                callback.call(pojo, new Object[] { value });
            } catch (Exception ignored) {
                getLogger().log(Logger.ERROR, "error invoking callback " + callback.getMethod() + " for property " + attr,
                        ignored);
            }
        }
        
    }
    

    
    /**
	 * Delegate APAM to resolve a given injection.
	 * 
	 * NOTE nothing is returned from this method, the call to APAM has as
	 * side-effect the update of the relation.
	 * 
	 * @param injection
	 */
    @Override
    public boolean resolve(RelationInjectionManager injection) {

        /*
         * This instance is not actually yet managed by APAM
         */
        if (getApform().getApamComponent() == null) {
            System.err.println("resolve failure for client " + getInstanceName() + " : APAM instance unkown");
            return false;
        }

        Apam apam = getFactory().getApam();
        if (apam == null) {
            System.err.println("resolve failure for client " + getInstanceName() + " : APAM not found");
            return false;
        }

		/*
		 * Find the relation to resolve and trigger resolution at the level specified in the source kind
		 */
		
		Relation relation = getApform().getApamComponent().getRelation(injection.getRelationInjection().getRelation().getIdentifier());
		
		Component source = null;
		switch (relation.getSourceKind()) {
			case INSTANCE:
				source = getApform().getApamComponent();
				break;
			case IMPLEMENTATION:
				source	= getApform().getApamComponent().getImpl();
				break;
			case SPECIFICATION:
				source	= getApform().getApamComponent().getSpec();
				break;
		}
		
		return CST.apamResolver.resolveLink(source, relation) != null;

    }

    /**
	 * Delegate APAM to remove the currently resolved relation and force a new
	 * resolution the next time the injected relation is accessed
	 * 
	 */
    @Override
    public boolean unresolve(RelationInjectionManager injection) {

        /*
         * This instance is not actually yet managed by APAM
         */
        if (getApform().getApamComponent() == null) {
          //  System.err.println("unresolve failure for client " + getInstanceName() + " : ASM instance unkown");
            return false;
        }

        Apam apam = getFactory().getApam();
        if (apam == null) {
            System.err.println("unresolve failure for client " + getInstanceName() + " : APAM not found");
            return false;
        }

		RelationDeclaration relation = injection.getRelationInjection().getRelation();
		for (Link outgoing : getApform().getApamComponent().getLinks(relation.getIdentifier())) {
            outgoing.remove();
        }

        return true;
    }

    /**
     * Adds a new injected field to this instance
     */
    @Override
	public void addInjection(RelationInjectionManager relation) {
		apform.injectedFields.add(relation);
    }

    /**
     * Get the list of injected fields
     */
    public Set<RelationInjectionManager> getInjections() {
        return apform.injectedFields;
    }

    /**
     * Adds a new callback to a property
     */
    public void addCallback(String property, Callback callback) {
        apform.propertyCallbacks.put(property, callback);
    }

    public void addCallbackRelation(CallbackTrigger trigger, Map<String, Set<Callback>> callbackDependecy) {
		apform.relationCallback.put(trigger, callbackDependecy);
    }


    private void fireCallbacks(Component destInstance, String depName, Map<String, Set<Callback>> map) {
        Set<Callback> callbacks = map.get(depName);
        performCallbacks(destInstance, callbacks);
    }

    private void loadCallbacks(AtomicImplementationDeclaration primitive, CallbackTrigger trigger) {
        Set<CallbackMethod> callbackMethods = primitive.getCallback(trigger);
        if (callbackMethods != null) {
            for (CallbackMethod callbackMethod : callbackMethods) {
                Set<MethodMetadata> metadatas;
                try {
                    metadatas = (Set<MethodMetadata>) primitive.getInstrumentation().getCallbacks(
                            callbackMethod.getMethodName(), false);
                    for (MethodMetadata methodMetadata : metadatas) {
                        if (apform.lifeCycleCallbacks.get(trigger) == null) {
                            apform.lifeCycleCallbacks.put(trigger, new HashSet<Callback>());
                        }
                        apform.lifeCycleCallbacks.get(trigger).add(new Callback(methodMetadata, this));
                    }
                } catch (NoSuchMethodException e) {
                    System.err.println("life cycle failure, when trigger : " + trigger + " " + e.getMessage());
                }
            }
        }
    }


    private void fireCallbacks(CallbackTrigger trigger, Map<CallbackTrigger, Set<Callback>> mapCallbacks) {
        Set<Callback> callbacks = mapCallbacks.get(trigger);
        performCallbacks(getApform().getApamComponent(), callbacks);
    }

    private void performCallbacks(Component inst, Set<Callback> callbacks) {
        if (callbacks != null) {
            for (Callback callback : callbacks) {
                if (callback.getArguments().length == 1) {
                    try {
                        callback.call(new Object[] { inst });
                    } catch (NoSuchMethodException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else if (callback.getArguments().length == 0) {
                    try {
                        callback.call();
                    } catch (NoSuchMethodException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

    }
    
}