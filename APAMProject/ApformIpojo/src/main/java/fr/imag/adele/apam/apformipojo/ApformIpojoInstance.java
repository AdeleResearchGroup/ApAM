package fr.imag.adele.apam.apformipojo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.apache.felix.ipojo.util.Callback;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apformipojo.handlers.DependencyInjectionManager;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.CallbackMethod;
import fr.imag.adele.apam.core.CallbackMethod.CallbackTrigger;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;
import fr.imag.adele.apam.impl.ComponentImpl;

public class ApformIpojoInstance extends InstanceManager implements ApformInstance, DependencyInjectionManager.Resolver {

    /**
     * The property used to configure this instance with its declaration
     */
    public final static String              ATT_DECLARATION = "declaration";

    /**
     * Whether this instance was created directly using the APAM API
     */
    private final boolean                   isApamCreated;

    /**
     * The declaration of the instance
     */
    private InstanceDeclaration             declaration;

    /**
     * The APAM instance associated to this component instance
     */
    private Instance                        apamInstance;

    /**
     * The list of injected fields handled by this instance
     */
    private Set<DependencyInjectionManager> injectedFields;

    /**
     * The list of callbacks to notify when a property is set
     */
    private Map<String, Callback>           propertyCallbacks;
    
    /**
     * The list of callbacks to notify when onInit, onRemove
     */
    private Map<CallbackTrigger, Callback>  lifeCycleCallbacks;

    public ApformIpojoInstance(ApformIpojoImplementation implementation, boolean isApamCreated, BundleContext context,
            HandlerManager[] handlers) {

        super(implementation, context, handlers);

        this.isApamCreated = isApamCreated;
        injectedFields = new HashSet<DependencyInjectionManager>();
        propertyCallbacks = new HashMap<String, Callback>();
        lifeCycleCallbacks = new HashMap<CallbackTrigger, Callback>();
    }

    @Override
    public ApformIpojoImplementation getFactory() {
        return (ApformIpojoImplementation) super.getFactory();
    }

    @Override
    public Bundle getBundle() {
        /*
         * TODO getContext should be the context of the factory, unless the instance is declared in another
         * bundle, to verify.
         * 
         * return getContext().getBundle();
         */
        return ((ComponentImpl) this.getApamInstance().getImpl()).getApformComponent().getBundle();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {

        String instanceName = (String) configuration.get("instance.name");
        declaration = (InstanceDeclaration) configuration.get(ApformIpojoInstance.ATT_DECLARATION);

        if (isApamCreated || (declaration == null)) {
            declaration = new InstanceDeclaration(getFactory().getDeclaration().getReference(), instanceName, null);
            for (Enumeration<String> properties = configuration.keys(); properties.hasMoreElements();) {
                String property = properties.nextElement();
                if (!Apform2Apam.isPlatformPrivateProperty(property))
                    declaration.getProperties().put(property, configuration.get(property).toString());
            }
        }

        configuration.put("instance.name", declaration.getName());
        super.configure(metadata, configuration);

    }

    @Override
    public Object getPojoObject() {
        if (getFactory().hasInstrumentedCode())
            return super.getPojoObject();

        return null;
    }

    @Override
    public InstanceDeclaration getDeclaration() {
        return declaration;
    }

    /**
     * Attach an APAM logical instance to this platform instance
     */
    @Override
    public void setInst(Instance apamInstance) {
        this.apamInstance = apamInstance;
        /*
        * Invoke the execution platform instance callback
        */
        if (getPojoObject() != null) {// the instance is not a composite
            if (apamInstance != null) { // starting the instance
                Object service = getServiceObject();
                if (service instanceof ApamComponent) {
                    ApamComponent serviceComponent = (ApamComponent) service;
                    serviceComponent.apamInit(getApamInstance());
                }
                // call backs methods
                fireCallbacks(CallbackTrigger.onInit, service);

            } else { // stopping the instance
                Object service = getServiceObject();
                if (service instanceof ApamComponent) {
                    ApamComponent serviceComponent = (ApamComponent) service;
                    serviceComponent.apamRemove();
                }
                // call back methods
                fireCallbacks(CallbackTrigger.onRemove, service);
            }
        }
    }

    @Override
    public Instance getInst() {
        return apamInstance;
    }

    /**
     * The attached APAM instance
     */
    public Instance getApamInstance() {
        return apamInstance;
    }

    /**
     * Adds a new injected field to this instance
     */
    @Override
    public void addInjection(DependencyInjectionManager dependency) {
        injectedFields.add(dependency);
    }

    /**
     * Get the list of injected fields
     */
    public Set<DependencyInjectionManager> getInjections() {
        return injectedFields;
    }

    /**
     * Adds a new callback to a property
     */
    public void addCallback(String property, Callback callback) {
        propertyCallbacks.put(property, callback);
    }

    /**
     * Adds a new callback lifeCycleCallback
     */
    public void addCallback(CallbackTrigger trigger, Callback callback) {
        lifeCycleCallbacks.put(trigger, callback);
    }
    
    /**
     * Delegate APAM to resolve a given injection.
     * 
     * NOTE nothing is returned from this method, the call to APAM has as
     * side-effect the update of the dependency.
     * 
     * @param dependency
     */
    @Override
    public boolean resolve(DependencyInjectionManager injection) {

        /*
         * This instance is not actually yet managed by APAM
         */
        if (apamInstance == null) {
            System.err.println("resolve failure for client " + getInstanceName() + " : ASM instance unkown");
            return false;
        }

        Apam apam = getFactory().getApam();
        if (apam == null) {
            System.err.println("resolve failure for client " + getInstanceName() + " : APAM not found");
            return false;
        }

        DependencyDeclaration dependency = injection.getDependencyInjection().getDependency();
        return CST.apamResolver.resolveWire(apamInstance, dependency.getIdentifier());

    }

	/**
	 * Delegate APAM to remove the currently resolved dependency and force a new resolution
	 * the next time the injected dependency is accessed
	 * 
	 */
	@Override
	public boolean unresolve(DependencyInjectionManager injection) {
	
		/*
		 * This instance is not actually yet managed by APAM
		 */
		if (apamInstance == null) {
			System.err.println("unresolve failure for client " + getInstanceName() + " : ASM instance unkown");
			return false;
		}
	
		Apam apam = getFactory().getApam();
		if (apam == null) {
			System.err.println("unresolve failure for client " + getInstanceName() + " : APAM not found");
			return false;
		}
	
		DependencyDeclaration dependency = injection.getDependencyInjection().getDependency();
		for (Wire outgoing : apamInstance.getWires(dependency.getIdentifier())) {
			outgoing.remove();
		}
		
		return true;
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
                    getDeclaration().getProperties().put(configurationProperty.getName(),
                            configurationProperty.getValue());
                }
            }

            if (provides != null) {
                for (ProvidedServiceDescription providedServiceDescription : provides.getProvidedServices()) {
                    for (Object key : providedServiceDescription.getProperties().keySet()) {
                        getDeclaration().getProperties().put((String) key,
                                providedServiceDescription.getProperties().get(key).toString());
                    }
                }
            }
        }

        /*
         * For instances that are not created using the Apam API, register instance with APAM on validation
         */
        if (state == ComponentInstance.VALID && !isApamCreated)
            Apform2Apam.newInstance(this);

        if (state == ComponentInstance.INVALID)
            // Apform2Apam.vanishInstance(getInstanceName());
            ComponentBrokerImpl.disappearedComponent(getInstanceName());
    }

    /**
     * Apform: get the service object of the instance
     */
    @Override
    public Object getServiceObject() {
        return getPojoObject();
    }

    /**
     * Apform: resolve dependency
     */
    @Override
    public boolean setWire(Instance destInst, String depName) {
        // System.err.println("Native instance set wire " + depName + " :" + getInstanceName() + "->" + destInst);

        /*
         * Validate all the injections can be performed
         */

        for (DependencyInjectionManager injectedField : injectedFields) {
            if (!injectedField.isValid())
                return false;
        }

        /*
         * perform injection update
         */
        for (DependencyInjectionManager injectedField : injectedFields) {
            if (injectedField.getDependencyInjection().getDependency().getIdentifier().equals(depName)) {
                injectedField.addTarget(destInst);
            }
        }

        return true;
    }

    /**
     * Apform: unresolve dependency
     */
    @Override
    public boolean remWire(Instance destInst, String depName) {
        // System.err.println("Native instance rem wire " + depName + " :" + getInstanceName() + "->" + destInst);

        /*
         * Validate all the injections can be performed
         */

        for (DependencyInjectionManager injectedField : injectedFields) {
            if (!injectedField.isValid())
                return false;
        }

        /*
         * perform injection update
         */
        for (DependencyInjectionManager injectedField : injectedFields) {
            if (injectedField.getDependencyInjection().getDependency().getIdentifier().equals(depName)) {
                injectedField.removeTarget(destInst);
            }
        }

        return true;
    }

    /**
     * Apform: substitute dependency
     */
    @Override
    public boolean substWire(Instance oldDestInst, Instance newDestInst, String depName) {
        // System.err.println("Native instance subs wire " + depName + " :" + getInstanceName() + "from ->" +
        // oldDestInst
        // + " to ->" + newDestInst);

        /*
         * Validate all the injections can be performed
         */

        for (DependencyInjectionManager injectedField : injectedFields) {
            if (!injectedField.isValid())
                return false;
        }

        /*
         * perform injection update
         */
        for (DependencyInjectionManager injectedField : injectedFields) {
            if (injectedField.getDependencyInjection().getDependency().getIdentifier().equals(depName)) {
                injectedField.substituteTarget(oldDestInst, newDestInst);
            }
        }

        return true;
    }

    @Override
    public void setProperty(String attr, String value) {

        Object pojo = getPojoObject();
        Callback callback = propertyCallbacks.get(attr);

        if (pojo == null || callback == null)
            return;

		try {
			callback.call(pojo,new Object[] {value});
		} catch (Exception ignored) {
			getLogger().log(Logger.ERROR, "error invoking callback "+callback.getMethod()+" for property "+attr, ignored);
		}		
	}

    private void fireCallbacks(CallbackTrigger trigger, Object service) {
        if (getInst().getImpl().getDeclaration() instanceof AtomicImplementationDeclaration) {
            AtomicImplementationDeclaration atomicImpl = (AtomicImplementationDeclaration) getInst().getImpl().getDeclaration();
            Set<CallbackMethod> callbacks = atomicImpl.getCallback(trigger);
            if (callbacks != null) {
                for (CallbackMethod callbackMethod : callbacks) {
                    Method callback;
                    try {
                        if (callbackMethod.hasAnInstanceArgument()) {
                            callback = service.getClass().getMethod(callbackMethod.getMethodName(), Instance.class);
                            callback.invoke(service, this);
                        } else {
                            callback = service.getClass().getMethod(callbackMethod.getMethodName());
                            callback.invoke(service);
                        }
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
 }
