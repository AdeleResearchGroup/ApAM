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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
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

	protected Instance auxListInstanceReferencedBy(Object instance){

		return CST.componentBroker.getInstService(instance);

	}

	protected List<Instance> auxListInstanceReferencedBy(String prefix,Collection instances){

		List<Instance> res=new ArrayList<Instance>();

		for(Object instance:instances){

			Instance i=CST.componentBroker.getInstService(instance);

			res.add(i);

			System.out.println(String.format("%sInstance name %s ( oid: %s ) ",
					prefix, i.getName(), i.getServiceObject()));

		}

		return res;

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
		for (Map.Entry<String, Object> entry: component.getAllProperties().entrySet()) {
			System.out.print(entry.getKey() + "="
					+ entry.getValue());
			if(!entry.getValue().equals(component.getProperty(entry.getKey()))){
				System.out.print("("+component.getProperty(entry.getKey())+")");
			}
			System.out.println("");
		}
		System.out.println(String.format(
				"%s------------ /Properties -------------", prefix));
	}

	protected void auxDisconectWires(Instance instance) {

		for (Link wire : instance.getLinks()) {

			instance.removeLink(wire);

		}

	}

}
