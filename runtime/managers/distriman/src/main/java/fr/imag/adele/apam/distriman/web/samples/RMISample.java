package fr.imag.adele.apam.distriman.web.samples;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.Callable;

import org.apache.cxf.endpoint.Server;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleException;
import org.osgi.service.http.HttpService;

@Component
@Instantiate
@Provides
public class RMISample {

	@Requires(optional = false)
	private HttpService http;

	Registry registry;

	Server server;

	public void start() {

		try {

			// if (System.getSecurityManager() == null)
			// {
			// System.setSecurityManager(new RMISecurityManager());
			// System.out.println("Security manager installed.");
			// }

			registry = LocateRegistry.createRegistry(1100);

			registry.rebind("myMessageSuper", new RMIMessageImpl());

			System.out.println("RMI Bind ready");

		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void connectCall() {

		try {
			Registry myRegistry = LocateRegistry.getRegistry(1100);
			RMIMessage impl = (RMIMessage) myRegistry.lookup("myMessageSuper");
			RMICustomType current = impl.sayHello("Jander");

			System.out.println("Client side:" + current.value);

			System.out.println("Message Sent");
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void stop() {
		try {
			registry.unbind("myMessageSuper");
		} catch (AccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
