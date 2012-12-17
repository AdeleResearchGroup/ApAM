package fr.imag.adele.apam.distriman;

import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Destroy the RemoteMachine
     */
    protected void destroy() {
        logger.info("RemoteMachine " + my_url + " destroyed.");
        System.out.println("RemoteMachine " + my_url + " destroyed.");

        //Remove this Instance from the broker
        ComponentBrokerImpl.disappearedComponent(this.getDeclaration().getName());
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
