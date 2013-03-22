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
package fr.imag.adele.apam.distriman.discovery;


import static com.google.common.collect.Sets.newHashSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.eclipse.jetty.util.log.Log;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformCompositeType;
import fr.imag.adele.apam.apform.ApformSpecification;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.distriman.client.RemoteMachine;
import fr.imag.adele.apam.distriman.provider.LocalMachine;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;
import fr.imag.adele.apam.impl.ComponentImpl;

/**
 * ApformCompositeType of and the factory of RemoteMachine.
 * Instance of this class should be a singleton.
 *
 * @ThreadSafe
 */

@Component
@Instantiate
@Provides
public class RemoteMachineFactory implements ApamMachineDiscovery,ApformCompositeType {
    private static String PROP_MY_NAME = "DistriManMachine";

    private final CompositeDeclaration declaration;

    static Logger logger = LoggerFactory.getLogger(RemoteMachineFactory.class);
    
    private static final Map<String, RemoteMachine> machines = new HashMap<String, RemoteMachine>();


    private final BundleContext my_context;

    public RemoteMachineFactory(BundleContext context) {
    	
        my_context = context;

        //create my unique declaration
        declaration =  new CompositeDeclaration(PROP_MY_NAME,null,null);
        declaration.setInstantiable(false);
    }

    @Validate
    public void init(){
        //Add the ApformCompositeType to Apam
        Apform2Apam.newImplementation(this);
    }

    @Invalidate
    public void destroy(){
        //Remove this implem from the broker
        ComponentBrokerImpl.disappearedComponent(getDeclaration().getName());
    }

    @Override
    public CompositeDeclaration getDeclaration() {
        return declaration;
    }

    @Override
    public Set<ManagerModel> getModels() {
        return Collections.emptySet();
    }

    @Override
    public RemoteMachine createInstance(Map<String, String> initialproperties) throws ComponentImpl.InvalidConfiguration {
       throw new UnsupportedOperationException("ApformCompositeType is not instantiable");
    }

    /**
     * @param url The RemoteMachine unique URL
     * @return The newly created or existing RemoteMachine of given url
     */
    public RemoteMachine newRemoteMachine(String url,String id) {
    	
        synchronized (machines){
            if (machines.containsKey(url)){
                logger.error("machine {} is already in the pool of machines",url);
                return machines.get(url);
            }
            
            RemoteMachine machine = machines.put(url,new RemoteMachine(url,id,this));
            
            return machine;
        }
    }

    /**
     * Destroy the RemoteMachine of given url, created through this RemoteMachineFactory.
     * @param url the RemoteMachine URL
     * @return the destroyed RemoteMachine or null if not present.
     */
    public RemoteMachine destroyRemoteMachine(String url,String id){
        RemoteMachine machine;

        for(Map.Entry<String, RemoteMachine> element:machines.entrySet()){
        	if(element.getValue().getId().equals(id)){
        		logger.info("destroying machine with the id {}",id);
        		machines.remove(element.getValue().getURL());
        		element.getValue().destroy();
        	}
        	logger.info("pool of machine contains key {}",element.getKey());
        }
        
        synchronized (machines){
            machine = machines.remove(url);
        }

        if(machine != null){
        	logger.info("destroying machine {}",url);
            machine.destroy();
        }else {
        	logger.info("machine {} was not found in pool of machines by the url",url);
        }

        return machine;
    }
    
    public void destroyRemoteMachines(){

        for(Map.Entry<String, RemoteMachine> element:machines.entrySet()){
        	destroyRemoteMachine(element.getKey(),element.getValue().getId());
        }
    
    }

    /**
     * @param url The RemoteMachine url
     * @return The RemoteMachine representing the machine of given <code>url</code>
     */
    public RemoteMachine getRemoteMachine(String url){
        synchronized (machines){
        	
        	RemoteMachine rm=machines.get(url+"/apam/machine");
        	//TODO distriman: find a better way to avoid this reference to localhost/127.0.0.1 
        	if(rm!=null) return rm;
        	
        	if(url.indexOf("127.0.0.1")!=-1){
        		rm=machines.get(url.replaceAll("127.0.0.1", "localhost")+"/apam/machine");
        	}
        	
            return rm;
        }
    }

    /**
     * @return A set containing all available RemoteMachine
     */
    public Set<RemoteMachine> getRemoteMachines(){
        synchronized (machines){
            return newHashSet(machines.values());
        }
    }


    @Override
    public ApformSpecification getSpecification() {
        return null;
    }

    @Override
    public void setProperty(String attr, String value) {
        //TODO distriman: implement set property for distriman representation in apam?
    }

    @Override
    public Bundle getBundle() {
        return my_context.getBundle();
    }

	public Map<String, RemoteMachine> getMachines() {
		return machines;
	}

}