package fr.imag.adele.apam.distriman.web;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIMessageImpl extends UnicastRemoteObject implements RMIMessage {

	protected RMIMessageImpl() throws RemoteException {
		super();
	}

	@Override
	public RMICustomType sayHello(String name) throws RemoteException {
		
		System.out.println("Server Site: Hello "+name);
		
		RMICustomType my=new RMICustomType();
		my.value="new value for"+name;
		
		return my;
		
	}

	
}
