package fr.imag.adele.apam.distriman;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import fr.imag.adele.apam.*;

/**
 * User: barjo
 * Date: 18/12/12
 * Time: 14:15
 *
 * @ThreadSafe
 */
public class EndpointFactory {

    public static final String PROTOCOL_NAME = "cxf";

    private final DependencyManager apamMan;

    /**
     * Multimap containing the exported Instances and their Endpoint registrations
     */
    private final SetMultimap<Instance,EndpointRegistration> endpoints = HashMultimap.create();

    public EndpointFactory() {
        //Get apamMan
        apamMan = ApamManagers.getManager(CST.APAMMAN);
    }

    private String createEndpoint(Instance instance){
        //TODO create the endpoint with cxf
        return instance.getName();
    }

    private void destroyEndpoint(String name){
        //TODO Destroy the endpoint

    }

    public EndpointRegistration resolveAndExport(RemoteDependency dependency,RemoteMachine client){
        Instance neo = null; //The chosen one
        EndpointRegistration registration = null;
        //Get local instance matching the RemoteDependency
        Resolved resolved = apamMan.resolveDependency(client.getInst(),dependency,true);


        //No local instance matching the RemoteDependency
        if(resolved.instances.isEmpty()){
            return null;
        }

        //Check if we already have an endpoint for the instances
        synchronized (endpoints){

            Sets.SetView<Instance> alreadyExported = Sets.intersection(resolved.instances, endpoints.keySet());

            //Nope, create a new endpoint
            if (alreadyExported.isEmpty()){
                neo=resolved.instances.iterator().next();

                //Todo Create endpoint
                registration=new EndpointRegistrationImpl(neo,client,createEndpoint(neo),PROTOCOL_NAME);

            }  else {
                neo=alreadyExported.iterator().next();
                registration=new EndpointRegistrationImpl(endpoints.get(neo).iterator().next());
            }

            //Add the EndpointRegistration to endpoints
            endpoints.put(neo,registration);
        }


        return registration;
    }

    /**
     * EndpointRegistration implementation.
     */
    private class EndpointRegistrationImpl implements EndpointRegistration{
        private Instance exported;
        private RemoteMachine client;
        private String url;
        private String protocol;

        private EndpointRegistrationImpl(Instance instance, RemoteMachine client, String endpointUrl, String protocol) {
            if(instance == null || client == null || endpointUrl == null ){
                throw new NullPointerException("Instance, RemoteMachine, endpointUrl cannot be null");
            }

            this.exported   = instance;
            this.client     = client;
            this.url        = endpointUrl;
            this.protocol   = protocol;

            client.addEndpointRegistration(this);
        }

        /**
         * Clone
         * @param registration The EndpointRegistration to be cloned.
         */
        private EndpointRegistrationImpl(EndpointRegistration registration){
            this(registration.getInstance(),registration.getClient(),registration.getEndpointUrl(),registration.getProtocol());
        }

        @Override
        public Instance getInstance() {
            return exported;
        }

        @Override
        public RemoteMachine getClient() {
            return client;
        }

        @Override
        public String getEndpointUrl() {
            return url;
        }

        @Override
        public String getProtocol() {
            return protocol;
        }


        @Override
        public void close() {
            //Has already been closed
            if(exported == null){
                return;
            }

            synchronized (endpoints){

                //remove this EndpointRegistration to the RemoteMachine that ask for it.
                client.rmEndpointRegistration(this);

                endpoints.remove(getInstance(),getClient());

                //Last registration, destroy the endpoints.
                if (!endpoints.containsKey(getInstance()))
                    destroyEndpoint(getEndpointUrl());

                exported    = null;
                client      = null;
                url         = null;
                protocol    = null;

                //todo if last destroy endpoint.
            }
        }

    }

}
