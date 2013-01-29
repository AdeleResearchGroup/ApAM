package fr.imag.adele.apam.distriman.web.samples;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIMessage extends Remote {
	public RMICustomType sayHello(String name) throws RemoteException;
}
