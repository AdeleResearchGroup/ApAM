package fr.imag.adele.apam.apformipojo;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.apformipojo.handlers.DependencyInjectionManager;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.core.ResolvableReference;

public class ApformIpojoInstance extends InstanceManager implements ApformInstance, DependencyInjectionManager.Resolver  {

	/**
	 * The property used to configure this instance with its declaration
	 */
	public final static String ATT_DECLARATION	= "declaration";
	
    /**
     * Whether this instance was created directly using the APAM API
     */
    private final boolean				isApamCreated;

    /**
     * The declaration of the instance
     */
    private InstanceDeclaration			declaration;

    /**
     * The APAM instance associated to this component instance
     */
    private Instance					apamInstance;

    /**
     * The list of injected fields handled by this instance
     */
    private Set<DependencyInjectionManager> 	injectedFields;
    
	public ApformIpojoInstance(ApformIpojoImplementation implementation, boolean isApamCreated, BundleContext context,
            HandlerManager[] handlers) {

    	super(implementation, context, handlers);
    	this.isApamCreated	= isApamCreated;
    	this.injectedFields	= new HashSet<DependencyInjectionManager>();
    }

    @Override
    public ApformIpojoImplementation getFactory() {
        return (ApformIpojoImplementation) super.getFactory();
    }
    
	@Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
    	
		String instanceName = (String) configuration.get("instance.name");
    	declaration 		= (InstanceDeclaration) configuration.get(ATT_DECLARATION);
    	
    	if (isApamCreated || declaration == null) {
    		declaration = new InstanceDeclaration(getFactory().getDeclaration().getReference(),instanceName,null);
    		for (Enumeration<String> properties = configuration.keys(); properties.hasMoreElements();) {
    			String property = properties.nextElement();
				declaration.getProperties().put(property, configuration.get(property));
			}
    	}
    	
    	configuration.put("instance.name",declaration.getName());
    	/* 
    	 * TODO we had this guard to avoid  configuring the instance for non instrumented
    	 * components, but now that composite instances become iPojo instances we try to use
    	 * it, but may have unexpected effects to test
    	 * 
    	 * if (getFactory().hasInstrumentedCode())
    	 */
    	super.configure(metadata, configuration);

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
    }

    /**
     * The attached APAM instance
     */
    public Instance getApamInstance() {
    	return this.apamInstance;
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
     * Delegate APAM to resolve a given injection.
     * 
     * NOTE nothing is returned from this method, the call to APAM has as
     * side-effect the update of the dependency.
     * 
     * @param dependency
     */
    @Override
    public void resolve(DependencyInjectionManager injection) {

        /*
         * This instance is not actually yet managed by APAM
         */
        if (apamInstance == null) {
            System.err.println("resolve failure for client " + getInstanceName() + " : ASM instance unkown");
            return;
        }

        Apam apam = getFactory().getApam();
        if (apam == null) {
            System.err.println("resolve failure for client " + getInstanceName() + " : APAM not found");
            return;
        }

//        System.err.println("resolving " + getInstanceName() + " dependency " + dependency.getName());
        /*
         * Make a copy of constraints and preferences before invoking resolution. This allow resolution managers to modify constraints
         * and preferences are part of their processing.
         */
        
        DependencyDeclaration dependency 	= injection.getDependencyInjection().getDependency();
        ResolvableReference target			= dependency.getTarget();

        /*
         * Resolve implementation dependencies explicitly
         */
        ImplementationReference<?> targetImplementation = target.as(ImplementationReference.class);
        if ( targetImplementation != null) {

        	if (!dependency.isMultiple())
                ApamResolver.newWireImpl(apamInstance, targetImplementation.getName(), dependency.getIdentifier());
            else
                ApamResolver.newWireImpls(apamInstance, targetImplementation.getName(), dependency.getIdentifier());
        	
        	return;
        }
        
        /*
         * Resolve all other dependencies by specification
         */
        if (!dependency.isMultiple())
            ApamResolver.newWireSpec(apamInstance, dependency.getIdentifier());
        else
            ApamResolver.newWireSpecs(apamInstance, dependency.getIdentifier());
        
    }

    /**
     * Notify instance activation/deactivation
     */
    @Override
    public void setState(int state) {
        super.setState(state);

        if (isApamCreated || !isStarted())
            return;

        if (state == ComponentInstance.VALID)
            Apform2Apam.newInstance(getInstanceName(), this);

        if (state == ComponentInstance.INVALID)
            Apform2Apam.vanishInstance(getInstanceName());
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
//        System.err.println("Native instance set wire " + depName + " :" + getInstanceName() + "->" + destInst);
    	
    	boolean injected = false;
    	
    	for (DependencyInjectionManager injectedField : injectedFields) {
			if (injectedField.getDependencyInjection().getDependency().getIdentifier().equals(depName)) {
				injectedField.addTarget(destInst);
				injected = true;
			}
		}
    	
        return injected;
    }

    /**
     * Apform: unresolve dependency
     */
    @Override
    public boolean remWire(Instance destInst, String depName) {
//        System.err.println("Native instance rem wire " + depName + " :" + getInstanceName() + "->" + destInst);
    	boolean injected = false;
    	
    	for (DependencyInjectionManager injectedField : injectedFields) {
			if (injectedField.getDependencyInjection().getDependency().getIdentifier().equals(depName)) {
				injectedField.removeTarget(destInst);
				injected = true;
			}
		}
    	
        return injected;
    }

    /**
     * Apform: substitute dependency
     */
    @Override
    public boolean substWire(Instance oldDestInst, Instance newDestInst, String depName) {
//        System.err.println("Native instance subs wire " + depName + " :" + getInstanceName() + "from ->" + oldDestInst
//                + " to ->" + newDestInst);
    	boolean injected = false;
    	
    	for (DependencyInjectionManager injectedField : injectedFields) {
			if (injectedField.getDependencyInjection().getDependency().getIdentifier().equals(depName)) {
				injectedField.substituteTarget(oldDestInst, newDestInst);
				injected = true;
			}
		}
    	
        return injected;
    }

}
