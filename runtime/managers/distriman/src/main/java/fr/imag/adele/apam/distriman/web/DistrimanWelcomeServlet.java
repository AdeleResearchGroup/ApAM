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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.distriman.NodePool;
import fr.imag.adele.apam.distriman.RemoteMachine;

@Component
@Provides
@Instantiate
public class DistrimanWelcomeServlet extends HttpServlet implements DistrimanServlet{

	BundleContext context;
	
	public DistrimanWelcomeServlet() {
	}
	
	public DistrimanWelcomeServlet(BundleContext context) {
		this.context=context;
	}

	@Requires
	NodePool discovery;

	private String html="<html><head><title>.:Apam - Distriman:.</title> <style type='text/css'>  body {    color: black;    background-color: #d8da3d; } table, tr {	border-style: dotted; }  </style></head><body><center><strong>Available apam remote nodes</strong><table>	<tr>		<td width='200px'>			URL		</td width='100px'>		<td>			IP		</td>		<td width='50px'>			Port		</td></tr>%s</table></center></body><html>";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/html");
		if (discovery != null) {

			//System.out.println("called servlet");

			String conteTemplate="<tr><tr><td>%s</td><td>%s</td><td>%s</td><tr>";
			
			String conte="";
			
			for (String key : discovery.getMachines().keySet()) {
				RemoteMachine machine = discovery.getMachines().get(key);
				//resp.getWriter().write(machine.getUrl() + " ");
				conte+=String.format(conteTemplate, machine.getUrl(),"NONE","NONE");
				
			}

			if (discovery.getMachines().size() == 0) {
				conte="<tr><td colspan='3' align='center'>No remote nodes available</td><tr>";
			}
			
			resp.getWriter().write(String.format(html,conte));

			resp.flushBuffer();

			super.doGet(req, resp);

		} else {
			//resp.getWriter().write("empty discovery");
		}
	}

	
	
}
