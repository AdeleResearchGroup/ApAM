package fr.imag.adele.apam.test.testcases;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.pax.test.iface.device.Eletronic;
import fr.imag.adele.apam.pax.test.impl.S1Impl;
import fr.imag.adele.apam.test.support.Constants;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(JUnit4TestRunner.class)
public class ConstraintTest extends ExtensionAbstract{
	
	/**
	 * Verify if the constraints were used to inject the dependencies in the
	 * component
	 */
	@Test
	public void ConstraintsCheckingImplementation() {

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.pax.test.impl.S1Impl");

		Instance s1Inst = s1Impl.createInstance(null, null);

		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();

		Instance philipsSwitch = CST.componentBroker.getInstService(s1
				.getSimpleDevice110v());

		Assert.assertTrue(philipsSwitch.match("(manufacturer=philips)"));
		Assert.assertTrue(philipsSwitch.match("(voltage=110)"));
		Assert.assertTrue(philipsSwitch
				.match("(&amp;(voltage=110)(manufacturer=philips))"));

	}
	
	/**
	 * Verify if the constraints were used to inject the dependencies in the
	 * component by initial properties
	 * 
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void ConstraintsCheckingInstanceFilteringByInitialProperty()
			throws InvalidSyntaxException {

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation samsungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");
		final Instance samsungInst = samsungImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "95");
					}
				});

		Implementation lgImpl = CST.apamResolver.findImplByName(null,
				"LgSwitch");
		final Instance lgInst = lgImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "100");
					}
				});

		Implementation siemensImpl = CST.apamResolver.findImplByName(null,
				"SiemensSwitch");
		final Instance siemensInst = siemensImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "105");
					}
				});

		Implementation boschImpl = CST.apamResolver.findImplByName(null,
				"BoschSwitch");
		final Instance boschInst = boschImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "110");
					}
				});

		Implementation philipsImpl = CST.apamResolver.findImplByName(null,
				"philipsSwitch");
		final Instance philipsInst = philipsImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "117");
					}
				});

		Set<Instance> validInstances = new HashSet<Instance>() {
			{
				add(boschInst);
				add(siemensInst);
				add(lgInst);
				add(samsungInst);
			}
		};

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.pax.test.impl.S1Impl");

		Instance s1Inst = s1Impl.createInstance(null, null);
		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();

		for (Eletronic e : s1.getEletronicInstancesConstraintsInstance()) {
			Instance p = CST.componentBroker.getInstService(e);
			System.out.println("---- Voltage:"
					+ p.getProperty("currentVoltage") + " / Name:"
					+ p.getName());

			boolean found = false;

			for (Instance l : validInstances)
				if (l.getName().equals(p.getName())) {
					found = true;
					break;
				}

			// Check if all valid instances were injected
			Assert.assertTrue(found);

		}

		// check if there is no other instance injected
		Assert.assertTrue(s1.getEletronicInstancesConstraintsInstance().size() == validInstances
				.size());

	}

	/**
	 * Verify if the constraints were used to inject the dependencies in the
	 * component by set property
	 * 
	 * @throws InvalidSyntaxException
	 */
	@Test
	public void ConstraintsCheckingInstanceFilteringBySetProperty()
			throws InvalidSyntaxException {

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation samsungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");
		final Instance samsungInst = samsungImpl.createInstance(null, null);

		Implementation lgImpl = CST.apamResolver.findImplByName(null,
				"LgSwitch");
		final Instance lgInst = lgImpl.createInstance(null, null);

		Implementation siemensImpl = CST.apamResolver.findImplByName(null,
				"SiemensSwitch");
		final Instance siemensInst = siemensImpl.createInstance(null, null);

		Implementation boschImpl = CST.apamResolver.findImplByName(null,
				"BoschSwitch");
		final Instance boschInst = boschImpl.createInstance(null, null);

		Implementation philipsImpl = CST.apamResolver.findImplByName(null,
				"philipsSwitch");
		final Instance philipsInst = philipsImpl.createInstance(null, null);

		samsungInst.setProperty("currentVoltage", "95");
		lgInst.setProperty("currentVoltage", "100");
		siemensInst.setProperty("currentVoltage", "105");
		boschInst.setProperty("currentVoltage", "110");
		philipsInst.setProperty("currentVoltage", "117");

		Set<Instance> validInstances = new HashSet<Instance>() {
			{
				add(boschInst);
				add(siemensInst);
				add(lgInst);
				add(samsungInst);
			}
		};

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.pax.test.impl.S1Impl");

		Instance s1Inst = s1Impl.createInstance(null, null);
		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();

		for (Eletronic e : s1.getEletronicInstancesConstraintsInstance()) {
			Instance p = CST.componentBroker.getInstService(e);
			System.out.println("---- Voltage:"
					+ p.getProperty("currentVoltage") + " / Name:"
					+ p.getName());

			boolean found = false;

			for (Instance l : validInstances)
				if (l.getName().equals(p.getName())) {
					found = true;
					break;
				}

			// Check if all valid instances were injected
			Assert.assertTrue(found);

		}

		// check if there is no other instance injected
		Assert.assertTrue(s1.getEletronicInstancesConstraintsInstance().size() == validInstances
				.size());

	}
	
}
