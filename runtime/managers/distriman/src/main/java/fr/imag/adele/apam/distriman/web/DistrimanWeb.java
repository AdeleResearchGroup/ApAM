package fr.imag.adele.apam.distriman.web;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.http.HttpService;

@org.apache.felix.ipojo.annotations.Component(name = "Apam::Distriman Web")
@Instantiate
public class DistrimanWeb {

	private final static String URL="/hello";
	
	@Requires(nullable = false, optional = false)
	HelloWorldIface world;

	@Requires(optional = false)
	HttpService http;

	@Validate
	private void init() {
		try {
			http.registerServlet(URL, world, null, null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Invalidate
	private void stop() {
		http.unregister(URL);
	}
}
