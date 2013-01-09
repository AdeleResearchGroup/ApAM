package fr.imag.adele.apam.distriman;

	import java.io.*;
import java.net.*;
import java.util.*;
import static java.lang.System.out;

	public class ListNets {

	    public static void main(String args[]) throws IOException {
	        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
	        for (NetworkInterface netint : Collections.list(nets))
	            displayInterfaceInformation(netint);
	    }

	    static void displayInterfaceInformation(NetworkInterface netint) throws IOException {
	        out.printf("Display name: %s\n", netint.getDisplayName());
	        out.printf("Name: %s\n", netint.getName());
	        
	        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
	        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
	        	
	        	if(inetAddress.isLoopbackAddress()|!inetAddress.isReachable(3000)) continue;
	        	
	            //out.printf("InetAddress: %s\n", inetAddress);
	            out.printf("Hostaddress: %s\n\n", inetAddress.getHostAddress());
	            
	            
	        }
	        
	        out.printf("\n");
	     }
	} 
	