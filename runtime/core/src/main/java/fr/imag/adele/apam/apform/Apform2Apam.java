/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.apam.apform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;

public class Apform2Apam {

	/**
	 * A description of a thread  that is currently waiting for some processing to be finished
	 * by APAM
	 * 
	 * @author vega
	 *
	 */
	public static abstract class PendingThread {

		protected final String description;

		protected List<StackTraceElement> stack;

		protected PendingThread(String description) {
			this.description = description;
		}

		/**
		 * The request description
		 */
		public String getDescription() {
			return description;
		}

		/**
		 * The condition that is being waiting by the thread
		 */
		public abstract String getCondition();
		
		/**
		 * The stack of the thread waiting for processing in APAM
		 */
		public List<StackTraceElement> getStack() {
			return stack;
		}

	}

	/**
	 * This interface represents information regarding the current status of the underlying platform
	 * 
	 * @author vega
	 *
	 */
	public interface Platform {
		
		/**
		 * Whether there is pending declarations, being currently deployed, in the platform
		 */
		public boolean hasPendingDeclarations();
		
		/**
		 * Blocks the current thread until all pending declarations are processed
		 */
		public void waitForDeclarations();
		
		/**
		 * Get the list of threads waiting for pending declarations at the platform level
		 */
		public List<? extends PendingThread> getPending();
	}


	private static Platform platform = null;
	
	/**
	 * The list of threads waiting for some declaration processing in the underlying platform
	 */
	public static List<PendingThread> gePlatformWaitingThreads() {
		return Collections.unmodifiableList(getPlatform().getPending());
	}
	
	/**
	 * Get access to information regarding the underlying platform
	 */
	private static Platform getPlatform()  {
		
		synchronized (Apform2Apam.class) {
			try {
				while (Apform2Apam.platform == null)
					Apform2Apam.class.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return Apform2Apam.platform;
	}
	
	/**
	 * This method is invoked by the underlying platform, to give controlled access to its internal
	 * status 
	 */
	public static void setPlatform(Platform platform) {
		synchronized (Apform2Apam.class) {
			Apform2Apam.platform = platform;
			Apform2Apam.class.notifyAll();
		}
	}

	/**
	 * Wait for a component to be deployed
	 */
	public static void waitForComponent(String componentName) {
		waitForComponent(componentName, 0);
	}

	/**
	 * The components that clients are waiting for deployment to complete
	 */
	private static Set<String> expectedComponents = new HashSet<String>();

	/**
	 * Wait for a component to be deployed
	 */
	public static void waitForComponent(String componentName, long timeout) {

		synchronized (Apform2Apam.expectedComponents) {

			/*
			 * Last verification before blocking. The expected component could
			 * have been added to APAM between the moment we checked and this
			 * method call.
			 * 
			 * NOTE notice that the check is inside the synchronized block to
			 * avoid race conditions with appearing components.
			 * 
			 * TODO perhaps this code should be refactored into the broker, so
			 * that validation and blocking can be done atomically
			 */
			if (CST.componentBroker.getComponent(componentName) != null) {
				return;
			}

			ComponentWaitingThread current = getCurrent();
			Apform2Apam.expectedComponents.add(componentName);
			current.pending(componentName);

			/*
			 * long startWaiting = System.currentTimeMillis();
			 */

			try {
				while (Apform2Apam.expectedComponents.contains(componentName)) {
					Apform2Apam.expectedComponents.wait(timeout);

					/*
					 * NOTE current implementation actually waits forever, even
					 * if it wakes up at the timeout expiration. However if we
					 * change this, most of the time this cause errors because
					 * findByName return a null component, and this is not
					 * systematically tested.
					 * 
					 * TODO Either remove timeout or check component after
					 * calling finfByName.
					 * 
					 * long elapsed = System.currentTimeMillis() - startWaiting;
					 * if (elapsed > timeout) return;
					 */
				}
			} catch (InterruptedException interrupted) {
				interrupted.printStackTrace();
			} finally {
				current.resumed();
			}
			return;
		}
	}

	/**
	 * The list of threads waiting for a component to be deployed in APAM
	 */
	static private final List<ComponentWaitingThread> pending = new ArrayList<ComponentWaitingThread>();

	/**
	 * The list of threads waiting for a component in APAM
	 */
	public static List<ComponentWaitingThread> getComponentWaitingThreads() {
		return Collections.unmodifiableList(pending);
	}
	
	/**
	 * The thread that is making the request to wait for a component 
	 */
	static private final ThreadLocal<ComponentWaitingThread> current = new ThreadLocal<ComponentWaitingThread>();


	/**
	 * The thread that is currently executing inside APAM, an that needs to wait for a component
	 * 
	 */
	private static ComponentWaitingThread getCurrent() {
		ComponentWaitingThread currentRequest = current.get();
		if (currentRequest == null) {
			currentRequest = new ExternallWaitingThread();
			currentRequest.started();
		}
		return currentRequest;
	}

	/**
	 * The stack of the thread currently executing inside APAM, and that is going to wait for a component
	 * 
	 */
	private static List<StackTraceElement> getCurrentStack() {

		List<StackTraceElement> stack = new ArrayList<StackTraceElement>(Arrays.asList(new Throwable().getStackTrace()));

		/*
		 * Remove ourselves from the top of the stack, to increase the
		 * readability of the stack trace
		 */
		Iterator<StackTraceElement> frames = stack.iterator();
		while (frames.hasNext()) {
			if (frames.next().getClassName().startsWith(Apform2Apam.class.getName())) {
				frames.remove();
			}

			break;
		}
		return stack;
	}

	/**
	 * A description of thread that needs to wait for the reification of a component in Apam, 
	 * 
	 * @author vega
	 */
	private static abstract class ComponentWaitingThread extends PendingThread {

		private boolean isProcessing;
		private String requiredComponent;

		protected ComponentWaitingThread(String description) {
			super(description);
			this.isProcessing = false;
		}

		/**
		 * The required component
		 */
		@SuppressWarnings("unused")
		public String getRequiredComponent() {
			return requiredComponent;
		}

		/**
		 * The condition this thread is waiting for
		 */
		public String getCondition() {
			return "component "+ requiredComponent;
		}

		/**
		 * Mark this thread as started processing on behalf of APAM
		 */
		protected void started() {
			isProcessing = true;
			current.set(this);
		}

		/**
		 * Whether this tread is processing on behalf of APAM
		 */
		@SuppressWarnings("unused")
		public boolean isProcessing() {
			return isProcessing;
		}

		/**
		 * Mark this thread as finished processing in APAM
		 */
		protected void finished() {
			isProcessing = false;
			current.remove();
		}


		/**
		 * Mark this thread as pending for a component
		 */
		protected void pending(String requiredComponent) {
			this.requiredComponent = requiredComponent;
			this.stack = getCurrentStack();
			pending.add(this);
		}

		/**
		 * Whether this thread is pending
		 */
		@SuppressWarnings("unused")
		public boolean isPending() {
			return pending.contains(this);
		}

		/**
		 * Mark this request as resumed after the requested component is found
		 */
		protected void resumed() {
			this.requiredComponent = null;
			this.stack = null;

			pending.remove(this);
		}


		@Override
		public String toString() {
			return description;
		}
	}

	/**
	 * A thread that waits for a component, but is not part of the pool of threads used in to handle
	 * apform event.
	 * 
	 * These are external threads that execute APAM requests that are finished as soon as they are
	 * satisfied.
	 * 
	 * @author vega
	 * 
	 */
	private static class ExternallWaitingThread extends ComponentWaitingThread {

		public ExternallWaitingThread() {
			super("Thread " + Thread.currentThread().getName());
		}

		/**
		 * Automatically finish the request when resumed
		 */
		@Override
		protected void resumed() {
			super.resumed();
			finished();
		}
	}

	/**
	 * A new implementation, represented by object "client" just appeared in the
	 * platform.
	 * 
	 */
	public static void newImplementation(ApformImplementation client) {
		Apform2Apam.executor.execute(new ImplementationDeploymentProcessing(client));
	}

	/**
	 * A new instance, represented by object "client" just appeared in the
	 * platform.
	 */
	public static void newInstance(ApformInstance client) {
		Apform2Apam.executor.execute(new InstanceAppearenceProcessing(client));
	}

	/**
	 * A new specification, represented by object "client" just appeared in the
	 * platform.
	 * 
	 */
	public static void newSpecification(ApformSpecification client) {
		Apform2Apam.executor.execute(new SpecificationDeploymentProcessing(client));
	}

	/**
	 * The event executor. We use a pool of a threads to handle notification to
	 * APAM of underlying platform events, without blocking the platform thread.
	 */
	static private final Executor executor = Executors.newCachedThreadPool();


	/**
	 * A request from apform to add a component to APAM, this is executed asynchronously and may block waiting
	 * for other required components.
	 * 
	 * @author vega
	 * 
	 */

	private abstract static class ComponentAppearenceRequest extends ComponentWaitingThread implements Runnable {

		private final ApformComponent component;

		protected ComponentAppearenceRequest(ApformComponent component) {
			super("Adding component " + component.getDeclaration().getName());
			this.component = component;
			
			synchronized (Apform2Apam.reifying) {
				reifying.add(component.getDeclaration().getReference().getName());
			}
		}

		/**
		 * The component that needs to be reified in APAM
		 * 
		 * @return
		 */
		public ApformComponent getComponent() {
			return component;
		}

		/**
		 * The required components that need to be already refified in APAM as a
		 * requisites to start reifying this component
		 */
		protected abstract List<ComponentReference<?>> getRequirements();

		/**
		 * Notify any threads waiting for the deployment of a component
		 */
		private void notifyDeployment(Component component) {

			synchronized (Apform2Apam.expectedComponents) {
				/*
				 * If it is expected wake up all threads blocked in
				 * waitForComponent
				 */
				if (Apform2Apam.expectedComponents.contains(component.getName())) {
					Apform2Apam.expectedComponents.remove(component.getName());
					Apform2Apam.expectedComponents.notifyAll();
				}
			}

		}

		/**
		 * The method that reifies the apform component in APAM
		 */
		protected abstract Component reify();

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			try {
				started();

				/*
				 * Wait for required components
				 */
				for (ComponentReference<?> requirement : getRequirements()) {
					CST.componentBroker.getWaitComponent(requirement.getName());
				}

				/*
				 * perform reification and notify after completion
				 */
				Component apamReification = reify();
				if (apamReification != null) {
					notifyDeployment(apamReification);
				}
			} catch (Exception unhandledException) {
				logger.error("Error handling Apform event :", unhandledException);
			} finally {
				
				synchronized (Apform2Apam.reifying) {
					reifying.remove(component.getDeclaration().getReference().getName());
				}

				finished();
			}

		}

	}

	/**
	 * The list of declarations that are already processed by the underlying platform, but are currently
	 * in the process of being reified in APAM
	 */
	private static Set<String> reifying = new HashSet<String>();
	
	/**
	 * Test whether a component is currently being reified
	 */
	public static boolean isReifying(String component) {

		if (component == null)
			return false;
		
		/*
		 * Wait for platform to finish deploying declarations 
		 */
		if (getPlatform().hasPendingDeclarations())
			getPlatform().waitForDeclarations();

		/*
		 * Check if the specified component declaration is currently being 
		 * processed
		 */

		synchronized (reifying) {
			return reifying.contains(component);
		}
		
	}


	/**
	 * Task to handle implementation deployment
	 * 
	 * @author vega
	 * 
	 */
	private static class ImplementationDeploymentProcessing extends ComponentAppearenceRequest {

		public ImplementationDeploymentProcessing(ApformImplementation implementation) {
			super(implementation);
		}

		@Override
		public ApformImplementation getComponent() {
			return (ApformImplementation) super.getComponent();
		}

		@Override
		protected List<ComponentReference<?>> getRequirements() {

			List<ComponentReference<?>> required = new ArrayList<ComponentReference<?>>();
			ComponentReference<?> specification = getComponent().getDeclaration().getSpecification();
			if (specification != null) {
				required.add(specification);
			}

			/*
			if (getComponent() instanceof ApformCompositeType) {
				ApformCompositeType composite = (ApformCompositeType) getComponent();
				ComponentReference<?> main = composite.getDeclaration().getMainComponent();
				if (main != null) {
					required.add(main);
				}
			}
			 */
			
			return required;
		}

		@Override
		public Component reify() {
			return CST.componentBroker.addImpl(null, getComponent());
		}

	}

	/**
	 * Task to handle instance deployment
	 * 
	 * 
	 * @author vega
	 * 
	 */
	private static class InstanceAppearenceProcessing extends ComponentAppearenceRequest {

		public InstanceAppearenceProcessing(ApformInstance instance) {
			super(instance);
		}

		@Override
		public ApformInstance getComponent() {
			return (ApformInstance) super.getComponent();
		}

		@Override
		protected List<ComponentReference<?>> getRequirements() {
			return Collections.<ComponentReference<?>> singletonList(getComponent().getDeclaration().getImplementation());
		}

		@Override
		public Component reify() {
			return CST.componentBroker.addInst(null, getComponent());
		}

	}


	/**
	 * Task to handle specification deployment
	 * 
	 * @author vega
	 * 
	 */
	private static class SpecificationDeploymentProcessing extends ComponentAppearenceRequest {

		public SpecificationDeploymentProcessing(ApformSpecification specification) {
			super(specification);
		}

		@Override
		public ApformSpecification getComponent() {
			return (ApformSpecification) super.getComponent();
		}

		@Override
		protected List<ComponentReference<?>> getRequirements() {
			return Collections.emptyList();
		}

		@Override
		public Component reify() {
			return CST.componentBroker.addSpec(getComponent());

		}
	}

	private static Logger logger = LoggerFactory.getLogger(Apform2Apam.class);

}
