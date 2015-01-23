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
package fr.imag.adele.apam.test.testcases;

//import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.pax.test.grant.impl.DayState;
import fr.imag.adele.apam.pax.test.grant.impl.ToolManager;
import fr.imag.adele.apam.pax.test.grant.impl.Worker;
import fr.imag.adele.apam.pax.test.iface.device.Eletronic;
import fr.imag.adele.apam.pax.test.implS1.ServiceDependencySource_tct018;
import fr.imag.adele.apam.pax.test.implS3.FailException;
import fr.imag.adele.apam.pax.test.implS3.S3GroupAImpl;
import fr.imag.adele.apam.tests.helpers.Constants;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ConflictManTest extends ExtensionAbstract {

	class ThreadWrapper_grant extends Thread {

		final Worker worker;

		public ThreadWrapper_grant(Worker worker) {
			this.worker = worker;
		}

		@Override
		public void run() {
			try {
				worker.breakRock();
				System.out.println("test 0K");
			} catch (Exception exc) {
				System.out.println("resolve exception thrown ? "
						+ exc.getMessage());
			}

		}

	}

	/**
	 * This test should be changed accordingly to a new feature allowing to
	 * force breaking a grant the link (some kind of \<deny\> markup )
	 * 
	 * TODO currently when an instance is not granted to some client, the
	 * last client keeps the reference. This may lead to starvation of other
	 * clients and some unfair scheduling. Right now the scheduling of 
	 * non shared components doesn't deal with this issues, specially when
	 * there is no waiting, as we do not have any form of round-robin or
	 * preemption on request 
	 * 
	 *  We need to review this test depending on the handling of starvation
	 */
	@Ignore
	@Test
	public void CompositeContentForcedReleaseGrantTest_tct015() {
		CompositeType ct = (CompositeType) waitForImplByName(null,
				"Yard_tct015");

		Implementation impl_daystate = waitForImplByName(null, "DayState_15");
		Implementation impl_jackhammer = waitForImplByName(null,
				"JackHammer_singleton");
		Implementation impl_worker = waitForImplByName(null, "Worker_waiting");
		Implementation impl_toolmanager = waitForImplByName(null, "ToolManager");

		Composite yard = (Composite) ct.createInstance(null, null);
		impl_jackhammer.createInstance(null, null);

		Worker worker1 = (Worker) impl_worker.createInstance(yard, null)
				.getServiceObject();

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Instance dayinst = null;
		Instance managerinst = null;
		for (Instance inst : yard.getContainInsts()) {
			if (inst.getImpl().equals(impl_daystate)) {
				dayinst = inst;
			} else if (inst.getImpl().equals(impl_toolmanager)) {
				managerinst = inst;
			}
			System.out.println("Contains : " + inst.getName());
		}
		System.out.println();

		Assert.assertNotNull("DayState is not found in the composite", dayinst);
		ToolManager manager = (ToolManager) managerinst.getServiceObject();
		DayState state = (DayState) dayinst.getServiceObject();

		ThreadWrapper_grant thread = new ThreadWrapper_grant(worker1);

		System.out.println(">19h : afternoon !");

		state.setHour(19);
		thread = new ThreadWrapper_grant(worker1);
		thread.setDaemon(true);
		thread.start();

		apam.waitForIt(1000);
		Assert.assertFalse(
				"As the JackHammer is granted (afternoon), the worker resolution should be ok -> thread should be ended",
				thread.isAlive());

		System.out.println(">23h : night !");
		state.setHour(23);
		thread = new ThreadWrapper_grant(worker1);
		thread.setDaemon(true);
		thread.start();

		apam.waitForIt(1000);
		;
		manager.printTools();
		Assert.assertTrue(
				"As the JackHammer is not granted (night again), the worker resolution should fail -> thread should be waiting",
				thread.isAlive());
	}

	@Test
	public void CompositeContentGrantTest_tct013() {
		CompositeType ct = (CompositeType) waitForImplByName(null,
				"Yard_tct013");

		Implementation impl_daystate = waitForImplByName(null, "DayState");
		Implementation impl_jackhammer = waitForImplByName(null,
				"JackHammer_singleton");
		Implementation impl_worker = waitForImplByName(null, "Worker_waiting");
		Implementation impl_toolmanager = waitForImplByName(null, "ToolManager");

		Composite yard = (Composite) ct.createInstance(null, null);
		impl_jackhammer.createInstance(null, null);

		Worker worker1 = (Worker) impl_worker.createInstance(yard, null)
				.getServiceObject();

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Instance dayinst = null;
		Instance managerinst = null;
		for (Instance inst : yard.getContainInsts()) {
			if (inst.getImpl().equals(impl_daystate)) {
				dayinst = inst;
			} else if (inst.getImpl().equals(impl_toolmanager)) {
				managerinst = inst;
			}
			System.out.println("Contains : " + inst.getName());
		}
		System.out.println();

		Assert.assertNotNull("DayState is not found in the composite", dayinst);
		ToolManager manager = (ToolManager) managerinst.getServiceObject();
		DayState state = (DayState) dayinst.getServiceObject();

		ThreadWrapper_grant thread = new ThreadWrapper_grant(worker1);

		System.out.println(">Init : night !");
		thread.setDaemon(true);
		thread.start();

		apam.waitForIt(1000);
		manager.printTools();
		Assert.assertTrue(
				"As the JackHammer is not granted(night), the worker resolution should fails -> thread should be waiting",
				thread.isAlive());

		System.out.println(">9h : morning !");
		state.setHour(9);

		apam.waitForIt(1000);
		manager.printTools();
		Assert.assertFalse(
				"As the JackHammer is granted (morning), the worker resolution should be ok -> thread should be ended",
				thread.isAlive());
	}

	@Test
	public void CompositeContentGrantToExternalTest_tct016() {
		CompositeType ct = (CompositeType) waitForImplByName(null,
				"Yard_tct013");

		Implementation impl_daystate = waitForImplByName(null, "DayState");
		Implementation impl_jackhammer = waitForImplByName(null,
				"JackHammer_singleton");
		Implementation impl_worker = waitForImplByName(null, "Worker_waiting");
		Implementation impl_toolmanager = waitForImplByName(null, "ToolManager");

		Composite yard = (Composite) ct.createInstance(null, null);
		impl_jackhammer.createInstance(null, null);

		Worker worker1 = (Worker) impl_worker.createInstance(null, null)
				.getServiceObject();

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Instance dayinst = null;
		Instance managerinst = null;
		for (Instance inst : yard.getContainInsts()) {
			if (inst.getImpl().equals(impl_daystate)) {
				dayinst = inst;
			} else if (inst.getImpl().equals(impl_toolmanager)) {
				managerinst = inst;
			}
			System.out.println("Contains : " + inst.getName());
		}
		System.out.println();

		Assert.assertNotNull("DayState is not found in the composite", dayinst);
		ToolManager manager = (ToolManager) managerinst.getServiceObject();
		DayState state = (DayState) dayinst.getServiceObject();

		ThreadWrapper_grant thread = new ThreadWrapper_grant(worker1);

		System.out.println(">Init : night !");
		thread.setDaemon(true);
		thread.start();

		apam.waitForIt(1000);
		manager.printTools();
		Assert.assertTrue(
				"As the JackHammer is not granted(night), the worker resolution should fails -> thread should be waiting",
				thread.isAlive());

		System.out.println(">9h : morning !");
		state.setHour(9);

		apam.waitForIt(1000);
		manager.printTools();
		Assert.assertFalse(
				"As the JackHammer is granted (morning), the worker resolution should be ok, EVEN if instance is external -> thread should be ended",
				thread.isAlive());
	}

	@Test
	public void CompositeContentGrantToExternalTest_tct017() {
		CompositeType ct = (CompositeType) waitForImplByName(null,
				"Yard_tct017");

		Implementation impl_daystate = waitForImplByName(null, "DayState_17");
		Implementation impl_jackhammer = waitForImplByName(null,
				"JackHammer_multiple");
		Implementation impl_worker = waitForImplByName(null,
				"Worker_waiting_exists");
		Implementation impl_toolmanager = waitForImplByName(null,
				"ToolManager_17");

		Composite yard = (Composite) ct.createInstance(null, null);
		impl_jackhammer.createInstance(null, null);

		Worker worker1 = (Worker) impl_worker.createInstance(yard, null)
				.getServiceObject();

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Instance dayinst = null;
		Instance managerinst = null;
		for (Instance inst : yard.getContainInsts()) {
			if (inst.getImpl().equals(impl_daystate)) {
				dayinst = inst;
			} else if (inst.getImpl().equals(impl_toolmanager)) {
				managerinst = inst;
			}
			System.out.println("Contains : " + inst.getName());
		}
		System.out.println();

		Assert.assertNotNull("DayState is not found in the composite", dayinst);
		ToolManager manager = (ToolManager) managerinst.getServiceObject();
		DayState state = (DayState) dayinst.getServiceObject();

		ThreadWrapper_grant thread = new ThreadWrapper_grant(worker1);

		manager.printTools();
		
		System.out.println(">Init : night !");
		thread.setDaemon(true);
		thread.start();

		Assert.assertTrue(
				"As the JackHammer is not granted(night), the worker resolution should fails -> thread should be waiting",
				thread.isAlive());

		System.out.println(">9h : morning !");
		state.setHour(9);

		apam.waitForIt(1000);
		manager.printTools();
		Assert.assertFalse(
				"As the JackHammer is granted (morning), the worker resolution should be ok -> thread should be ended",
				thread.isAlive());
	}

	@Test
	public void CompositeContentGrantWrongDependencyTest_tct020() {
		CompositeType ct = (CompositeType) waitForImplByName(null,
				"Yard_tct013");

		Implementation impl_daystate = waitForImplByName(null, "DayState");
		Implementation impl_jackhammer = waitForImplByName(null,
				"JackHammer_singleton");
		Implementation impl_worker = waitForImplByName(null, "Night_worker");
		Implementation impl_toolmanager = waitForImplByName(null, "ToolManager");

		Composite yard = (Composite) ct.createInstance(null, null);
		impl_jackhammer.createInstance(null, null);

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Worker worker1 = (Worker) impl_worker.createInstance(yard, null)
				.getServiceObject();

		Instance dayinst = null;
		Instance managerinst = null;
		for (Instance inst : yard.getContainInsts()) {
			if (inst.getImpl().equals(impl_daystate)) {
				dayinst = inst;
			} else if (inst.getImpl().equals(impl_toolmanager)) {
				managerinst = inst;
			}
			System.out.println("Contains : " + inst.getName());
		}
		System.out.println();

		Assert.assertNotNull("DayState is not found in the composite", dayinst);
		ToolManager manager = (ToolManager) managerinst.getServiceObject();
		DayState state = (DayState) dayinst.getServiceObject();
		// manager.printTools();
		state.setHour(4);

		ThreadWrapper_grant thread = new ThreadWrapper_grant(worker1);

		System.out.println(">Init : night !");
		thread.setDaemon(true);
		thread.start();

		apam.waitForIt(1000);
		manager.printTools();
		Assert.assertTrue(
				"As the JackHammer is not granted(night), the worker resolution should fails -> thread should be waiting",
				thread.isAlive());

	}

	@Test
	public void CompositeContentGrantWrongImplementationTest_tct019() {
		CompositeType ct = (CompositeType) waitForImplByName(null,
				"Yard_tct013");

		Implementation impl_daystate = waitForImplByName(null, "DayState");
		Implementation impl_jackhammer = waitForImplByName(null,
				"JackHammer_singleton");
		Implementation impl_worker = waitForImplByName(null,
				"Worker_waiting_bis");
		Implementation impl_toolmanager = waitForImplByName(null, "ToolManager");

		Composite yard = (Composite) ct.createInstance(null, null);
		impl_jackhammer.createInstance(null, null);

		Worker worker1 = (Worker) impl_worker.createInstance(yard, null)
				.getServiceObject();

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Instance dayinst = null;
		Instance managerinst = null;
		for (Instance inst : yard.getContainInsts()) {
			if (inst.getImpl().equals(impl_daystate)) {
				dayinst = inst;
			} else if (inst.getImpl().equals(impl_toolmanager)) {
				managerinst = inst;
			}
			System.out.println("Contains : " + inst.getName());
		}
		System.out.println();

		Assert.assertNotNull("DayState is not found in the composite", dayinst);
		ToolManager manager = (ToolManager) managerinst.getServiceObject();
		DayState state = (DayState) dayinst.getServiceObject();

		ThreadWrapper_grant thread = new ThreadWrapper_grant(worker1);

		System.out.println(">Init : night !");
		thread.setDaemon(true);
		thread.start();

		apam.waitForIt(1000);
		manager.printTools();
		Assert.assertTrue(
				"As the JackHammer is not granted(night), the worker resolution should fails -> thread should be waiting",
				thread.isAlive());

		System.out.println(">9h : morning !");
		state.setHour(9);

		apam.waitForIt(1000);
		manager.printTools();
		Assert.assertTrue(
				"The JackHammer is granted (morning) but the worker resolution should fail too (because wrong implementation working)  -> thread should be waiting",
				thread.isAlive());
	}

	@Test
	public void CompositeContentMngtDisputeAmongInjectionAndOwn_tc047() {

		Implementation sharedDependencyImpl = waitForImplByName(null,
				"BoschSwitch");

		Instance sharedDependency = sharedDependencyImpl.createInstance(null,
				null);

		CompositeType compositeAImpl = (CompositeType) waitForImplByName(null,
				"composite-a");

		Composite compositeA = (Composite) compositeAImpl.createInstance(null,
				null);

		S3GroupAImpl s3b = (S3GroupAImpl) compositeA.getMainInst()
				.getServiceObject();
		s3b.getElement();

		System.out.println("Original composite:"
				+ sharedDependency.getComposite());

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		CompositeType compositeBImpl = (CompositeType) waitForImplByName(null,
				"composite-a-dispute-inject-own");

		Composite compositeB = (Composite) compositeBImpl.createInstance(null,
				null);

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		System.out.println("Composite after the own composite instantiation:"
				+ sharedDependency.getComposite());

		String message = "Class A needs the instance IC, when B composite (declaring that owns IC) is instantiated, the IC should receive as parent composite the composite B. This did not happened";

		Assert.assertTrue(message,
				sharedDependency.getComposite() == compositeB);

	}

	@Test
	public void CompositeContentMngtDisputeAmongInjectionAndOwnInstanceIntoComposite_tc048() {

		Implementation sharedDependencyImpl = waitForImplByName(null,
				"BoschSwitch");

		CompositeType compositeAImpl = (CompositeType) waitForImplByName(null,
				"composite-a");

		Composite compositeA = (Composite) compositeAImpl.createInstance(null,
				null);

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		// Instance sharedDependency=sharedDependencyImpl.createInstance(null,
		// null); //works
		Instance sharedDependency = sharedDependencyImpl.createInstance(
				compositeA, null); // do not works

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		S3GroupAImpl s3b = (S3GroupAImpl) compositeA.getMainInst()
				.getServiceObject();
		s3b.getElement();

		System.out.println("Original composite:"
				+ sharedDependency.getComposite());

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		CompositeType compositeBImpl = (CompositeType) waitForImplByName(null,
				"composite-a-dispute-inject-own");

		Composite compositeB = (Composite) compositeBImpl.createInstance(null,
				null);

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		System.out.println("Composite after the own composite instantiation:"
				+ sharedDependency.getComposite());

		String message = "Class A needs the instance (that is already located inside another composite) IC, when B composite (declaring that owns IC) is instantiated, the IC should receive as parent composite the composite B. This did not happened";

		Assert.assertTrue(message,
				sharedDependency.getComposite() == compositeB);

	}

	@Test
	public void CompositeContentMngtOwnSpecification_tc046() {

		CompositeType cta = (CompositeType) waitForImplByName(null,
				"composite-a-own-specification");

		Composite composite_a = (Composite) cta.createInstance(null, null);

		Implementation device = waitForImplByName(null, "BoschSwitch");
		Instance deviceinst = device.createInstance(null, null);

		String message = "When a composite declares to own a specification, that means every instance of that specification should be owned by that composite. This test failed, the actual owner composite of that component and the one that declares to be the owner are different";

		Assert.assertTrue(message, deviceinst.getComposite() == composite_a);

	}

	@Test
	public void CompositeContentSimpleReleaseGrantTest_tct014() {
		CompositeType ct = (CompositeType) waitForImplByName(null,
				"Yard_tct013");

		Implementation impl_daystate = waitForImplByName(null, "DayState");
		Implementation impl_jackhammer = waitForImplByName(null,
				"JackHammer_singleton");
		Implementation impl_worker = waitForImplByName(null, "Worker_waiting");
		Implementation impl_toolmanager = waitForImplByName(null, "ToolManager");

		Composite yard = (Composite) ct.createInstance(null, null);
		impl_jackhammer.createInstance(null, null);

		Worker worker1 = (Worker) impl_worker.createInstance(yard, null)
				.getServiceObject();

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Instance dayinst = null;
		Instance managerinst = null;
		for (Instance inst : yard.getContainInsts()) {
			if (inst.getImpl().equals(impl_daystate)) {
				dayinst = inst;
			} else if (inst.getImpl().equals(impl_toolmanager)) {
				managerinst = inst;
			}
			System.out.println("Contains : " + inst.getName());
		}
		System.out.println();

		Assert.assertNotNull("DayState is not found in the composite", dayinst);
		ToolManager manager = (ToolManager) managerinst.getServiceObject();
		DayState state = (DayState) dayinst.getServiceObject();
		manager.printTools();
		apam.waitForIt(500);
		ThreadWrapper_grant thread = new ThreadWrapper_grant(worker1);

		System.out.println(">19h : afternoon !");

		state.setHour(19);
		thread = new ThreadWrapper_grant(worker1);
		thread.setDaemon(true);
		thread.start();

		apam.waitForIt(500);
		Assert.assertFalse(
				"As the JackHammer is granted (afternoon), the worker resolution should be ok -> thread should be ended",
				thread.isAlive());

		System.out.println(">23h : night !");
		state.setHour(23);
		manager.printTools();
		apam.waitForIt(500);
		thread = new ThreadWrapper_grant(worker1);
		thread.setDaemon(true);
		thread.start();

		apam.waitForIt(500);
		Assert.assertTrue(
				"As the JackHammer is not granted (night again), the worker resolution should fail -> thread should be waiting",
				thread.isAlive());
	}

	@Override
	public List<Option> config() {
		Map<String, String> mapOfRequiredArtifacts = new HashMap<String, String>();
		mapOfRequiredArtifacts.put("apam-pax-samples-impl-s3",
				"fr.imag.adele.apam.tests.services");
		mapOfRequiredArtifacts.put("apam-pax-samples-impl-s2",
				"fr.imag.adele.apam.tests.services");
		mapOfRequiredArtifacts.put("apam-pax-samples-impl-s1",
				"fr.imag.adele.apam.tests.services");
		mapOfRequiredArtifacts.put("apam-pax-samples-iface",
				"fr.imag.adele.apam.tests.services");
		mapOfRequiredArtifacts.put("apam-pax-dynaman-grant",
				"fr.imag.adele.apam.tests.app");
		
		List<Option> addon = super.config(mapOfRequiredArtifacts, false);

		addon.add(systemPackage("javax.xml.parsers"));
		addon.add(0, packApamConflictManager());
		return addon;
	}

	@Test
	public void DependencyRelease_tct018() {
		Implementation implSource = waitForImplByName(null,
				"ServiceDependencySource_tct018");
		Implementation implTarget = waitForImplByName(null,
				"ServiceDependencyTarget_tct018");
		apam.waitForIt(Constants.CONST_WAIT_TIME);

		Instance instSourceA = implSource.createInstance(null, null);
		Instance instSourceB = implSource.createInstance(null, null);
		Instance instTarget = implTarget.createInstance(null, null);

		ServiceDependencySource_tct018 sourceA = (ServiceDependencySource_tct018) instSourceA
				.getServiceObject();
		ServiceDependencySource_tct018 sourceB = (ServiceDependencySource_tct018) instSourceB
				.getServiceObject();

		boolean exceptionThrown = false;

		try {
			sourceA.getAndKeepTarget();
			System.out.println("Source A resolved target, but keeping");
			sourceB.getAndReleaseTarget();
			System.out.println("Source B resolved target");
		} catch (Throwable ex) {
			System.out.println("Exception thrown : " + ex.getMessage());
			exceptionThrown = true;
		}
		Assert.assertTrue(
				"Usual Case if A resolve and use a target (not shared), B cannot use it",
				exceptionThrown);

		exceptionThrown = false;
		try {
			sourceA.getAndReleaseTarget();
			System.out.println("Source A resolved target, and released");
			sourceB.getAndReleaseTarget();
			System.out.println("Source B resolved target, and released");
		} catch (Throwable ex) {
			System.out.println("Exception thrown : " + ex.getMessage());
			exceptionThrown = true;
		}
		Assert.assertFalse(
				"If A  set its field to null, dependency should be available for B",
				exceptionThrown);

	}

}
