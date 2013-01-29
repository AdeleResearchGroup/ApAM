package fr.imag.adele.apam.util.tracker;

import fr.imag.adele.apam.*;

import org.osgi.framework.Filter;

import java.util.HashSet;
import java.util.Set;

/**
 * The {@code ComponentTracker} tracks {@code Component} from the Apam {@code ComponentBroker}
 *
 * @ThreadSafe
 */
public class ComponentTracker<T extends Component> implements  ComponentTrackerCustomizer<T> {

    /**
     * The Apam {@code ComponentBroker} being used by this tracker.
     */
    protected final ComponentBroker broker;


    /**
     * The filter used by this {@code ComponentTracker} which specifies the search criteria for the
     * {@code Component} to track.
     */
    private final Filter filter;

    private final Set<T> components;

    private final ComponentTrackerCustomizer<T> customizer;

    private final Class<T> type;

    private final ComponentListener listener;

    protected ComponentTracker(final Class<T> type, final Filter filter) {
        this(type,filter,null);
    }

    protected ComponentTracker(final Class<T> type, final Filter filter,final ComponentTrackerCustomizer<T> customizer) {
        this.broker = CST.componentBroker;
        this.filter = filter;
        this.customizer= (customizer == null ? this : customizer);
        this.components = new HashSet<T>();
        this.listener = new ComponentListener();
        this.type=type;
    }


    /**
     * Start to track the Component.
     */
    public void open(){

        ApamManagers.addDynamicManager(listener);
        Set<T> presents = new HashSet<T>();
        if (type.isAssignableFrom(Instance.class)){
            presents.addAll((Set<T>) broker.getInsts(filter));
        } else if (type.isAssignableFrom(Specification.class)){
            presents.addAll((Set<T>) broker.getSpecs(filter));
        } else if (type.isAssignableFrom(Implementation.class)){
            presents.addAll((Set<T>) broker.getImpls(filter));
        }
        synchronized (components){
            for(T comp : presents){
                components.add(comp);
                customizer.addingComponent(comp);
            }
        }

    }

    /**
     * Close the tracker, stop tracking the Component.
     */
    public void close(){
        ApamManagers.removeDynamicManager(listener);

        synchronized (components){
            for(T comp : components){
                customizer.removedComponent(comp);
            }
            components.clear();
        }
    }

    /**
     * @return The Component tracked by this ComponentTracker.
     */
    public Set<T> getComponents(){
        Set<T> tracked = new HashSet<T>();

        synchronized (components){
            tracked.addAll(components);
            return tracked;
        }
    }


    /**
     * @return The numbers of Component track by this ComponentTracker.
     */
    public int size(){
        synchronized (components){
            return components.size();
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
       ComponentListener
     -------------------*/

    private class ComponentListener implements DynamicManager{


        public void addedComponent(Component newComponent) {


            if (!newComponent.match(filter)){ //nothing to do
                return;
            }

            if (!type.isInstance(newComponent)){
                return;
            }

            synchronized (components){
                customizer.addingComponent((T) newComponent);
                components.add((T) newComponent);
            }

        }

        public void removedComponent(Component lostComponent) {
            if (!type.isInstance(lostComponent)){
                return;
            }

            synchronized (components){
                if(components.remove(lostComponent)){
                    customizer.removedComponent((T) lostComponent);
                }
            }
        }

		@Override
		public void removedWire(Wire wire) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void addedWire(Wire wire) {
			// TODO Auto-generated method stub
			
		}

    }
}