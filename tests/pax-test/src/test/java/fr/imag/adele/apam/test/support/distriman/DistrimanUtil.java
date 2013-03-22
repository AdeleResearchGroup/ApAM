package fr.imag.adele.apam.test.support.distriman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class DistrimanUtil {

	public static String curl(Map<String,String> parameters, String url) throws IOException{
		
		HttpURLConnection connection = null;
		PrintWriter outWriter = null;
		BufferedReader serverResponse = null;
		StringBuffer output = new StringBuffer();

		connection = (HttpURLConnection) new URL(url).openConnection();

		// SET REQUEST INFO
		connection.setRequestMethod("GET");
		connection.setDoOutput(true);

		outWriter = new PrintWriter(connection.getOutputStream());
		
		for(Map.Entry<String, String> entry:parameters.entrySet()){
			output.append(String.format("%s=%s",entry.getKey(),URLEncoder.encode(entry.getValue(), "UTF-8")).toString());
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
