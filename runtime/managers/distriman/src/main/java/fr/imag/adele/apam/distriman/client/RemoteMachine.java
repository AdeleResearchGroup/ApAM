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

import static java.util.Collections.singleton;

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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.distriman.discovery.RemoteMachineFactory;
import fr.imag.adele.apam.distriman.dto.RemoteDependency;
import fr.imag.adele.apam.distriman.provider.EndpointRegistration;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.impl.RemoteInstanceImpl;

/**
 * Each Apam/Distriman machines available over the network, have a RemoteMachine
 * composite.
 * 
 * 
 * 
 * User: barjo Date: 05/12/12 Time: 14:32
 */
public class RemoteMachine implements ApformInstance {

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

	public RemoteMachine(String url, RemoteMachineFactory daddy) {
		my_url = url;
		my_impl = daddy;
		my_declaration = new InstanceDeclaration(daddy.getDeclaration()
				.getReference(), "RemoteMachine_" + url, null);
		my_declaration.setInstantiable(false);

		// Add the Instance to Apam
		Apform2Apam.newInstance(this);

		logger.info("RemoteMachine " + my_url + " created.");
		System.out.println("RemoteMachine " + my_url + " created.");
	}

	public String getUrl() {
		return my_url;
	}

	public void addEndpointRegistration(EndpointRegistration registration) {
		if (running.get())
			my_endregis.add(registration);
	}

	public boolean rmEndpointRegistration(EndpointRegistration registration) {
		return running.get() && my_endregis.remove(registration);
	}

	/**
	 * Destroy the RemoteMachine //TODO but a volatile destroyed flag ?
	 */
	public void destroy() {
		if (running.compareAndSet(true, false)) {

			logger.info("RemoteMachine " + my_url + " destroyed.");
			System.out.println("RemoteMachine " + my_url + " destroyed.");

			// Remove this Instance from the broker
			ComponentBrokerImpl.disappearedComponent(this.getDeclaration()
					.getName());

			for (EndpointRegistration endreg : my_endregis) {
				endreg.close();
			}
		}
	}

	public Resolved resolveRemote(Instance client,
			DependencyDeclaration dependency) {
		if (running.get()) {
			try {
				RemoteDependency remoteDep = new RemoteDependency(dependency);

				JSONObject jsonObject = remoteDep.toJson();

				jsonObject.put("client_url", this.getUrl());

				String json = jsonObject.toString();
				
				Instance instance = createClientProxy(json,client);

				// TODO distriman: log the remote resolution information on the client side
				if (instance == null) {
					
					logger.info("dependency {} was NOT found in {}",dependency.getIdentifier(),this.getUrl());
					
					return null;
				}
				
				logger.info("dependency {} was found remotely",dependency.getIdentifier());

				Set<Implementation> impl=Collections.emptySet();
				
				return new Resolved(impl, singleton(instance));

				// TODO call this machine getUrl
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null; // TODO
	}

	private Instance createClientProxy(String jsondep,Instance client) {

		HttpURLConnection connection = null;
		PrintWriter outWriter = null;
		BufferedReader serverResponse = null;
		StringBuffer buff = new StringBuffer();
		try {
			
			System.out.println("#### Using address:"+this.getUrl());
			
			connection = (HttpURLConnection) new URL(
					this.getUrl()+"/apam/machine").openConnection();

			// SET REQUEST INFO
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);

			outWriter = new PrintWriter(connection.getOutputStream());

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

			if(sb.toString().trim().equals("")) return null;
			
			String decoded = URLDecoder.decode(sb.toString(), "UTF-8");

			System.out.println("Decoded value=" + decoded);

			JSONObject jsonResponse = new JSONObject(decoded);

			String endpointUrl = jsonResponse.getString("endpoint_url");
			String instancename = jsonResponse.getString("instance_name");
			String interfacename = jsonResponse.getString("interface_name");
			
			Class ifaceClazz=Class.forName(interfacename);
			
			Thread.currentThread().setContextClassLoader(ServerFactoryBean.class.getClassLoader());
			ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
			factory.setServiceClass(ifaceClazz);
			factory.setAddress(endpointUrl);
			
			Object proxyRaw=factory.create();
			
//			System.out.println(String.format("Client side: [instance ID:%s, endpoint:%s]", instancename,endpointUrl).toString());
//			P2Spec p2=(P2Spec)factory.create();
//			System.out.println("Proxy Instantiated:"+p2.getName());
			
			return new RemoteInstanceImpl(endpointUrl, this.getInst().getComposite(), this, proxyRaw);

		} catch (MalformedURLException mue) {
			mue.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			
			if (connection != null)
				connection.disconnect();

			if (serverResponse != null) {
				try {
					serverResponse.close();
				} catch (Exception ex) {
				}
			}
		}
		// }

		// ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
		// factory.setServiceClass(Instance.class);
		// System.out.println("******************Trying to access url:"+my_url);
		// factory.setAddress(my_url+"/ws");
		// Instance client = (Instance) factory.create();
		//
		// return client; // null
		
		
		return null;
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
		// TODO distriman: implement set property for remote instances
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
	public boolean substWire(Instance oldDestInst, Instance newDestInst,
			String depName) {
		return false;
	}

	@Override
	public void setInst(Instance asmInstImpl) {
		this.apamInstance = asmInstImpl;

	}

	@Override
	public Instance getInst() {
		return apamInstance;
	}
}
