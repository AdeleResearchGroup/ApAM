package fr.imag.adele.apam.test.support.distriman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

import junit.framework.Assert;

import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import fr.imag.adele.apam.pax.distriman.test.iface.P2Spec;

public class DistrimanUtil {

	public static String httpRequestDependency(String id, String type, String clazz,
			String variable, boolean isMultiple, String clientUrl) {

		StringBuffer payload = new StringBuffer();

		//final String jsonPayload = " {\"id\":\"p2\",\"rref\":{\"name\":\"fr.imag.adele.apam.pax.distriman.test.iface.P2Spec\",\"type\":\"itf\"},\"component_name\":\"P1\",\"is_multiple\":false,\"client_url\":\"http://127.0.0.1:8081/apam/machine\"}";
		
		payload.append("{");
		payload.append(String.format("\"id\":\"%s\",", id));
		payload.append(String.format(
				"\"rref\": {\"name\":\"%s\",\"type\":\"%s\"},", clazz,type));
		payload.append(String.format("\"component_name\":\"%s\",", variable));
		payload.append(String.format("\"is_multiple\":\"%s\",", isMultiple));
		payload.append(String.format("\"client_url\":\"%s\"", clientUrl));
		payload.append("}");

		return payload.toString();

	}

	public static void waitForUrl(long maxwait, String url) {

		boolean connectionStablished = false;
		long start = System.currentTimeMillis();
		long current = 0;

		while (!connectionStablished) {

			try {
				current = System.currentTimeMillis();
				
				if((current-start)>maxwait) break;
				
				HttpURLConnection connection = (HttpURLConnection) new URL(url)
						.openConnection();

				connectionStablished = true;

				Thread.sleep(1000);

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public static String curl(Map<String, String> parameters, String url)
			throws IOException {

		HttpURLConnection connection = null;
		PrintWriter outWriter = null;
		BufferedReader serverResponse = null;
		StringBuffer output = new StringBuffer();

		connection = (HttpURLConnection) new URL(url).openConnection();

		// SET REQUEST INFO
		connection.setRequestMethod("GET");
		connection.setDoOutput(true);

		outWriter = new PrintWriter(connection.getOutputStream());

		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			output.append(String.format("%s=%s", entry.getKey(),
					URLEncoder.encode(entry.getValue(), "UTF-8")).toString());
		}

		outWriter.write(output.toString());
		outWriter.flush();

		serverResponse = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));

		String line;
		StringBuffer sb = new StringBuffer();

		while ((line = serverResponse.readLine()) != null) {
			sb.append(line);
		}

		return URLDecoder.decode(sb.toString(),"UTF-8");

	}
	
	public static void endpointConnect(Map<String, String> endpoints) {

		for (Map.Entry<String, String> entry : endpoints.entrySet()) {

			try {

				ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
				factory.setServiceClass(Class.forName(entry.getKey()));
				factory.setAddress(entry.getValue());
				
				Object proxy = (Object) factory.create();
				System.err.println(proxy.toString());

			} catch (Exception e) {
				Assert.fail(String
						.format("distriman(provider host) created an endpoint but was not possible to connect to it, failed with the message %s",
								e.getMessage()));
			}

		}

	}
	
	public static Map<String,String> endpointGet(String jsonstring) throws JsonParseException, JsonMappingException, IOException{
		
		ObjectMapper om=new ObjectMapper();
		
		JsonNode node=om.readValue(jsonstring, JsonNode.class);
		
		Map<String,String> endpoints=om.convertValue(node.get("endpoint_entry"), new TypeReference<Map<String, String>>() {});
		
		System.err.println("Class\tURL");
		for(Map.Entry<String, String> entry:endpoints.entrySet()){
			System.err.println(String.format("%s\t%s", entry.getKey(),entry.getValue()));
		}
		
		return endpoints;
	}

}
