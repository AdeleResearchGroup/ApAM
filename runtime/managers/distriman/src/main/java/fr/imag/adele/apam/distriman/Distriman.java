/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.distriman;

import fr.imag.adele.apam.*;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.distriman.disco.MachineDiscovery;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@org.apache.felix.ipojo.annotations.Component(name = "Apam::Distriman")
@Instantiate
@Provides
public class Distriman implements DependencyManager{

    //ApamManager priority
    private static final int PRIORITY = 4;

    /**
     * Hostname of the InetAdress to be used for the MachineDiscovery.
     */
    @Property(name = "inet.host",value = "localhost",mandatory = true)
    private String HOST;

    @Requires(optional = false)
    private HttpService http;


    //Default logger
    private static Logger logger = LoggerFactory.getLogger(Distriman.class);

    private final CxfEndpointFactory endpointFactory;

    private RemoteMachineFactory remotes;

    private final LocalMachine my_local = LocalMachine.INSTANCE;

    /**
     * MachineDiscovery allows for machine discovery
     */
    private final MachineDiscovery discovery;


    private final BundleContext context;

    public Distriman(BundleContext context) {
        this.context = context;

        remotes = new RemoteMachineFactory(context);
        discovery = new MachineDiscovery(remotes);
        endpointFactory=new CxfEndpointFactory();
    }

    public String getName() {
        return CST.DISTRIMAN;
    }


    //
    // DependencyManager methods
    //

    @Override
    public void getSelectionPath(Instance client, DependencyDeclaration dependency, List<DependencyManager> selPath) {
        //To change body of implemented methods use File | Settings | File Templates.
    	selPath.add(selPath.size(), this);
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void newComposite(ManagerModel model, CompositeType composite) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    /**
     * That's the meat! Ask synchroneously to each available RemoteMachine to resolved the <code>dependency</code>,
     * the first to solve it create the proxy.
     *
     * @param client the instance asking for the resolution (and where to create implementation, if needed). Cannot be null.
     * @param dependency a dependency declaration containing the type and name of the dependency target. It can be
     *            -the specification Name (new SpecificationReference (specName))
     *            -an implementation name (new ImplementationRefernece (name)
     *            -an interface name (new InterfaceReference (interfaceName))
     *            -a message name (new MessageReference (dataTypeName))
     *            - or any future resource ...
     * @param needsInstances
     * @return The Resolved object if a proxy has been created, null otherwise.
     */
	public Resolved resolveDependency(Instance client, DependencyDeclaration dependency, boolean needsInstances) {
        Resolved resolved = null;
        
        if (!needsInstances){ //TODO should really just handle only instances?
            return null;
        }

        Iterator<RemoteMachine> machines = remotes.getRemoteMachines().iterator();

        while (machines.hasNext() && resolved == null){
        	
        	RemoteMachine ma=machines.next();
        	
            resolved = ma.resolveRemote(client,dependency);
            
        }
        
       if (resolved.instances!=null)
    	   System.out.println(String.format("Dependency %s resolved, number of available instances:%d",dependency.getIdentifier(),resolved.instances.size()));

		return resolved;
	}

    @Override
    public Instance findInstByName(Instance client, String instName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Implementation findImplByName(Instance client, String implName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Specification findSpecByName(Instance client, String specName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Component findComponentByName(Instance client, String compName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Instance resolveImpl(Instance client, Implementation impl, Set<String> constraints, List<String> preferences) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<Instance> resolveImpls(Instance client, Implementation impl, Set<String> constraints) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl, Instance inst, Set<Instance> insts) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ComponentBundle findBundle(CompositeType context, String bundleSymbolicName, String componentName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //
    // Lifecycle call back
    //

    @Validate
    private void init(){
        logInfo("Starting...");
        
        //init the local machine
        my_local.init("127.0.0.1",Integer.parseInt(context.getProperty("org.osgi.service.http.port")),this);

        //start the discovery
        discovery.start(HOST);

        //start the CxfEndpointFactory
        endpointFactory.start(http);

        //Register this local machine servlet
        try {
        	
        	System.out.println("##### Registering:"+LocalMachine.INSTANCE.getPath());
        	
            http.registerServlet(LocalMachine.INSTANCE.getPath(),my_local.getServlet(),null,null);
        } catch (Exception e) {
            discovery.stop();
           throw new RuntimeException(e);
        }

        //publish this local machine over the network!
        try {
            discovery.publishLocalMachine(my_local);
        } catch (IOException e) {
            discovery.stop();
            http.unregister(my_local.getPath());
            throw new RuntimeException(e);
        }

        //Add this manager to Apam
        ApamManagers.addDependencyManager(this,PRIORITY);

        //Add Distriman to Apam
        logInfo("Successfully initialized");
    }

    @Invalidate
    private void stop(){
        logInfo("Stopping...");

        //Goodbye Apam
        ApamManagers.removeDependencyManager(this);

        //stop the discovery
        discovery.stop();

        //stop the CxfEndpointFactory
        endpointFactory.stop(http);
        
        http.unregister(LocalMachine.INSTANCE.getPath());//my_local.getPath()

        logInfo("Successfully stopped");
    }

    //
    // Endpoints Creation methods
    //


    public EndpointRegistration resolveRemoteDependency(RemoteDependency dependency, String machineUrl) throws ClassNotFoundException{
        EndpointRegistration registration = null;

        System.out.println("**** Fetching machine:"+machineUrl);
        
        //Get the composite that represent the remote machine asking to resolve the RemoteDependency
        RemoteMachine remote = remotes.getRemoteMachine(machineUrl);

        //No RemoteMachine corresponding to the given url is available
        if(remote == null){
            return null;
        }

        return endpointFactory.resolveAndExport(dependency,remote);
    }

    //
    // Convenient static log method
    //
    protected static void logInfo(String message,Throwable t){
        logger.info("["+CST.DISTRIMAN+"]"+message,t);
    }

    protected static void logInfo(String message){
        logger.info("["+CST.DISTRIMAN+"]"+message);
    }
}
