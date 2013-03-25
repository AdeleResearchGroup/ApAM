package fr.imag.adele.apam.test.support.distriman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class DistrimanUtil {

	public static String httpRequestDependency(String id, String clazz,
			String variable, boolean isMultiple, String clientUrl) {

		StringBuffer payload = new StringBuffer();

		//final String jsonPayload = " {\"id\":\"p2\",\"rref\":{\"name\":\"fr.imag.adele.apam.pax.distriman.test.iface.P2Spec\",\"type\":\"itf\"},\"component_name\":\"P1\",\"is_multiple\":false,\"client_url\":\"http://127.0.0.1:8081/apam/machine\"}";
		
		payload.append("{");
		payload.append(String.format("\"id\":\"%s\",", id));
		payload.append(String.format(
				"\"rref\": {\"name\":\"%s\",\"type\":\"itf\"},", clazz));
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

		return sb.toString();

	}

}
