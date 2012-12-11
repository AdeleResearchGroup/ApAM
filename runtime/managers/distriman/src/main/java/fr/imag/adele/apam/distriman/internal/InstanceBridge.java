package fr.imag.adele.apam.distriman.internal;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.tracker.ComponentTrackerCustomizer;
import fr.imag.adele.apam.util.tracker.InstanceTracker;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.ow2.chameleon.rose.api.Machine;

import java.util.HashMap;
import java.util.Map;

/**
 * User: barjo
 * Date: 23/11/12
 * Time: 14:26
 */
@Component(name = "InstanceBridge")
public class InstanceBridge implements ComponentTrackerCustomizer<Instance> {

    private final BundleContext context;
    private Machine machine;
    private final InstanceTracker tracker;
    private final Map<Instance,ServiceRegistration> registrations = new HashMap<Instance, ServiceRegistration>();


    public InstanceBridge(BundleContext context) {
        this.context = context;
        this.tracker = new InstanceTracker(ApamFilter.newInstance(""),this);
    }

    @Validate
    private void start() {

        //Start the instance tracker
        tracker.open();
    }

    @Invalidate
    private void stop() {

        //Stop the instance tracker
        tracker.close();

        //registration must be empty, (removed call by the close)
        assert registrations.isEmpty();
    }


    @Override
    public void addingComponent(Instance component) {
        //To change body of implemented methods use File | Settings | File Templates.
        //context.registerService()
    }

    @Override
    public void removedComponent(Instance component) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}