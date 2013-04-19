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
package fr.imag.adele.apam.distriman.web;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.http.HttpService;

import fr.imag.adele.apam.distriman.client.RemoteMachine;
import fr.imag.adele.apam.distriman.discovery.ApamMachineFactory;

@Component(name = "Apam::Distriman::Provider servlet")
@Instantiate
@Provides
public class DistrimanWeb extends HttpServlet implements Servlet, ServletConfig { 

	private final static String URL = "/distriman";
	private final static String RESOURCE = "/static";

//	@Requires
//	CXFSample service;
	
	@Requires(nullable = false)
	HttpService http;

	@Requires(nullable = false)
	ApamMachineFactory discovery;

	private String html = "<html><head><title>.:Apam - Distriman:.</title> <style type='text/css'>  body {    color: black;    background-color: #d8da3d; } table, tr {	border-style: dotted; }  </style></head><body><center><strong>Available apam remote nodes</strong><table>	<tr>		<td width='200px'>			URL		</td width='100px'>		<td>			IP		</td>		<td width='50px'>			Port		</td></tr>%s</table></center></body><html>";

	@Validate
	private void initialize() {
		try {
			http.registerServlet(URL, this, null, null);
			http.registerResources(RESOURCE, "/", null);
			
//			try {
//				//service = new SampleService();
//				service.start();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
			
		} catch (Exception e) {
			e.printStackTrace();
			// throw new RuntimeException(e);
		}
	}

	@Invalidate
	private void stop() {
		http.unregister(URL);
		http.unregister(RESOURCE);
//		service.stop();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/html");
		if (discovery != null) {

//			try {
//				service.connectCall();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
			
			String conteTemplate = "<tr><tr><td>%s</td><td>%s</td><td>%s</td><tr>";

			String conte = "";

			for (String key : discovery.getMachines().keySet()) {
				RemoteMachine machine = discovery.getMachines().get(key);
				conte += String.format(conteTemplate, machine.getURLServlet(), "NONE",
						"NONE");
			}

			if (discovery.getMachines().size() == 0) {
				conte += "<tr><td colspan='3' align='center'>No remote nodes available</td><tr>";
			}

			resp.getWriter().write(String.format(html, conte));

		} else {
			resp.getWriter().write("empty discovery");
		}

		resp.flushBuffer();

	}
}
