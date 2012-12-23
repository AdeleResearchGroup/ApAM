package fr.imag.adele.apam.distriman;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singleton;

/**
 * Each Apam/Distriman machines available over the network, have a RemoteMachine composite.
 *
 *
 *
 * User: barjo
 * Date: 05/12/12
 * Time: 14:32
 */
public class RemoteMachine  implements ApformInstance{

    /**
     * The RemoteMachine URL.
     */
    private final String my_url;

    private final RemoteMachineFactory my_impl;

    private Instance apamInstance = null;

    private static Logger logger = LoggerFactory.getLogger(RemoteMachine.class);

    private final InstanceDeclaration my_declaration;

    private final Set<EndpointRegistration> my_endregis = new HashSet<EndpointRegistration>();

    private final AtomicBoolean running = new AtomicBoolean(true);

    protected RemoteMachine(String url, RemoteMachineFactory daddy) {
        my_url = url;
        my_impl = daddy;
        my_declaration = new InstanceDeclaration(daddy.getDeclaration().getReference(),"RemoteMachine_"+url,null);
        my_declaration.setInstantiable(false);

        //Add the Instance to Apam
        Apform2Apam.newInstance(this);

        logger.info("RemoteMachine "+my_url+" created.");
        System.out.println("RemoteMachine "+my_url+" created.");
    }

    public String getUrl() {
        return my_url;
    }

    public void addEndpointRegistration(EndpointRegistration registration){
        if(running.get())
            my_endregis.add(registration);
    }

    public boolean rmEndpointRegistration(EndpointRegistration registration){
        return running.get() && my_endregis.remove(registration);
    }

    /**
     * Destroy the RemoteMachine
     * //TODO but a volatile destroyed flag ?
     */
    protected void destroy() {
        if(running.compareAndSet(true,false)){

            logger.info("RemoteMachine " + my_url + " destroyed.");
            System.out.println("RemoteMachine " + my_url + " destroyed.");

            //Remove this Instance from the broker
            ComponentBrokerImpl.disappearedComponent(this.getDeclaration().getName());

            for(EndpointRegistration endreg: my_endregis){
                endreg.close();
            }
        }
    }

    public Resolved resolveRemote(Instance client, DependencyDeclaration dependency) {
        if(running.get()){
            try{
                RemoteDependency remoteDep = new RemoteDependency(dependency);

                String json = remoteDep.toJson().toString();
                Instance instance = createProxy(json);

                if (instance == null){
                    return null;
                }

                return new Resolved(null, singleton(instance));

                //createProxy(json);
                //TODO call this machine getUrl
            }catch (Exception e){
                //TODO handle
            }
        }
        return null; //TODO
    }

    private Instance createProxy(String jsondep){
        return null; // null
    }

    // ===============
    // ApformInstance
    // ===============

    @Override
    public InstanceDeclaration getDeclaration() {
        return my_declaration;
    }

    @Override
    public void setProperty(String attr, String value) {
        throw new UnsupportedOperationException("Cannot change RemoteMachine property during the execution");
    }

    @Override
    public Bundle getBundle() {
        return my_impl.getBundle();
    }

    @Override
    public Object getServiceObject() {
        return null;
    }

    @Override
    public boolean setWire(Instance destInst, String depName) {
        return false;
    }

    @Override
    public boolean remWire(Instance destInst, String depName) {
        return false;
    }

    @Override
    public boolean substWire(Instance oldDestInst, Instance newDestInst, String depName) {
        return false;
    }

    @Override
    public void setInst(Instance asmInstImpl) {
        this.apamInstance=asmInstImpl;

    }

    @Override
    public Instance getInst() {
        return apamInstance;
    }
}



