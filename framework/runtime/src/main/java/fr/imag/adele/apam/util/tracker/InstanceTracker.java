package fr.imag.adele.apam.util.tracker;

import fr.imag.adele.apam.Instance;
import org.osgi.framework.Filter;

/**
 * The {@code InstanceTracker} is a {@code ComponentTracker} specialized in order to track {@code Instance}.
 *
 * User: barjo
 * Date: 05/11/12
 * Time: 11:41
 */
public class InstanceTracker extends ComponentTracker<Instance> {

    public InstanceTracker(final Filter filter) {
        super(Instance.class, filter);
    }

    public InstanceTracker(final Filter filter, final ComponentTrackerCustomizer<Instance> customizer) {
        super(Instance.class, filter, customizer);
    }
}