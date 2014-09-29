package fr.imag.adele.apam.application.lock;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpService;

import fr.imag.adele.apam.AttrType;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.util.Attribute;

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
		sendState(response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		setProperties(request);
		sendState(response);
	}

	private void sendState(HttpServletResponse response) throws ServletException, IOException  {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		
		PrintWriter builder = response.getWriter(); 
		encodeProperties(builder);
		builder.flush();
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

	private void encodeProperties(PrintWriter builder) {
		
		builder.append('{');
		
		boolean first = true;
		for (Map.Entry<String,Object> property : application.getAllProperties().entrySet()) {
			
			
			PropertyDefinition definition = application.getPropertyDefinition(property.getKey());
			
			/*
			 * skip internal properties
			 */
			if (definition == null)
				continue;

			if (!first)
				builder.append(',');

			builder.append('"').append(property.getKey()).append('"');
			builder.append(':');
			encodeAttribute(builder,definition,property.getValue());
			
			first = false;
		}

		builder.append('}');
		
	}
	
	@SuppressWarnings("unchecked")
	private void encodeAttribute(PrintWriter builder, PropertyDefinition definition, Object value) {
		
		/*
		 * multi-valued properties are already stored as set of strings
		 */
		if (definition.isSet()) {
			
			builder.append('[');
			boolean first = true;
			for (String element : ((Set<String>) value)) {
				if (! first)
					builder.append(',');
				
				builder.append(element);
				first = false;
			};
			builder.append(']');
			return;
		}
		
		/*
		 * for other types encode depending on the type of property
		 */
		switch(Attribute.splitType(definition.getType()).type) {
			case AttrType.INTEGER :
				builder.append(((Integer)value).toString());
				break;
			case AttrType.BOOLEAN :
				builder.append(((Boolean)value).toString());
				break;
			case AttrType.FLOAT :
				builder.append(((Float)value).toString());
				break;
			case AttrType.STRING :
			case AttrType.ENUM :
			case AttrType.VERSION :
			default :
				builder.append('"').append(value.toString()).append('"');
				break;
		}

	}
	
	public void dispose() {
		webContainer.unregister("/DayNightApplication");
		webContainer.unregister("/DayNightApplication/REST");	}
}
