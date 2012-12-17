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

	public DistrimanWelcomeServlet() {
	}
	
	public DistrimanWelcomeServlet(BundleContext context) {

	}

	@Requires
	NodePool discovery;

	private String html="<html><head><title>.:Apam - Distriman:.</title> <style type='text/css'>  body {    color: black;    background-color: #d8da3d; } table, tr {	border-style: dotted; }  </style></head><body><center><strong>List of available apam nodes</strong><table>	<tr>		<td width='200px'>			URL		</td width='100px'>		<td>			IP		</td>		<td width='50px'>			Port		</td></tr>%s</table></center></body><html>";
	
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
				conte="<tr><td colspan='3'>No discovery nodes available</td><tr>";
			}
			
			resp.getWriter().write(String.format(html,conte));

			resp.flushBuffer();

			super.doGet(req, resp);

		} else {
			//resp.getWriter().write("empty discovery");
		}
	}

	
	
}
