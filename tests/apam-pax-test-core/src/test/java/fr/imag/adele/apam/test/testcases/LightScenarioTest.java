/**
 * Copyright 2011-2013 Universite Joseph Fourier, LIG, ADELE team
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
 *
 * LightScenarioTest.java - 17 juil. 2013
 */
package fr.imag.adele.apam.test.testcases;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Ignore;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.util.PathUtils;

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.test.lights.button.ButtonImpl;
import fr.imag.adele.apam.test.lights.button.SwingButtonImpl;
import fr.imag.adele.apam.test.lights.devices.BinaryLight;
import fr.imag.adele.apam.test.lights.panel.LightManagerTester;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

/**
 * @author thibaud
 * 
 */
//@RunWith(PaxExam.class)
//@ExamReactorStrategy(PerMethod.class)
public class LightScenarioTest extends ExtensionAbstract {

	Instance myKitchen;
	Set<String> linksKitchen;

	@Override
	public List<Option> config() {

		Map<String, String> mapOfRequiredArtifacts = new HashMap<String, String>();
		mapOfRequiredArtifacts.put("LightingScenarioTest",
				"fr.imag.adele.apam.test.lights");

		List<Option> defaults = super.config(mapOfRequiredArtifacts, true);
		try {
			defaults.add(CoreOptions.bundle(
					(new File(PathUtils.getBaseDir(), "bundle/wireadmin.jar"))
							.toURI().toURL().toExternalForm()).start());

		} catch (Exception error) {
			Assert.assertTrue("Error deploying WireAdmin", false);
		}

		return defaults;
	}

	@Override
	public void setUp() {
		super.setUp();
		waitForApam();
		linksKitchen = new HashSet<String>();

		// First launch the "devices"
		Implementation implemButtonGUI = waitForImplByName(null, "ButtonGUI");
		Implementation implemButtonNotGUI = waitForImplByName(null,
				"ButtonNotGUI");
		Implementation implemBinaryLightImpl = waitForImplByName(null,
				"BinaryLightImpl");

		// set the location properties of the devices
		Hashtable<String, String> propKitchen = new Hashtable<String, String>();
		propKitchen.put("location", "kitchen");

		Hashtable<String, String> propLiving = new Hashtable<String, String>();
		propLiving.put("location", "living");

		Instance buttonKitchen = implemButtonGUI
				.createInstance(null, propKitchen);
		linksKitchen.add(buttonKitchen.getName());
		Instance lightKitchen = implemBinaryLightImpl
				.createInstance(null, propKitchen);
		linksKitchen.add(lightKitchen.getName());

		
		Instance buttonLiving = implemButtonNotGUI
				.createInstance(null, propLiving);		
		Instance lightLiving = implemBinaryLightImpl
				.createInstance(null, propLiving);

		// Wait for the devices to be instantiated
		apam.waitForIt(1000);

		// Then launch the Application
		Implementation implemApplication = waitForImplByName(null,
				"LightApplicationKitchen");

		myKitchen = implemApplication.createInstance(null,
				Collections.<String, String> emptyMap());
	}

    //@Test not working anymore on jenkins because of AWT
	public void testButtonKitchen() {
		Implementation implementation = waitForImplByName(null,
				"LightManagerPanel");

		Instance inst = implementation.createInstance(null,
				Collections.<String, String> emptyMap());

		LightManagerTester tester = (LightManagerTester) inst
				.getServiceObject();

		// Wait for the binding between components
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		try {
			tester.testButtonKitchen();
		} catch (Exception exc) {
			Assert.fail("testButtonKitchen() -> " + exc.getMessage());
		}
	}

    //@Test not working anymore on jenkins because of AWT
	public void testButtonLiving() {
		Implementation implementation = waitForImplByName(null,
				"LightManagerPanel");

		Instance inst = implementation.createInstance(null, null);

		LightManagerTester tester = (LightManagerTester) inst
				.getServiceObject();

		// Wait for the binding between components
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		try {
			tester.testButtonLiving();
		} catch (Exception exc) {
			Assert.fail("testButtonLiving() -> " + exc.getMessage());
		}
	}

	//@Test not working anymore on jenkins because of AWT
	public void testMyKitchenBinding() {

		waitForInstByName(null, "LightApplicationKitchen-0");

		// Wait for the binding between lightApplication myKitchen and devices
		// upon filter location
		auxListInstances();

		Set<Link> listRelations = myKitchen.getRawLinks();
		Iterator<Link> it = listRelations.iterator();
		while (it.hasNext()) {
			Link rel = it.next();
			// Should find one binding with buttonKitchen and one binding with a
			// lightKitchen
			System.out.println("Link : " + rel);
			System.err.println("-> " + rel.getDestination().getName());
			if (linksKitchen.contains(rel.getDestination().getName())) {
				linksKitchen.remove(rel.getDestination().getName());
			} else {
				Assert.fail("testMyKitchenBinding() -> this link should not exists : "
						+ rel.getDestination().getName());
			}
		}
		if (linksKitchen.size() > 0) {
			Assert.fail("testMyKitchenBinding() -> not all links completed, "
					+ linksKitchen.size() + " links remaining");
		}
	}
}
