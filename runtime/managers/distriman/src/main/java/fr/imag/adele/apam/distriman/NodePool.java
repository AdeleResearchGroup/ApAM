package fr.imag.adele.apam.distriman;

import java.util.Map;

public interface NodePool {

	public RemoteMachine newRemoteMachine(String url);
	public RemoteMachine destroyRemoteMachine(String url);
	public Map<String, RemoteMachine> getMachines();
}
