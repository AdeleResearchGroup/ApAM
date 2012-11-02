package fr.imag.adele.apam.util.tracker;

import fr.imag.adele.apam.*;
import org.osgi.framework.Filter;

import java.util.HashSet;
import java.util.Set;

/**
 * The {@code ComponentTracker} simplifies using {@code Component} from the Apam {@code ComponentBroker}
 *
 *
 */
 public class ComponentTracker<T extends Component> implements  ComponentTrackerCustomizer<T>, DynamicManager<T> {

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
        this(filter,null);
    }

    public ComponentTracker(final Filter filter,final ComponentTrackerCustomizer<T> customizer) {
        this.broker = CST.componentBroker;
        this.filter = filter;
        this.customizer= (customizer == null ? this : customizer);
        this.components = new HashSet<T>();
    }


    public void open(){

        ApamManagers.addDynamicManager(this);
        Set<T> presents = new HashSet<T>();
        if (presents.getClass().getComponentType().isAssignableFrom(Instance.class)){
            presents.addAll((Set<T>) broker.getInsts(filter));
        } else if (presents.getClass().getComponentType().isAssignableFrom(Specification.class)){
            presents.addAll((Set<T>) broker.getSpecs(filter));
        } else if (presents.getClass().getComponentType().isAssignableFrom(Implementation.class)){
            presents.addAll((Set<T>) broker.getImpls(filter));
        }
        synchronized (components){
            for(T comp : presents){
                components.add(comp);
                customizer.addingComponent(comp);
            }
        }

    }

    public void close(){
        ApamManagers.removeDynamicManager(this);

        synchronized (components){
            for(T comp : components){
                customizer.removedComponent(comp);
            }
            components.clear();
        }
    }

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

    /*------------------
       DynamicManager
     -------------------*/

    @Override
    public void addedInApam(T newComponent) {
        if (!newComponent.match(this.filter)){ //nothing to do
            return;
        }

        synchronized (components){
            customizer.addingComponent(newComponent);
            components.add(newComponent);
        }

    }

    @Override
    public void removedFromApam(T lostComponent) {
        synchronized (components){
            if(components.remove(lostComponent)){
                customizer.removedComponent(lostComponent);
            }
        }
    }
}