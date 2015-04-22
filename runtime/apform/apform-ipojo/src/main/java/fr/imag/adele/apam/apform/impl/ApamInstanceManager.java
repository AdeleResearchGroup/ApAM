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
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.MethodInterceptor;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandlerDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandlerDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.RelationDefinition;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apform.impl.handlers.PropertyInjectionHandler;
import fr.imag.adele.apam.apform.impl.handlers.RelationInjectionManager;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.instrumentation.CallbackDeclaration;
import fr.imag.adele.apam.declarations.references.components.VersionedReference;
import fr.imag.adele.apam.impl.BaseApformComponent;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.impl.InstanceImpl;

public class ApamInstanceManager extends InstanceManager implements	RelationInjectionManager.Resolver, MethodInterceptor {

	/**
	 * The property used to configure this instance with its declaration
	 */
	public final static String ATT_DECLARATION = "declaration";

	/**
	 * Whether this instance was created directly using the APAM API
	 */
	private final boolean isApamCreated;

	/**
	 * The corresponding APAM declaration
	 */
	private InstanceDeclaration declaration;

	/**
	 * The associated Apform component
	 */
	protected Apform apform;

	public ApamInstanceManager(ApamImplementationFactory implementation, boolean isApamCreated,
				BundleContext context, HandlerManager[] handlers) {

		super(implementation, context, handlers);

		this.isApamCreated = isApamCreated;
	}

	@Override
	public ApamImplementationFactory getFactory() {
		return (ApamImplementationFactory) super.getFactory();
	}

	@Override
	public Object getPojoObject() {
		
		if (getFactory().hasInstrumentedCode())
			return super.getPojoObject();

		/*
		 * abstract APAM instances (for example composites) have no associated service object
		 */
		return null;
	}

	public ApamInstanceManager.Apform getApform() {
		return apform;
	}

	public Instance getApamComponent() {
		return getApform().getApamComponent();
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {

		String instanceName = (String) configuration.get("instance.name");

		declaration = (InstanceDeclaration) configuration.get(ApamInstanceManager.ATT_DECLARATION);
		if (isApamCreated || (declaration == null)) {
			
			declaration = new InstanceDeclaration(VersionedReference.any(getFactory().getApform().getDeclaration().getReference()), instanceName);
			
			for (Enumeration<String> properties = configuration.keys(); properties.hasMoreElements();) {
				String property = properties.nextElement();
				declaration.getProperties().put(property, configuration.get(property).toString());
			}
		}

		/*
		 * Create the associated Apform component
		 * 
		 * IMPORTANT This must be done before invoking super.configure() because APAM handler configuration
		 * requires that the Apform be initialized.
		 * 
		 * TODO refactor responsibilities between the Apform and the instance manager to avoid such fragile
		 * ordering problems
		 */
		apform = this.new Apform();

		/*
		 * InstanceManager.configure assumes that there is instrumented code manipulation data, so we cannot
		 * reuse this method for APAM abstract components. This is one of the drawbacks of trying to reuse
		 * InstanceManager for all kind of APAM components.
		 */

		if (getFactory().hasInstrumentedCode()) {
			configuration.put("instance.name", declaration.getName());
			super.configure(metadata, configuration);
		} else {
			// Add the name
			m_name = (String) configuration.get("instance.name");

			// Create the standard handlers and add these handlers to the list
			for (int i = 0; i < m_handlers.length; i++) {
				m_handlers[i].init(this, metadata, configuration);
			}
		}

		/*
		 * TODO Currently there is no life-cycle handler, so we perform
		 * configuration directly in the instance manager. Consider implementing
		 * life-cycle management as a handler..
		 */
		loadLifeCycleCallbacks();
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
					declaration.getProperties().put(configurationProperty.getName(),configurationProperty.getValue());
				}
			}

			if (provides != null) {
				for (ProvidedServiceDescription providedServiceDescription : provides.getProvidedServices()) {
					for (Object key : providedServiceDescription.getProperties().keySet()) {
						declaration.getProperties().put((String) key,providedServiceDescription.getProperties().get(key).toString());
					}
				}
			}
		}

		/*
		 * For instances that are not created using the Apam API, register instance with APAM on validation
		 */
		if (state == ComponentInstance.VALID)
			getApform().validated();

		if (state == ComponentInstance.INVALID)
			getApform().invalidated();

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
		 * Find the relation to resolve and trigger resolution at the level
		 * specified in the source kind
		 */

		RelationDefinition relDef = getApform().getApamComponent().getRelation(injection.getRelation().getIdentifier());

		Component source = null;
		switch (relDef.getSourceKind()) {
		case INSTANCE:
		case COMPONENT:
			source = getApform().getApamComponent();
			break;
		case IMPLEMENTATION:
			source = getApform().getApamComponent().getImpl();
			break;
		case SPECIFICATION:
			source = getApform().getApamComponent().getSpec();
			break;
		}

		return CST.apamResolver.resolveLink(source, relDef) != null;

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
			// System.err.println("unresolve failure for client " +
			// getInstanceName() + " : ASM instance unkown");
			return false;
		}

		Apam apam = getFactory().getApam();
		if (apam == null) {
			System.err.println("unresolve failure for client " + getInstanceName() + " : APAM not found");
			return false;
		}

		RelationDeclaration relation = injection.getRelation();
		for (Link outgoing : ((InstanceImpl)getApform().getApamComponent()).getExistingLinks(relation.getIdentifier())) {
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
	 * Loads the definition of the life-cycle callbacks associated with this
	 * instance
	 */
	private void loadLifeCycleCallbacks() throws ConfigurationException {

		if (!(getFactory().getDeclaration() instanceof AtomicImplementationDeclaration))
			return;

		AtomicImplementationDeclaration implementation = (AtomicImplementationDeclaration) getFactory().getDeclaration();

		/*
		 * Add automatically a life-cycle callback in case the class implements directly the APAM component interface
		 */
		Class<?> pojoClass = this.getClazz();
		if (ApamComponent.class.isAssignableFrom(pojoClass)) {
			implementation.addCallback(AtomicImplementationDeclaration.Event.INIT, 	 new CallbackDeclaration(implementation, "apamInit"));
			implementation.addCallback(AtomicImplementationDeclaration.Event.REMOVE, new CallbackDeclaration(implementation, "apamRemove"));
		}
		
		/*
		 * register callbacks
		 */
		for (AtomicImplementationDeclaration.Event trigger : AtomicImplementationDeclaration.Event.values()) {

			Set<CallbackDeclaration> callbacks = implementation.getCallback(trigger);

			if (callbacks == null)
				continue;

			for (CallbackDeclaration callbackDeclaration : callbacks) {
				
				LifecycleCallback callback = new LifecycleCallback(this.apform, trigger, callbackDeclaration);
				addCallback(callback);
				
				/*
				 * track remove callback execution
				 */
				if (trigger == AtomicImplementationDeclaration.Event.REMOVE) {
					register(callback.getMethodMetadata(),this);
				}
			}

		}
	}


	/**
	 * Stop the component when the remove callback is explicitly invoked by code
	 */
	@Override
	public void onFinally(Object pojo, Member method) {
		
		if (getApform().getApamComponent() != null) {
			
			/*
			 * avoid re-executing the invoked callback
			 */
			LifecycleCallback triggering = null;
			for (LifecycleCallback callback : this.apform.lifeCycleCallbacks) {
				if (callback.invokes((Method)method))
					triggering = callback;
			}
			
			if (triggering != null)
				removeCallback(triggering);
			
			/*
			 * stop the instance
			 */
			stop();
		}
	}

	@Override
	public void onEntry(Object pojo, Member method, Object[] args) {
	}

	@Override
	public void onExit(Object pojo, Member method, Object returnedObj) {
	}

	@Override
	public void onError(Object pojo, Member method, Throwable throwable) {
	}

	/**
	 * Adds a new life-cycle change callback
	 */
	public void addCallback(LifecycleCallback callback)	throws ConfigurationException {
		apform.lifeCycleCallbacks.add(callback);
	}

	private void removeCallback(LifecycleCallback callback)	{
		apform.lifeCycleCallbacks.remove(callback);
	}

	/**
	 * Adds a new property change callback
	 */
	public void addCallback(PropertyCallback callback) throws ConfigurationException {
		apform.propertyCallbacks.add(callback);
	}

	/**
	 * Adds a new relation life-cycle callback
	 */
	public void addCallback(RelationCallback callback) throws ConfigurationException {
		apform.relationCallbacks.add(callback);
	}

	/**
	 * This class represents the mediator between an APAM component and this iPOJO instance manager.
	 * 
	 * It keeps synchronized the APAM architectural layer (in terms of component instances and links) and the execution layer
	 * (in terms of iPOJO managers, java service provider objects and references).
	 * 
	 * Modifications at the architecture layer are synchronously propagated into the execution, by the injection performed by
	 * the different handlers associated to the iPOJO manager.
	 * 
	 * Actions performed at the execution layer (for example, referencing an injected filed) are propagated synchronously to the 
	 * architecture layer (for example, to perform relation resolution), and may block application threads.
	 * 
	 * Modifications performed at the OSGi or iPOJO layer without passing by the APAM layer (for example, component invalidation,
	 * component reconfiguration, or bundle stopping) are asynchronously reflected into the architecture layer, trying to avoid 
	 * blocking platform threads. 
	 * 
	 * APAM components are mapped to valid iPOJO components. When an iPOJO component becomes invalid, its corresponding component
	 * in APAM is destroyed. However, because this mapping is done asynchronously, we need to carefully consider some intermediate
	 * states in the code. 
	 * 
	 * NOTE TODO currently when the APAM component is destroyed its corresponding IPOJO component is disposed. Then an iPOJO component
	 * that becomes invalid will be disposed as a side-effect. This is a result of the mismatch between the life-cycle of iPOJO and
	 * APAM, that makes difficult to reuse iPOJO handlers directly on APAM components. We should review the integration of APAM
	 * with iPOJO. 
	 * 
	 */
	public class Apform extends	BaseApformComponent<Instance, InstanceDeclaration> implements ApformInstance {

		/**
		 * The list of injected fields handled by this instance
		 */
		private final Set<RelationInjectionManager> injectedFields;

		/**
		 * The list of callbacks to notify when onInit, onRemove
		 */
		private final Set<LifecycleCallback> lifeCycleCallbacks;

		/**
		 * The list of callbacks to notify when a property is set
		 */
		private final Set<PropertyCallback> propertyCallbacks;

		/**
		 * The list of callbacks to notify when bind, Unbind
		 */
		private final Set<RelationCallback> relationCallbacks;

		private Apform() throws ConfigurationException {

			super(ApamInstanceManager.this.declaration);

			injectedFields = new HashSet<RelationInjectionManager>();
			lifeCycleCallbacks = new HashSet<LifecycleCallback>();
			propertyCallbacks = new HashSet<PropertyCallback>();
			relationCallbacks = new HashSet<RelationCallback>();

		}

		/**
		 * Get the associated iPOJO component
		 */
		public ApamInstanceManager getManager() {
			return ApamInstanceManager.this;
		} 

		/**
		 * Updates the APAM layer when the corresponding IPOJO component is validated
		 */
		private void validated() {
			/*
			 * For instances that are not created using the Apam API, register instance with APAM on validation
			 */
			if (! ApamInstanceManager.this.isApamCreated)
				Apform2Apam.newInstance(this);
			
		}
		
		/**
		 * The service object associated to the component last time that it was valid. 
		 */
		private Object lastValidServiceObject;
		
		/**
		 * Updates the APAM layer when the corresponding IPOJO component is invalidated
		 * 
		 * NOTE notice that we keep track of the last valid service object on invalidation, rather than validation,
		 * because this avoids unnecessarily object instantiation.
		 */
		private void invalidated() {
			Object[] pojoObjects	= ApamInstanceManager.this.getPojoObjects();
			lastValidServiceObject 	= pojoObjects != null && pojoObjects.length > 0 ? pojoObjects[0] : null ; 
			((ComponentBrokerImpl)CST.componentBroker).disappearedComponent(getDeclaration().getName());
		}


		@Override
		public Object getServiceObject() {
			
			/*
			 * For APAM instances whose corresponding iPOJO component is invalid we need to use the cached service object.
			 * 
			 * This can only happen because the APAM instance is asynchronously synchronized with the iPOJO instance, and
			 * so an APAM component can still used by other components or callbacks, even if the iPOJO instance is no longer
			 * valid  
			 * 
			 */
			if (getManager().getState() != ComponentInstance.VALID)
				return lastValidServiceObject;
			
			return ApamInstanceManager.this.getPojoObject();
		}

		@Override
		public void setApamComponent(Component apamComponent) throws InvalidConfiguration {

			if (this.apamComponent == apamComponent)
				return;
			
			Instance previousComponent = this.apamComponent;
			super.setApamComponent(apamComponent);


			PropertyInjectionHandler handler = (PropertyInjectionHandler) ApamInstanceManager.this.getHandler(
													ApamComponentFactory.APAM_NAMESPACE + ":"+ PropertyInjectionHandler.NAME);

			if (apamComponent != null) { // starting the instance

				if (handler != null)
					handler.setApamComponent(apamComponent);

				fireCallbacks(AtomicImplementationDeclaration.Event.INIT,this.apamComponent,false);
				return;
			}

			if (apamComponent == null) { // stopping the instance

				fireCallbacks(AtomicImplementationDeclaration.Event.REMOVE,previousComponent,true);

				/*
				 * dispose this instance
				 */
				if (handler != null)
					handler.setApamComponent(null);

				ApamInstanceManager.this.dispose();

				/*
				 * avoid keeping a reference to the service object
				 */
				lastValidServiceObject = null;
				
				return;
			}

		}

		@Override
		public boolean checkLink(Component destination, String depName) {

			/*
			 * Validate all the instrumentations can be performed
			 */

			for (RelationInjectionManager injectedField : injectedFields) {
				if (!injectedField.isValid())
					return false;
			}

			return true;
		}

		@Override
		public boolean setLink(Component destination, String depName) {

			/*
			 * perform injection update
			 */
			for (RelationInjectionManager injectedField : injectedFields) {
				if (injectedField.getRelation().getIdentifier().equals(depName)) {
					injectedField.addTarget(destination);
				}
			}

			fireCallbacks(RelationDeclaration.Event.BIND, depName, destination);
			return true;
		}

		@Override
		public boolean remLink(Component destination, String depName) {


			/*
			 * perform injection update
			 */
			for (RelationInjectionManager injectedField : injectedFields) {
				if (injectedField.getRelation().getIdentifier().equals(depName)) {
					injectedField.removeTarget(destination);
				}
			}

			fireCallbacks(RelationDeclaration.Event.UNBIND, depName,destination);
			return true;
		}

		@Override
		public void setProperty(String attr, String value) {

			Object pojo = getPojoObject();
			if (pojo == null)
				return;

			fireCallbacks(attr, super.getApamComponent().getPropertyObject(attr));
		}

		
		private void fireCallbacks(AtomicImplementationDeclaration.Event event, Instance component, boolean ignoreException) throws InvalidConfiguration {
			
			if (getServiceObject() == null)
				return;
			
			for (LifecycleCallback callback : lifeCycleCallbacks) {
				try {
					if (callback.isTriggeredBy(event))
						callback.invoke(component);
				} catch (Throwable exception) {

					if (exception instanceof InvocationTargetException) {
						exception = exception.getCause();
					}
					
					getLogger().log(Logger.ERROR,"error invoking lifecycle callback "+ callback.getMethod(), exception);
					if (! ignoreException)
						throw new InvalidConfiguration(exception);
				}
			}
			

		}

		private void fireCallbacks(String attr, Object value)   {

			if (getServiceObject() == null)
				return;
			
			for (PropertyCallback callback : propertyCallbacks) {
				try {
					if (callback.isTriggeredBy(attr))
						callback.invoke(value);
				} catch (Exception ignored) {
					getLogger().log(Logger.ERROR,"error invoking callback " + callback.getMethod() + " for property " + attr, ignored);
				}
			}
		}

		private void fireCallbacks(RelationDeclaration.Event event,	String relationName, Component target)  {

			if (getServiceObject() == null)
				return;

			for (RelationCallback callback : relationCallbacks) {
				try {
					if (callback.isTriggeredBy(relationName, event))
						callback.invoke(target);
				} catch (Exception ignored) {
					getLogger().log(Logger.ERROR,"error invoking relation callback " + callback.getMethod() + " for relation" + relationName, ignored);
				}
			}
		}

	}

}