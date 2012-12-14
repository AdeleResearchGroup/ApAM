package fr.imag.adele.apam.apform.legacy.ipojo;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.IPojoFactory;
import org.apache.felix.ipojo.Pojo;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.impl.ApformComponentImpl;
import fr.imag.adele.apam.apform.impl.ApformInstanceImpl;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;

/**
 * This class tracks iPojo legacy implementations and instances and register
 * them in APAM
 * 
 * @author vega
 * 
 */
@org.apache.felix.ipojo.annotations.Component(name = "ApformIpojoTracker" , immediate=true)
@Instantiate(name = "ApformIpojoTracker-Instance")

public class ApformIpojoTracker implements ServiceTrackerCustomizer {

    /**
     * The reference to the APAM platform
     */
	@Requires
    private Apam                apam;

    /**
     * The instances service tracker.
     */
    private ServiceTracker      instancesServiceTracker;

    /**
     * The bundle context associated with this tracker
     */
    private final BundleContext context;


    public ApformIpojoTracker(BundleContext context) {
        this.context = context;
    }

    /**
     * Callback to handle factory binding
     */
    @Bind(id="factories",aggregate=true,optional=true)
    public void factoryBound(Factory factory) {

        if (factory instanceof ApformComponentImpl)
            return;

        if (factory instanceof IPojoFactory) {
            ApformImplementation implementation = new ApformIPojoImplementation((IPojoFactory) factory);
            Apform2Apam.newImplementation(implementation);
        }
    }

    /**
     * Callback to handle factory unbinding
     */
    @Unbind(id="factories",aggregate=true,optional=true)
    public void factoryUnbound(Factory factory) {
        if (factory instanceof ApformComponentImpl)
            return;

        if (factory instanceof IPojoFactory) {
           // Apform2Apam.vanishImplementation(factory.getName());
            ComponentBrokerImpl.disappearedComponent(factory.getName()) ;
        }

    }

    /**
     * Callback to handle instance binding
     */
    public boolean instanceBound(ServiceReference reference,
            ComponentInstance ipojoInstance) {
        /*
         * ignore handler instances
         */
        if (ipojoInstance.getFactory() instanceof HandlerFactory)
            return false;

        /*
         * Ignore instances of private factories, as no implementation is
         * available to register in APAM
         * 
         * TODO should we register iPojo private factories in APAM when their
         * instances are discovered? how to know when to unregister them?
         */
        try {
            String factoryFilter = "(factory.name=" + ipojoInstance.getFactory().getName() + ")";
            if (context.getServiceReferences(Factory.class.getName(),factoryFilter) == null)
                return false;
        } catch (InvalidSyntaxException ignored) {
        }

        /*
         * In the case of APAM instances registered in the registry (hybrid
         * components), registration in APAM has already be done by the Instance
         * Manager
         */
        if (ipojoInstance instanceof ApformInstanceImpl)
            return false;

        /*
         * For legacy instances, register the corresponding declaration in APAM
         */
        ApformIpojoInstance apformInstance = new ApformIpojoInstance(ipojoInstance, reference);
        Apform2Apam.newInstance(apformInstance);

        return true;
    }

    /**
     * Callback to handle instance unbinding
     */
    public void instanceUnbound(ComponentInstance ipojoInstance) {

        /*
         * In the case of APAM instances registered in the registry (hybrid
         * components), registration in APAM has already be done by the Instance
         * Manager
         */
        if (ipojoInstance instanceof ApformInstanceImpl)
            return;

        /*
         * For ipojo instances, unregister the corresponding declaration in
         * APAM
         */
        ComponentBrokerImpl.disappearedComponent(ipojoInstance.getInstanceName()) ;
    }

    /**
     * Starting.
     */
    @Validate
    public void start() {

        try {
            Filter filter = context.createFilter("(instance.name=*)");
            instancesServiceTracker = new ServiceTracker(context, filter, this);
            instancesServiceTracker.open();

        } catch (InvalidSyntaxException e) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Stopping.
     */
    @Invalidate
    public void stop() {
        instancesServiceTracker.close();
    }

    @Override
    public Object addingService(ServiceReference reference) {

        /*
         * Ignore events while APAM is not available
         */
        if (apam == null)
            return null;

        /*
         * ignore services that are not iPojo
         */
        Object service = context.getService(reference);
        if ((service instanceof Pojo) && instanceBound(reference,((Pojo) service).getComponentInstance()))
            return service;

        /*
         * If service is not a recognized iPojo instance, don't track it
         */
        context.ungetService(reference);
        return null;
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {

        if (!(service instanceof Pojo))
            return;

        ComponentInstance ipojoInstance = ((Pojo) service).getComponentInstance();
        instanceUnbound(ipojoInstance);
        context.ungetService(reference);
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {

        if (!(service instanceof Pojo))
            return;

        ComponentInstance ipojoInstance = ((Pojo) service).getComponentInstance();

        /*
         * If the service is not reified in APAM, just ignore event
         */
        Instance inst = CST.componentBroker.getInst(ipojoInstance.getInstanceName());
        if (inst == null)
            return;

        /*
         * Otherwise propagate property changes to Apam
         */
        for (String key : reference.getPropertyKeys()) {
            if (!Apform2Apam.isPlatformPrivateProperty(key)) {
                String value = reference.getProperty(key).toString();
                if (value != inst.getProperty(key))
                    inst.setProperty(key, value);
            }
        }
    }
}
