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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.distriman.Distriman;
import fr.imag.adele.apam.distriman.discovery.MachineDiscovery;
import fr.imag.adele.apam.distriman.dto.RemoteDependencyDeclaration;

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

		if (this.host != null || this.port != -1) {
			logger.info("trying to change host name or port address from {}:{} to {}:{}",new Object[]{this.host,this.port,host,port});
			return; 
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
	
		private final DependencyManager apamMan;

		private MyServlet() {
			// Get ApamMan in order to resolve the dependancy
			apamMan = ApamManagers.getManager(CST.APAMMAN);
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {
			//resp.sendError(204);
			doPost(req, resp);
		}

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {

			PrintWriter writer=resp.getWriter();
			
			try {
								
				String content=URLDecoder.decode(req.getParameter("content"), "UTF-8");
				
				ObjectMapper om=new ObjectMapper();
				
				JsonNode requestJson=om.readValue(content, JsonNode.class);
				
				RemoteDependencyDeclaration remoteDependency = RemoteDependencyDeclaration.fromJson(requestJson);
				String identifier = remoteDependency.getIdentifier();
				
				logger.info("requesting resolution of the identifier {} in the address {}",identifier,remoteDependency.getClientURL());
				
				EndpointRegistration reg = distriman.resolveDependencyLocalMachine(
						remoteDependency, remoteDependency.getClientURL());
				
				String jsonString=toJson(reg);
				
				logger.info("payload of the response {}",jsonString);
				
				writer.write(URLEncoder.encode(jsonString.toString(), "UTF-8")+"\n");
				
				writer.flush();
				
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				writer.close();
			}

		}
	}

	private String toJson(EndpointRegistration registration) throws JsonGenerationException, JsonMappingException, IOException {
		
		ObjectMapper om=new ObjectMapper();
		
		ObjectNode node=om.createObjectNode();
		
		ObjectNode nodeendpoint=om.createObjectNode();
		
		for (Map.Entry<String, String> entry: registration.getEndpoint().entrySet()) {
			nodeendpoint.put(entry.getKey(), entry.getValue());
		}
		
		node.put("endpoint_entry", nodeendpoint);
		node.put("protocol", registration.getProtocol());
		node.put("instance_name", registration.getInstance().getName());
		
		return node.toString();
	}

}
