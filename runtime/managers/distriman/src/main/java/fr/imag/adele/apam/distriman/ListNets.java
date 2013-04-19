package fr.imag.adele.apam.distriman;

import java.io.*;
import java.net.*;
import java.util.*;

import static java.lang.System.out;

public class ListNets {

	private static ListNets instance;

	private ListNets(){}
	
	public static void main(String args[]) throws IOException {
		ListNets.getInstance().getAddresses();
	}

	public List<InetAddress> getAddresses() {
		// out.printf("Display name: %s\n", netint.getDisplayName());
		// out.printf("Name: %s\n", netint.getName());

		List<InetAddress> addresses = new ArrayList<InetAddress>();

		try {
			out.printf("<ifaces>\n");
			for (NetworkInterface ni :  Collections.list(NetworkInterface.getNetworkInterfaces())) {

				for (InetAddress inetAddress : Collections.list(ni.getInetAddresses())) {
					if (!inetAddress.isReachable(3000)
							|| inetAddress instanceof Inet6Address 
							|| !inetAddress.isLoopbackAddress())
						continue;

					// out.printf("InetAddress: %s\n", inetAddress);
					out.printf("Hostaddress: %s\n", inetAddress.getHostAddress());
					addresses.add(inetAddress);
				}
			}
			out.printf("</ifaces>\n");
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		out.printf("\n");
		
		return addresses;
		
	}

	public static ListNets getInstance() {
		if (instance == null)
			instance = new ListNets();
		return instance;
	}
}
