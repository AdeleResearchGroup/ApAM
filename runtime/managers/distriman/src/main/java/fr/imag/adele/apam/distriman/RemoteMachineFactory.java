package fr.imag.adele.apam.distriman;

import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformCompositeType;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.impl.ComponentImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * ApformCompositeType of and the factory of RemoteMachine.
 * Instance of this class should be a singleton.
 *
 * @ThreadSafe
 */
public class RemoteMachineFactory implements ApformCompositeType {
    private static String PROP_MY_NAME = "DistriManMachine";

    private final CompositeDeclaration declaration;

    /**
     * The RemoteMachine created through this factory, indexed by their url.
     */
    private final Map<String, RemoteMachine> machines = new HashMap<String, RemoteMachine>();


    private final BundleContext my_context;

    public RemoteMachineFactory(BundleContext context) {
        my_context = context;

        //create my unique declaration
        declaration =  new CompositeDeclaration(PROP_MY_NAME,null,null);
        declaration.setInstantiable(false);

        //Add the ApformCompositeType to Apam
        Apform2Apam.newImplementation(this);
    }

    @Override
    public CompositeDeclaration getDeclaration() {
        return new CompositeDeclaration(PROP_MY_NAME,null,null);
    }

    @Override
    public Set<ManagerModel> getModels() {
        return null;
    }

    @Override
    public RemoteMachine createInstance(Map<String, String> initialproperties) throws ComponentImpl.InvalidConfiguration {
       throw new UnsupportedOperationException("ApformCompositeType is not instantiable");
    }

    /**
     * @param url The RemoteMachine unique URL
     * @return The newly created or existing RemoteMachine of given url
     */
    public RemoteMachine newRemoteMachine(String url) {
        synchronized (machines){
            if (machines.containsKey(url)){
                //TODO log warning
                return machines.get(url);
            }

            RemoteMachine machine = machines.put(url,new RemoteMachine(url,this));

            return machine;
        }
    }

    /**
     * Destroy the RemoteMachine of given url, created through this RemoteMachineFactory.
     * @param url the RemoteMachine URL
     * @return the destroyed RemoteMachine or null if not present.
     */
    public RemoteMachine destroyRemoteMachine(String url){
        RemoteMachine machine;

        synchronized (machines){
            machine = machines.remove(url);
        }

        if(machine != null){
            machine.destroy();
        }

        return machine;
    }

    @Override
    public ApformSpecification getSpecification() {
        return null;
    }

    @Override
    public void setProperty(String attr, String value) {
        throw new UnsupportedOperationException("Cannot change the property of RemoteMachineFactory.");
    }

    @Override
    public Bundle getBundle() {
        return my_context.getBundle();
    }
}