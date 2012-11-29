package fr.imag.adele.apam.tests.helpers;

import java.util.ArrayList;
import java.util.List;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;

/**
 * Constains utility functions that are used by the test 
 * @author jander
 */
public abstract class TestUtils {

	protected List<Instance> auxLookForInstanceOf(String ... clazz) {

		List<Instance> pool = new ArrayList<Instance>();

		for (Instance i : CST.componentBroker.getInsts()) {

			ImplementationDeclaration apamImplDecl = i.getImpl()
					.getImplDeclaration();

			if (apamImplDecl instanceof AtomicImplementationDeclaration) {

				AtomicImplementationDeclaration atomicInitialInstance = (AtomicImplementationDeclaration) apamImplDecl;

                for(String classname:clazz){

                    if (atomicInitialInstance.getClassName().equals(classname)) {
                        pool.add(i);
                    }

                }
			}
		}

		return pool;
	}
	
	protected void auxListInstances(String prefix) {
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

	protected void auxListProperties(String prefix, Component component) {
		System.out.println(String.format(
				"%s------------ Properties -------------", prefix));
		for (String key : component.getAllProperties().keySet()) {
			System.out.println(key + "="
					+ component.getAllProperties().get(key.toString()));
		}
		System.out.println(String.format(
				"%s------------ /Properties -------------", prefix));
	}

	protected void auxDisconectWires(Instance instance) {

		for (Wire wire : instance.getWires()) {

			instance.removeWire(wire);

		}

	}
	
}
