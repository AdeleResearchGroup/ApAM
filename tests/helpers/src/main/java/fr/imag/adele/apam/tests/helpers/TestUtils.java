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
package fr.imag.adele.apam.tests.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;

/**
 * Constains utility functions that are used by the test
 * 
 * @author jander
 */
public abstract class TestUtils {

	private boolean apamReady = false;

	public static long waitPeriod = 200;
	public static long RESOLVE_TIMEOUT = 3000;

	protected void auxDisconectWires(Instance instance) {
		for (Link wire : instance.getRawLinks()) {
			wire.remove();
		}
	}

	protected Instance auxListInstanceReferencedBy(Object instance) {
		waitForApam();

		return CST.componentBroker.getInstService(instance);

	}

	protected List<Instance> auxListInstanceReferencedBy(String prefix, Collection instances) {
		waitForApam();

		List<Instance> res = new ArrayList<Instance>();

		for (Object instance : instances) {

			Instance i = CST.componentBroker.getInstService(instance);

			res.add(i);

			System.out.println(String.format("%sInstance name %s ( oid: %s ) ",
					prefix, i.getName(), i.getServiceObject()));

		}

		return res;

	}

	protected void auxListInstances() {
		auxListInstances("\t");
	}

	protected void auxListInstances(String prefix) {
		waitForApam();

		System.out.println(String.format(
				"%s------------ Instances (Total:%d) -------------", prefix,
				CST.componentBroker.getInsts().size()));
		for (Instance i : CST.componentBroker.getInsts()) {

			System.out.println(String.format("%sInstance name %s ( oid: %s ) ",
					prefix, i.getName(), i.getServiceObject()));

		}
		System.out.println(String.format(
				"%s------------ /Instances -------------", prefix));
	}

	protected void auxListProperties(Component component) {
		this.auxListProperties("\t\t", component);
	}

	protected void auxListProperties(String prefix, Component component) {
		System.out.println(String.format(
				"%s------------ Properties -------------", prefix));
		for (Map.Entry<String, Object> entry : component.getAllProperties()
				.entrySet()) {
			System.out.print(entry.getKey() + "=" + entry.getValue());
			if (!entry.getValue().equals(component.getProperty(entry.getKey()))) {
				System.out.print("(" + component.getProperty(entry.getKey())
						+ ")");
			}
			System.out.println("");
		}
		System.out.println(String.format(
				"%s------------ /Properties -------------", prefix));
	}

	protected List<Instance> auxLookForInstanceOf(String... clazz) {
		waitForApam();

		List<Instance> pool = new ArrayList<Instance>();
		for (Instance i : CST.componentBroker.getInsts()) {

			ImplementationDeclaration apamImplDecl = i.getImpl()
					.getImplDeclaration();

			if (apamImplDecl instanceof AtomicImplementationDeclaration) {

				AtomicImplementationDeclaration atomicInitialInstance = (AtomicImplementationDeclaration) apamImplDecl;

				for (String classname : clazz) {

					if (atomicInitialInstance.getClassName().equals(classname)) {
						pool.add(i);
					}
				}
			}
		}
		return pool;
	}

	public void waitForApam() {
		waitForApam(RESOLVE_TIMEOUT);
	}

	public void waitForApam(long timeout) {
		long sleep = 0;
		while (!apamReady && sleep < timeout) {
			if (CST.componentBroker != null && CST.apamResolver != null
					&& CST.apam != null) {

				apamReady = true;
			} else {
				try {
					Thread.sleep(waitPeriod);
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
			sleep += waitPeriod;
		}
		boolean foundAPAM = false;
		while (sleep < timeout && !foundAPAM) {
			try {
				Thread.sleep(waitPeriod);
			} catch (Exception exc) {
				exc.printStackTrace();
			}
			sleep += waitPeriod;
			if (CST.apamResolver != null) {
				if (CST.apamResolver.findInstByName(null, "APAM-Instance") != null) {
					// && CST.apamResolver.findInstByName(null,
					// "OSGiMan-Instance") != null
					// && CST.apamResolver.findInstByName(null,
					// "ConflictManager-Instance") != null)
					foundAPAM = true;
				}
			}
		}
	}

	protected Component waitForComponentByName(Component client,
			String componentName) {
		return waitForComponentByName(client, componentName, RESOLVE_TIMEOUT);
	}

	protected Component waitForComponentByName(Component client,
			String componentName, long timeout) {
		waitForApam();

		Component comp = CST.apamResolver.findComponentByName(client,
				componentName);
		long sleep = 0;

		while (sleep < timeout && comp == null) {
			try {
				Thread.sleep(waitPeriod);
			} catch (Exception exc) {
				exc.printStackTrace();
			}
			sleep += waitPeriod;
			if (CST.apamResolver != null) {
				comp = CST.apamResolver.findComponentByName(client,
						componentName);
			}
		}

		return comp;
	}

	protected Implementation waitForImplByName(Component client,
			String componentName) {
		return waitForImplByName(client, componentName, RESOLVE_TIMEOUT);
	}

	protected Implementation waitForImplByName(Component client, String componentName, long timeout) {
		waitForApam();
		Implementation impl = CST.apamResolver.findImplByName(client, componentName);
		long sleep = 0;

		while (sleep < timeout && impl == null) {
			try {
				Thread.sleep(waitPeriod);
			} catch (Exception exc) {
				exc.printStackTrace();
			}
			sleep += waitPeriod;
			if (CST.apamResolver != null) {
				impl = CST.apamResolver.findImplByName(client, componentName);
			}
		}

		return impl;
	}

	protected Instance waitForInstByName(Component client, String componentName) {
		return waitForInstByName(client, componentName, RESOLVE_TIMEOUT);
	}

	protected Instance waitForInstByName(Component client, String componentName, long timeout) {
		waitForApam();

		Instance inst = CST.apamResolver.findInstByName(client, componentName);
		long sleep = 0;

		while (sleep < timeout && inst == null) {
			try {
				Thread.sleep(waitPeriod);
			} catch (Exception exc) {
				exc.printStackTrace();
			}
			sleep += waitPeriod;
			if (CST.apamResolver != null) {
				inst = CST.apamResolver.findInstByName(client, componentName);
			}
		}

		return inst;
	}

	protected Specification waitForSpecByName(Component client,
			String componentName) {
		return waitForSpecByName(client, componentName, RESOLVE_TIMEOUT);
	}

	protected Specification waitForSpecByName(Component client,
			String componentName, long timeout) {
		waitForApam();
		Specification spec = CST.apamResolver.findSpecByName(client,
				componentName);
		long sleep = 0;

		while (sleep < timeout && spec == null) {
			try {
				Thread.sleep(waitPeriod);
			} catch (Exception exc) {
				exc.printStackTrace();
			}
			sleep += waitPeriod;
			if (CST.apamResolver != null) {
				spec = CST.apamResolver.findSpecByName(client, componentName);
			}
		}

		return spec;
	}

}
