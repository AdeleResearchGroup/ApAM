package fr.imag.adele.apam.application.lock;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpService;

import fr.imag.adele.apam.Instance;

public class WebInterface extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final HttpService	webContainer;
	private final Instance 		application;
	
	public WebInterface(HttpService webContainer, Instance application) {
		this.webContainer = webContainer;
		this.application	= application;
		
		try {
			webContainer.registerResources("/DayNightApplication","WEB-INF",null);
			webContainer.registerServlet("/DayNightApplication/REST",this,null,null);
		} catch (Exception ignored) {
			ignored.printStackTrace();
		}
		
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		setProperties(request);
		
		response.setContentType("application/x-www-form-urlencoded;charset=UTF-8");
		response.getWriter().println(getEncodedProperties());
		response.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		setProperties(request);
		response.setStatus(HttpServletResponse.SC_OK);
	}
	
	@SuppressWarnings("unchecked")
	private void setProperties(HttpServletRequest request) {
		for (Map.Entry<String,String[]> parameter : (Set<Map.Entry<String,String[]>>) request.getParameterMap().entrySet()) {
			if (application.getPropertyDefinition(parameter.getKey()) != null) {
				application.setProperty(parameter.getKey(),parameter.getValue()[0]);
			}
		}
		
	}

	private String getEncodedProperties() {
		
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String,String> property : application.getAllPropertiesString().entrySet()) {
			if (builder.length() > 0)
				builder.append('&');
			builder.append(property.getKey()).append('=').append(property.getValue());
		}

		try {
			return URLEncoder.encode(builder.toString(),"UTF-8");
		} catch (UnsupportedEncodingException ignored) {
			return builder.toString();
		}
	}
	
	
	public void dispose() {
		webContainer.unregister("/DayNightApplication");
		webContainer.unregister("/DayNightApplication/REST");	}
}
