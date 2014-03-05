package fr.imag.adele.apam.device.door.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Unbind;
import org.ow2.chameleon.fuchsia.core.declaration.ImportDeclaration;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.apform.ApformInstance;
import fr.imag.adele.apam.impl.InstanceImpl;

@Component
@Instantiate
public class SimulatedDoorFactory {

	
	/**
	 * The list of events registered before binding Apam
	 */
	private Set<ImportDeclaration> pending = new HashSet<ImportDeclaration>();

	/**
	 * The list of discovered declarations to process
	 * 
	 */
	private Set<ImportDeclaration> discovered = new HashSet<ImportDeclaration>();

	/**
	 * The created proxies
	 */
	private Map<String,ApformInstance> proxies = new HashMap<String,ApformInstance>();

    /**
     * The event executor. We use a pool of a threads to notify APAM of underlying platform events,
     * without blocking the platform thread.
     */
    static private final Executor executor = Executors.newCachedThreadPool();

    /**
     * The APAM resolver used to locate the proxy factory
     */
	private ApamResolver resolver;

	@Bind(id="apam")
	private void apamBound(Apam apam) {
		
		resolver = CST.apamResolver;
		
		/*
		 * Schedule pending device discovery requests
		 */
		for (ImportDeclaration declaration : pending) {
			executor.execute(new DeclarationDiscoveryProcessing(declaration));
		}
		
		pending.clear();
		
	}
	
	@Unbind(id="apam")
	private void apamUnbound(Apam apam) {
		resolver = null;
	}

	
	/**
	 * The device identifier associated to the declaration
	 *
	 */
	private final String getId(ImportDeclaration declaration) {
		return (String) declaration.getMetadata().get("ard.door.id");
	}

	/**
	 * Whether the declaration matches this factory
	 *
	 */
	private final boolean matches(ImportDeclaration declaration) {
		return getId(declaration) != null;
	}
	
	
	@Bind(id="declaration", aggregate=true)
	private void addDeclaration(ImportDeclaration declaration) {
		
		if (! matches(declaration))
			return;
		
		/*
		 * first we update synchronously the discovery table, so we can respect the order of events
		 * (bound/unbound) for each declaration
		 */
		synchronized (discovered) {
			discovered.add(declaration);
		}

		/*
		 * If APAM is not available register the pending request
		 */
		synchronized (this) {
			if (resolver == null) {
				pending.add(declaration);
				return;
			}
		}
		
		/*
		 * Create proxy asynchronously
		 */
		executor.execute(new DeclarationDiscoveryProcessing(declaration));
		
	}

	@Unbind(id="declaration")
	private void removeDeclaration(ImportDeclaration declaration) {

		if (! matches(declaration))
			return;
		
		/*
		 * If APAM is not available, just remove the pending request
		 */
		synchronized (this) {
			if (resolver == null)
				pending.remove(declaration);
		}
		
		/*
		 * first we update synchronously the discovery table, so we can respect the order of events
		 * (bound/unbound) for each declaration
		 */
		
		ApformInstance proxy = null;
		
		synchronized (discovered) {
			discovered.remove(declaration);
			proxy = proxies.remove(getId(declaration));
		}
		
		executor.execute(new DeclarationLostProcessing(proxy));

	}

	
	private class DeclarationDiscoveryProcessing implements Runnable {

		
		/**
		 * The discovered declaration
		 */
		private final ImportDeclaration declaration;
			
		
		public DeclarationDiscoveryProcessing(ImportDeclaration declaration) {
			this.declaration 	= declaration;
		}
		
		@Override
		public void run() {

			/*
			 * IMPORTANT Because we are processing this event asynchronously, we need to verify that the declaration is
			 * still available, and abort the processing as soon as possible.
			 */
			synchronized (discovered) {
				if (! discovered.contains(declaration))
					return;
			}
			
			/*
			 * IMPORTANT Because we are processing this event asynchronously, we need to verify that APAM is
			 * still available, and abort the processing as soon as possible.
			 */
			synchronized (this) {
				if (resolver == null) {
					pending.add(declaration);
					return;
				}
					
			}


			/*
			 * Look for an implementation 
			 */
			
			Implementation implementation = resolver.findImplByName(null, "SimulatedAccessDoor");
			
			if (implementation == null) {
				System.err.println("[Simulated door factory] Proxy not found for declaration  "+declaration);
				return;
			}
			
			try {
				
	
				/*
				 * Create an instance of the proxy, and configure it for the appropriate door id
				 */
	
				Map<String,Object> configuration = new Hashtable<String,Object>();
				configuration.put("doorId",getId(declaration));
	
				ApformInstance proxy = implementation.getApformImpl().addDiscoveredInstance(configuration);
				
				/*
				 * Ignore errors creating the proxy
				 */
				if (proxy == null) {
					System.err.println("[Simulated door factory] Proxy could not be instantiated  "+implementation.getName());
				}
				else {
					/*
					 * Update the service map
					 */
					synchronized (discovered) {
						
						/*
						 * If the declaration is no longer available, just dispose the created proxy and abort processing 
						 */
						if (! discovered.contains(declaration)) {
							if (proxy.getApamComponent() != null)
								((InstanceImpl)proxy.getApamComponent()).unregister();
							return;
						}
		
						/*
						 * otherwise add it to the map
						 */
						proxies.put(getId(declaration),proxy);
					}
				}
	
			} catch (Exception e) {
				System.err.println("[Simulated door factory] Proxy could not instantiated  "+implementation.getName());
				e.printStackTrace();
			}
			
		}
		
	}
	
	
	private class DeclarationLostProcessing implements Runnable {

		/**
		 * The instance to dispose
		 */
		private final ApformInstance proxy;
		
		public DeclarationLostProcessing(ApformInstance proxy) {
			this.proxy = proxy;
		}
		
		@Override
		public void run() {
			if (proxy != null && proxy.getApamComponent() != null)
				((InstanceImpl)proxy.getApamComponent()).unregister();
		}
		
	}


}
