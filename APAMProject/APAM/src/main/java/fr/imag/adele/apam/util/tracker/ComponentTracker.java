package fr.imag.adele.apam.util.tracker;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.ComponentBroker;
import fr.imag.adele.apam.DynamicManager;
import org.osgi.framework.Filter;

import java.util.HashSet;
import java.util.Set;

/**
 * The {@code ComponentTracker} simplifies using {@code Component} from the Apam {@code ComponentBroker}
 *
 *
 */
 abstract class ComponentTracker<T extends Component> implements  ComponentTrackerCustomizer<T>, DynamicManager {

    /**
     * The Apam {@code ComponentBroker} being used by this tracker.
     */
    protected final ComponentBroker broker;


    /**
     * The filter used by this {@code ComponentTracker} which specifies the search criteria for the
     * {@code Component} to track.
     */
    protected final Filter filter;


    protected final Set<T> components;


    protected final ComponentTrackerCustomizer<T> customizer;

    public ComponentTracker(final Filter filter) {
        this(filter, null);
    }

    public ComponentTracker(final Filter filter,final ComponentTrackerCustomizer<T> customizer) {
        this.broker = CST.componentBroker;
        this.filter = filter;
        this.customizer= (customizer == null ? this : customizer);
        this.components = new HashSet<T>();
    }


    public abstract void open();

    public abstract void close();

    /*---------
      Default Customizer
     ----------*/

    @Override
    public void addingComponent(T component) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removedComponent(T component) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
