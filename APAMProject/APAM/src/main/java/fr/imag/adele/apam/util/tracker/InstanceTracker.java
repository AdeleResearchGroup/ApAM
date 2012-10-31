package fr.imag.adele.apam.util.tracker;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Instance;
import org.osgi.framework.Filter;

import java.util.Set;

/**
 * User: barjo
 * Date: 31/10/12
 * Time: 16:38
 */
public class InstanceTracker extends ComponentTracker<Instance> {

    public InstanceTracker(Filter filter) {
        super(filter);
    }

    public InstanceTracker(Filter filter, ComponentTrackerCustomizer<Instance> customizer) {
        super(filter, customizer);
    }

    @Override
    public void open() {
        synchronized (this) {

            ApamManagers.addDynamicManager(this);

            Set<Instance> instances = broker.getInsts(filter);

            for(Instance ins : instances){
                instances.add(ins);
                components.add(ins);
            }
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            ApamManagers.removeDynamicManager(this);

            for(Instance ins : components){
                customizer.removedComponent(ins);
            }

            components.clear();
        }
    }

    @Override
    public void addedInApam(Component newComponent) {
        if ( !(newComponent instanceof Instance) || !newComponent.match(filter)){
            return; //nothing to do
        }

        synchronized (this){
            customizer.addingComponent((Instance) newComponent);
            components.add((Instance) newComponent);
        }

    }

    @Override
    public void removedFromApam(Component lostComponent) {
        if ( !(lostComponent instanceof Instance)){
            return; //nothing to do
        }

        synchronized (this){
            if (components.remove(lostComponent))  {
                customizer.removedComponent((Instance) lostComponent);
            }
        }
    }
}
