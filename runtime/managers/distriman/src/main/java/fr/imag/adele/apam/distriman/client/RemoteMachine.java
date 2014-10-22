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
package fr.imag.adele.apam.distriman.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.TypeReference;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.RelToResolve;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformImplementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.references.components.ImplementationReference;
import fr.imag.adele.apam.declarations.references.components.SpecificationReference;
import fr.imag.adele.apam.declarations.references.components.Versioned;
import fr.imag.adele.apam.declarations.references.resources.InterfaceReference;
import fr.imag.adele.apam.distriman.DistrimanConstant;
import fr.imag.adele.apam.distriman.discovery.ApamMachineFactoryImpl;
import fr.imag.adele.apam.distriman.dto.RemoteDependencyDeclaration;
import fr.imag.adele.apam.distriman.provider.EndpointRegistration;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;

/**
 * Each Apam/Distriman machines available over the network, have a RemoteMachine
 * composite.
 * 
 * User: barjo / jander Date: 05/12/12 Time: 14:32
 */
public class RemoteMachine implements ApformInstance {

    private static class RemoteImplem implements ApformImplementation {

		private final ImplementationDeclaration declaration;
		private Implementation implementation;
	
		public RemoteImplem(String name) {
	
		    String specName = null;
	
		    SpecificationReference specReference = null;
	
		    Specification spec = CST.componentBroker.getSpec(specName);
		    if (spec != null) {
		    	specReference = spec.getApformSpec().getDeclaration().getReference();
		    }
	
		    declaration = new RemoteImplementationDeclaration(name,specReference);
		    // TODO distriman: load remote interfaces
		    // declaration.getProvidedResources().add(new
		    // InterfaceReference(name));
		    declaration.setInstantiable(false);
		}

		@Override
		public ApformInstance addDiscoveredInstance(Map<String, Object> configuration) throws InvalidConfiguration,	UnsupportedOperationException {
		    throw new UnsupportedOperationException("RemoteImplem instances can only be created by resolution");
		}
	
		@Override
		public ApformInstance createInstance(Map<String, String> initialproperties)	throws InvalidConfiguration {
		    throw new UnsupportedOperationException("RemoteImplem is not instantiable");
		}
	
		@Override
		public Implementation getApamComponent() {
		    return implementation;
		}
	
		@Override
		public Bundle getBundle() {
		    return null;
		}
	
		@Override
		public ImplementationDeclaration getDeclaration() {
		    return declaration;
		}
	
		@Override
		public boolean remLink(Component destInst, String depName) {
		    return false;
		}
	
		@Override
		public void setApamComponent(Component apamComponent) {
		    implementation = (Implementation) apamComponent;
		}
	
		@Override
		public boolean setLink(Component destInst, String depName) {
		    return false;
		}
	
		@Override
		public void setProperty(String attr, String value) {
		}
	
		/* (non-Javadoc)
		 * @see fr.imag.adele.apam.apform.ApformComponent#checkLink(fr.imag.adele.apam.Component, java.lang.String)
		 */
		@Override
		public boolean checkLink(Component destInst, String depName) {
		    // TODO Auto-generated method stub
		    return false;
		}

    }

    public static class RemoteImplementationReference extends ImplementationReference<RemoteImplementationDeclaration> {

		public RemoteImplementationReference(String name) {
		    super(name);
		}

    }

    private static class RemoteImplementationDeclaration extends ImplementationDeclaration {

		protected RemoteImplementationDeclaration(String name, SpecificationReference specification) {
		    super(name,Versioned.any(specification));
		}
	
		@Override
		protected ImplementationReference<?> generateReference() {
		    return new RemoteImplementationReference(getName());
		}
    }
	

    /**
     * The RemoteMachine URL.
     */
    private final String RootURL;

    private final String ServletURL;

    private final String id;

    private final ApamMachineFactoryImpl my_impl;

    private Instance apamInstance = null;

    private static Logger logger = LoggerFactory.getLogger(RemoteMachine.class);

    private final InstanceDeclaration my_declaration;

    private final Set<EndpointRegistration> my_endregis = new HashSet<EndpointRegistration>();

    private final Set<String> remoteInstances = new HashSet<String>();

    private final AtomicBoolean running = new AtomicBoolean(true);

    private boolean isLocalhost = false;

    public RemoteMachine(String rootURL, String id, ApamMachineFactoryImpl daddy, boolean isLocalhost) {
		RootURL = rootURL;
		ServletURL = rootURL + DistrimanConstant.PROVIDER_URL;
		this.isLocalhost = isLocalhost;
		my_impl = daddy;
		this.id = id;
		my_declaration = new InstanceDeclaration(Versioned.any(daddy.getDeclaration().getReference()), "RemoteMachine_" + RootURL, null);
		my_declaration.setInstantiable(false);
	
		Apform2Apam.newInstance(this);
    }

    public void addEndpointRegistration(EndpointRegistration registration) {
    	my_endregis.add(registration);
    }

    private Instance createClientProxy(String jsondep, Instance client, RelToResolve dependency) throws IOException {

		HttpURLConnection connection = null;
		PrintWriter outWriter = null;
		BufferedReader serverResponse = null;
		StringBuffer buff = new StringBuffer();
		try {
	
		    logger.info("requesting resolution to address {}",
			    this.getURLServlet());
	
		    connection = (HttpURLConnection) new URL(this.getURLServlet())
			    .openConnection();
	
		    // SET REQUEST INFO
		    connection.setRequestMethod("POST");
		    connection.setDoOutput(true);
	
		    outWriter = new PrintWriter(connection.getOutputStream());
	
		    logger.info("request performed by the client {}", jsondep);
	
		    buff.append("content=");
		    buff.append(URLEncoder.encode(jsondep, "UTF-8"));
	
		    outWriter.write(buff.toString());
		    outWriter.flush();
	
		    serverResponse = new BufferedReader(new InputStreamReader(
			    connection.getInputStream()));
	
		    String line;
		    StringBuffer sb = new StringBuffer();
	
		    while ((line = serverResponse.readLine()) != null) {
			sb.append(line);
		    }
	
		    if (sb.toString().trim().equals("")) {
			return null;
		    }
	
		    String decoded = URLDecoder.decode(sb.toString(), "UTF-8");
	
		    System.out.println("Decoded value=" + decoded);
	
		    ObjectMapper om = new ObjectMapper();
	
		    JsonNode node = om.readValue(decoded, JsonNode.class);
	
		    Map<String, String> endpoints = om.convertValue(
			    node.get("endpoint_entry"),
			    new TypeReference<Map<String, String>>() {
			    });
	
		    for (Map.Entry<String, String> entry : endpoints.entrySet()) {
			String interfacename = entry.getKey();
			String endpointUrl = entry.getValue();
	
			logger.info("iterating over {} and {}", interfacename,
				endpointUrl);
	
			Object proxyRaw = null;
	
			if (dependency.getTarget() instanceof InterfaceReference) {
			    InterfaceReference ir = (InterfaceReference) dependency
				    .getTarget();
	
			    logger.info("Type to be loaded {}", ir.getJavaType());
	
			    logger.info("comparing interface {} with {}",
				    interfacename, ir.getJavaType());
	
			    if (interfacename.equals(ir.getJavaType())) {
	
				Class ifaceClazz = Class.forName(interfacename);
	
				logger.info(
					"connecting the interface {} to the endpoint {}",
					interfacename, endpointUrl);
	
				ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
				factory.setServiceClass(ifaceClazz);
				factory.setAddress(endpointUrl);
				proxyRaw = factory.create();
	
			    } else {
				logger.info("{} and {} are not equal", interfacename,
					ir.getJavaType());
			    }
			} else {
			    logger.info("its not a InterfaceReference");
			}
	
			RemoteInstanceImpl inst = new RemoteInstanceImpl(
				dependency.getName(), endpointUrl, this
					.getApamComponent().getComposite(), proxyRaw);
	
			String implName = inst.getImplementation();
			Implementation implem = CST.componentBroker.getImpl(implName);
			if (implem == null) {
			    implem = CST.componentBroker.addImpl(null,
				    new RemoteImplem(implName));
			}
	
			remoteInstances.add(inst.getFullName());
	
			return CST.componentBroker.addInst(null, inst);
	
		    }
	
		} catch (MalformedURLException mue) {
		    mue.printStackTrace();
		} catch (ClassNotFoundException e) {
		    e.printStackTrace();
		} finally {
	
		    if (connection != null) {
			connection.disconnect();
		    }
	
		    if (serverResponse != null) {
			try {
			    serverResponse.close();
			} catch (Exception ex) {
			}
		    }
		}
	
		return null;
    }

    /**
     * Destroy the RemoteMachine //TODO but a volatile destroyed flag ?
     */
    public void destroy() {

		logger.info("destroying remoteMachine {}", ServletURL);
	
		for (EndpointRegistration endreg : my_endregis) {
		    endreg.close();
		}
	
		for (String componentName : remoteInstances) {
			((ComponentBrokerImpl)CST.componentBroker).disappearedComponent(componentName);
		    remoteInstances.remove(componentName);
		}
	
		// Remove this Instance from the broker
		((ComponentBrokerImpl)CST.componentBroker).disappearedComponent(this.getDeclaration().getName());

    }

    @Override
    public Instance getApamComponent() {
		return apamInstance;
    }

    @Override
    public Bundle getBundle() {
		return my_impl.getBundle();
    }

    @Override
    public InstanceDeclaration getDeclaration() {
		return my_declaration;
    }

    public String getId() {
		return id;
    }

    @Override
    public Object getServiceObject() {
		return null;
    }

    // ===============
    // ApformInstance
    // ===============

    public String getURLRoot() {
		return RootURL;
    }

    public String getURLServlet() {
		return ServletURL;
    }

    public boolean isLocalhost() {
		return isLocalhost;
    }

    @Override
    public boolean remLink(Component destInst, String depName) {
		return false;
    }

    public Resolved resolveRemote(Instance client, RelToResolve dependency)  throws IOException {
	if (running.get()) {

	    RemoteDependencyDeclaration remoteDep = new RemoteDependencyDeclaration(
		    dependency, this.getURLRoot());

	    ObjectNode jsonObject = remoteDep.toJson();

	    String json = jsonObject.toString();

	    Instance instance = createClientProxy(json, client, dependency);

	    if (instance == null) {

		logger.info("dependency {} was NOT found in {}",
			dependency.getName(), this.getURLServlet());

		return null;
	    }

	    logger.info("dependency {} was found remotely in {}",
		    dependency.getName(), this.getURLServlet());

	    Set<Implementation> impl = Collections.emptySet();

	    return new Resolved(instance);

	}

	return null;
    }

    public boolean rmEndpointRegistration(EndpointRegistration registration) {
	return my_endregis.remove(registration);
    }

    @Override
    public void setApamComponent(Component apamComponent) {
	apamInstance = (Instance) apamComponent;
    }

    @Override
    public boolean setLink(Component destInst, String depName) {
	return false;
    }
    @Override
    public boolean checkLink(Component destInst, String depName) {
	return false;
    }    

    @Override
    public void setProperty(String attr, String value) {
	// TODO distriman: implement set property for remote instances
    }
}
