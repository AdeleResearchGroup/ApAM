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
package fr.imag.adele.apam.distriman.provider;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.distriman.Distriman;
import fr.imag.adele.apam.distriman.discovery.MachineDiscovery;
import fr.imag.adele.apam.distriman.dto.RemoteDependency;
import fr.imag.adele.apam.impl.ApamResolverImpl;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.UUID;

import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;

/**
 * Singleton that represents the local Apam, it contains a servlet allowing for
 * the remote machines to resolves their dependency through it.
 * 
 * User: barjo Date: 13/12/12 Time: 10:26
 */
public enum LocalMachine {
	INSTANCE;

	private final String name = UUID.randomUUID().toString();
	private final String type = MachineDiscovery.MDNS_TYPE;
	private final HttpServlet servlet = new MyServlet();
	private final String path = "/apam/machine";
	static Logger logger = LoggerFactory.getLogger(LocalMachine.class);
	
	private String host = null;
	private int port = -1;
	private Distriman distriman;

	/**
	 * Initialize the machine.
	 * 
	 * @param host
	 *            The machine hostname.
	 * @param port
	 *            The http port.
	 * @param distriman
	 *            This machine Distriman.
	 */
	public void init(String host, int port, Distriman distriman) {
		assert distriman != null;
		assert (host != null);
		assert port > 0;

		if (this.host != null || this.port != -1) {
			return; // todo log
		}

		this.host = host;
		this.port = port;
		this.distriman = distriman;
	}

	/**
	 * @return The machine url path.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return The machine unique name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return The machine url hostname
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @return The machine url http port.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return The machine dns/sd type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return The machine full url
	 */
	public String getURL() {
		return "http://" + host + ":" + String.valueOf(port) + path;
	}

	/**
	 * @return The machine servlet.
	 */
	public HttpServlet getServlet() {
		return servlet;
	}

	/**
	 * HttpServlet that allows for the network machine to resolved their
	 * dependency thanks to this machine.
	 */
	private class MyServlet extends HttpServlet {
		private static final String MEDIA_TYPE = "text/html";
		// private static final String MEDIA_TYPE = "application/json";
		private static final String CLIENT_URL = "client_url";
		private final DependencyManager apamMan;

		private MyServlet() {
			// Get ApamMan in order to resolve the dependancy
			apamMan = ApamManagers.getManager(CST.APAMMAN);
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {
			// super.doGet(req, resp);
			// doPost(req, resp);
		}

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

			// Content type not supported
			// if ( !req.getContentType().equalsIgnoreCase(MEDIA_TYPE)){
			// resp.sendError(SC_UNSUPPORTED_MEDIA_TYPE);
			// return;
			// }

			// Get the content
			// StringBuilder content = new StringBuilder();
			// BufferedReader reader = req.getReader();
			// String tmp;
			// while ( (tmp = reader.readLine()) != null){
			// content.append(tmp);
			// }
			// reader.close();

			// no content
			// if (content.length() == 0){
			// resp.sendError(SC_NO_CONTENT);
			// return;
			// }

			PrintWriter writer=resp.getWriter();
			
			try {

				JSONObject requestJson = new JSONObject(req.getParameter("content"));
				RemoteDependency remoteDependency = RemoteDependency.fromJson(requestJson);
				String remoteUrl = requestJson.getString(CLIENT_URL);
				String identifier = remoteDependency.getIdentifier();
				
				logger.info("requesting resolution of the identifier {} in the address {}",identifier,remoteUrl);
				
				EndpointRegistration reg = distriman.resolveRemoteDependency(
						remoteDependency, remoteUrl);

				String jsonString=toJson(reg);
				
				logger.info("payload of the response {}",jsonString);
				
				JSONObject responseJson = new JSONObject(jsonString);
				
				writer.write(URLEncoder.encode(responseJson.toString(), "UTF-8")+"\n");
				
				writer.flush();
				
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				writer.close();
			}
			
			// PrintWriter writer = resp.getWriter(); //Write the response
			// writer.write("got it!");

			// Handle content as json.
			// try {
			// JSONObject json = new JSONObject(content.toString());
			//
			// //Get the RemoteMachine url
			// String remoteUrl = json.getString(CLIENT_URL);
			//
			// System.out.println("Received servlet call to resolve a dependency");
			//
			// //get the dependency
			// RemoteDependency dependency = RemoteDependency.fromJson(json);
			//
			// //that's the meat, ask Distriman? to resolve the dependency and
			// create the endpoint ?
			// EndpointRegistration reg =
			// distriman.resolveRemoteDependency(dependency,remoteUrl);
			//
			// PrintWriter writer = resp.getWriter(); //Write the response
			// //Cannot resolved!
			// if(reg == null){
			// resp.setStatus(204); //Return a NO CONTENT 204
			// } else {
			// resp.setStatus(200); //OK
			// resp.setContentType(MEDIA_TYPE);
			// writer.write(toJson(reg)); //Parse the EndpointRegistration
			// }
			//
			// writer.close();
			//
			// } catch (JSONException e) {
			// throw new IOException(e);
			// }
		}
	}

	private String toJson(EndpointRegistration registration)
			throws JSONException {
		JSONObject json = new JSONObject();
		json.put("endpoint_url", registration.getEndpointUrl());
		json.put("protocol", registration.getProtocol());
		json.put("instance_name", registration.getInstance().getName());
		json.put("interface_name", registration.getInterfaceCanonical());
		
		return json.toString();
	}

}
