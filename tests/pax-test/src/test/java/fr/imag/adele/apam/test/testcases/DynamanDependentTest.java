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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import fr.imag.adele.apam.CST;
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

@RunWith(JUnit4TestRunner.class)
public class DynamanDependentTest extends ExtensionAbstract {

    @Override
    public List<Option> config() {
	Map<String, String> mapOfRequiredArtifacts= new HashMap<String, String>();
	mapOfRequiredArtifacts.put("apam-pax-samples-impl-s3", "fr.imag.adele.apam.tests.services");
	mapOfRequiredArtifacts.put("apam-pax-samples-iface", "fr.imag.adele.apam.tests.services");
	
	List<Option> addon = super.config(mapOfRequiredArtifacts,false);

	addon.add(systemPackage("javax.xml.parsers"));
	addon.add(0, packApamConflictManager());
	return addon;
    }
   

    
    @Test
    public void CompositeContentMngtDependencyFailWait_tc039() {
	
	
	apam.waitForIt(10000);
	

	CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
		null, "composite-a-fail-wait");
	apam.waitForIt(10000);

	Composite composite_a = (Composite) cta.createInstance(null, null);

	Instance instanceApp1 = composite_a.getMainInst();

	S3GroupAImpl ga1 = (S3GroupAImpl) instanceApp1.getServiceObject();

	ThreadWrapper wrapper = new ThreadWrapper(ga1);
	wrapper.setDaemon(true);
	wrapper.start();

	apam.waitForIt(3000);

	String message = "In case of composite dependency been marked as fail='wait', the thread should be blocked until the dependency is satisfied. During this test the thread did not block.";

	Assert.assertTrue(message, wrapper.isAlive());
    }

    @Test
    public void CompositeContentMngtDependencyFailException_tc040() {

	CompositeType ctroot = (CompositeType) CST.apamResolver.findImplByName(
		null, "composite-a-fail-exception");

	CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
		null, "composite-a-fail-exception");

	Composite composite_root = (Composite) ctroot
		.createInstance(null, null);

	Composite composite_a = (Composite) cta.createInstance(composite_root,
		null);

	Instance instanceApp1 = composite_a.getMainInst();

	S3GroupAImpl ga1 = (S3GroupAImpl) instanceApp1.getServiceObject();

	String messageTemplate = "In contentMngt->dependency if we adopt fail='exception' exception='A', the exception A should be throw in case the dependency is not satifiable. %s";

	boolean exception = false;
	boolean exceptionType = false;

	try {

	    Eletronic injected = ga1.getElement();
	    System.out.println("Element:" + injected);

	} catch (Exception e) {
	    exception = true;

	    System.err
		    .println("-------------- Exception raised -----------------");

	    e.printStackTrace();

	    System.err
		    .println("-------------- /Exception raised -----------------");

	    if (e instanceof FailException) {
		exceptionType = true;
	    }

	}

	String messageException = String.format(messageTemplate,
		"But no exception was thrown");
	String messageExceptionType = String.format(messageTemplate,
		"But the exception thrown was not of the proper type (A)");

	Assert.assertTrue(messageException, exception);
	Assert.assertTrue(messageExceptionType, exceptionType);

    }

    // Require by the test CompositeContentMngtDependencyFailWait
    class ThreadWrapper extends Thread {

	final S3GroupAImpl group;

	public ThreadWrapper(S3GroupAImpl group) {
	    this.group = group;
	}

	@Override
	public void run() {
	    System.out.println("Element injected:" + group.getElement());
	}

    }

    @Test
    public void CompositeWithEagerDependency_tc041() {
	CompositeType ct1 = (CompositeType) CST.apamResolver.findImplByName(
		null, "S2Impl-composite-eager");

	String message = "During this test, we enforce the resolution of the dependency by signaling dependency as eager='true'. %s";

	Assert.assertTrue(String.format(message,
		"Although, the test failed to retrieve the composite"),
		ct1 != null);

	auxListInstances("instances existing before the test-");

	Composite instanceComposite = (Composite) ct1.createInstance(null,
		new HashMap<String, String>());

	Implementation implS2 = CST.apamResolver.findImplByName(null,
		"fr.imag.adele.apam.pax.test.implS2.S2Impl");

	Instance instance = implS2.createInstance(instanceComposite, null);

	Assert.assertTrue(String.format(message,
		"Although, the test failed to instantiate the composite"),
		instance != null);

	// Force injection (for debuggin purposes)
	// S2Impl im=(S2Impl)instance.getServiceObject();
	// im.getDeadMansSwitch();

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	List<Instance> pool = auxLookForInstanceOf(
		"fr.imag.adele.apam.pax.test.impl.deviceSwitch.PhilipsSwitch",
		"fr.imag.adele.apam.pax.test.impl.deviceSwitch.GenericSwitch",
		"fr.imag.adele.apam.pax.test.impl.deviceSwitch.HouseMeterSwitch",
		"fr.imag.adele.apam.pax.test.deviceDead.DeadsManSwitch",
		"fr.imag.adele.apam.pax.test.impl.deviceSwitch.PropertyChangeNotificationSwitch",
		"fr.imag.adele.apam.pax.test.impl.deviceSwitch.PropertyInjectionTypeSwitch");

	auxListInstances("instances existing after the test-");
	System.out.println("pool.size() : " + pool.size());

	Assert.assertTrue(
		String.format(
			message,
			"Although, there exist no instance of dependence required(DeadsManSwitch.class), which means that it was not injected."),
		pool.size() == 1);

    }

    @Test
    public void CompositeWithEagerDependencyExplicitySpecification_tc051() {
	CompositeType ct1 = (CompositeType) CST.apamResolver.findImplByName(
		null, "S2Impl-composite-eager-forceEager");

	String message = "During this test, we enforce the resolution of the dependency by signaling dependency as eager='true'. %s";

	Assert.assertTrue(String.format(message,
		"Although, the test failed to retrieve the composite"),
		ct1 != null);

	auxListInstances("instances existing before the test-");

	Composite instanceComposite = (Composite) ct1.createInstance(null,
		new HashMap<String, String>());

	Implementation implS2 = CST.apamResolver.findImplByName(null,
		"fr.imag.adele.apam.pax.test.implS2.S2Impl-forceEager");

	Instance instance = implS2.createInstance(instanceComposite, null);

	Assert.assertTrue(String.format(message,
		"Although, the test failed to instantiate the composite"),
		instance != null);

	// Force injection (for debuggin purposes)
	// S2Impl im=(S2Impl)instance.getServiceObject();
	// im.getDeadMansSwitch();

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	List<Instance> pool = auxLookForInstanceOf(
		"fr.imag.adele.apam.pax.test.impl.deviceSwitch.PhilipsSwitch",
		"fr.imag.adele.apam.pax.test.impl.deviceSwitch.GenericSwitch",
		"fr.imag.adele.apam.pax.test.impl.deviceSwitch.HouseMeterSwitch",
		"fr.imag.adele.apam.pax.test.deviceDead.DeadsManSwitch",
		"fr.imag.adele.apam.pax.test.impl.deviceSwitch.PropertyChangeNotificationSwitch",
		"fr.imag.adele.apam.pax.test.impl.deviceSwitch.PropertyInjectionTypeSwitch",
		"fr.imag.adele.apam.pax.test.impl.deviceSwitch.PropertyTypeIntChangeNotificationSwitch");

	auxListInstances("instances existing after the test-");

	Assert.assertTrue(
		String.format(
			message,
			"Although, there exist no instance of dependence required(specification 'electronic-device'), which means that it was not injected correctly."),
		pool.size() == 1);

    }

    @Test
    public void CompositeContentMngtStartTriggerBySpecification_tc042() {
	auxListInstances("INSTANCE-t1-");

	String checkingFor = "specification";

	CompositeType composite = (CompositeType) CST.apamResolver
		.findImplByName(null, "composite-a-start-by-" + checkingFor);
	Composite compositeInstance = (Composite) composite.createInstance(
		null, null);

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	Implementation trigger = CST.apamResolver.findImplByName(null,
		"group-a-start-trigger");

	Implementation triggered = CST.apamResolver.findImplByName(null,
		"group-b-started-by-trigger");

	Instance triggerInstance = trigger.createInstance(compositeInstance,
		null);

	Assert.assertTrue(triggerInstance != null);

	List<Instance> instancesOfB = auxLookForInstanceOf(((AtomicImplementationDeclaration) triggered
		.getImplDeclaration()).getClassName());

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	auxListInstances("INSTANCE-t2-");

	String messageTemplate = "Its possible to create an instance according to the appearance of a certain %s by using <start/> element with <trigger/>. The expected instance was not created when the trigger was launched.";
	String message = String.format(messageTemplate, checkingFor);
	Assert.assertTrue(message, instancesOfB.size() == 1);

    }

    @Test
    public void CompositeContentMngtStartTriggerByImplementation_tc043() {
	auxListInstances("INSTANCE-t1-");

	String checkingFor = "implementation";

	CompositeType composite = (CompositeType) CST.apamResolver
		.findImplByName(null, "composite-a-start-by-" + checkingFor);
	Composite compositeInstance = (Composite) composite.createInstance(
		null, null);

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	Implementation trigger = CST.apamResolver.findImplByName(null,
		"group-a-start-trigger");

	Instance triggerInstance = trigger.createInstance(compositeInstance,
		null);

	Implementation triggered = CST.apamResolver.findImplByName(null,
		"group-b-started-by-trigger");

	Assert.assertTrue(triggerInstance != null);

	List<Instance> instancesOfB = auxLookForInstanceOf(((AtomicImplementationDeclaration) triggered
		.getImplDeclaration()).getClassName());

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	auxListInstances("INSTANCE-t2-");

	String messageTemplate = "Its possible to create an instance according to the appearance of a certain %s by using <start/> element with <trigger/>. The expected instance was not created when the trigger was launched.";
	String message = String.format(messageTemplate, checkingFor);
	Assert.assertTrue(message, instancesOfB.size() == 1);

    }

    @Test
    public void CompositeDependencyFailWait_tc044() {

	Implementation cta = (Implementation) CST.apamResolver.findImplByName(
		null, "group-a-fail-wait");

	Instance instanceApp1 = cta.createInstance(null, null);

	S3GroupAImpl ga1 = (S3GroupAImpl) instanceApp1.getServiceObject();

	ThreadWrapper wrapper = new ThreadWrapper(ga1);
	wrapper.setDaemon(true);
	wrapper.start();

	apam.waitForIt(3000);

	String message = "In case of dependency been marked as fail='wait', the thread should be blocked until the dependency is satisfied. During this test the thread did not block.";

	Assert.assertTrue(message, wrapper.isAlive());
    }

    @Test
    public void CompositeDependencyFailException_tc045() {

	Implementation group_a = (Implementation) CST.apamResolver
		.findImplByName(null, "group-a-fail-exception");

	Instance instance_a = (Instance) group_a.createInstance(null, null);

	S3GroupAImpl ga1 = (S3GroupAImpl) instance_a.getServiceObject();

	String messageTemplate = "In dependency if we adopt fail='exception' exception='A', the exception A should be throw in case the dependency is not satifiable. %s";

	boolean exception = false;
	boolean exceptionType = false;

	try {

	    Eletronic injected = ga1.getElement();
	    System.out.println("Element:" + injected);

	} catch (Exception e) {
	    exception = true;

	    System.err
		    .println("-------------- Exception raised -----------------");

	    e.printStackTrace();

	    System.err
		    .println("-------------- /Exception raised -----------------");

	    if (e instanceof FailException) {
		exceptionType = true;
	    }

	}

	String messageException = String.format(messageTemplate,
		"But no exception was thrown");
	String messageExceptionType = String.format(messageTemplate,
		"But the exception thrown was not of the proper type (A)");

	Assert.assertTrue(messageException, exception);
	Assert.assertTrue(messageExceptionType, exceptionType);

    }

    @Test
    public void CompositeDependencyFailExceptionNative_tc052() {

	System.err.println("Pax.exam.framework property:"
		+ System.getProperty("pax.exam.framework"));
	System.err.println("bundle 0 symbolic-name:"
		+ context.getBundle(0).getSymbolicName());
	System.err
		.println(org.osgi.framework.Constants.FRAMEWORK_VENDOR
			+ " pax property:"
			+ context
				.getProperty(org.osgi.framework.Constants.FRAMEWORK_VENDOR));
	System.err.println("java Version:"
		+ System.getProperty("java.specification.version"));
	System.err
		.println("system packages:"
			+ context
				.getBundle(0)
				.getBundleContext()
				.getProperty(
					org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES));

	Implementation group_a = (Implementation) CST.apamResolver
		.findImplByName(null, "group-a-fail-exception-native");

	Instance instance_a = (Instance) group_a.createInstance(null, null);

	S3GroupAImpl ga1 = (S3GroupAImpl) instance_a.getServiceObject();

	String messageTemplate = "In dependency if we adopt fail='exception' exception='A' (With A being an exception that already exists in java JRE), the exception A should be throw in case the dependency is not satifiable. But the exception thrown was not type (A)";

	boolean exceptionType = false;

	try {

	    Eletronic injected = ga1.getElement();
	    System.out.println("Element:" + injected);

	} catch (Exception e) {

	    System.err
		    .println("-------------- Exception raised -----------------");

	    e.printStackTrace();

	    System.err
		    .println("-------------- /Exception raised -----------------");

	    if (e instanceof ParserConfigurationException) {
		exceptionType = true;
	    }

	}

	Assert.assertTrue(messageTemplate, exceptionType);

    }

    @Test
    public void CompositeContentMngtOwnSpecification_tc046() {

	CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
		null, "composite-a-own-specification");

	Composite composite_a = (Composite) cta.createInstance(null, null);

	Implementation device = CST.apamResolver.findImplByName(null,
		"BoschSwitch");
	Instance deviceinst = device.createInstance(null, null);

	String message = "When a composite declares to own a specification, that means every instance of that specification should be owned by that composite. This test failed, the actual owner composite of that component and the one that declares to be the owner are different";

	Assert.assertTrue(message, deviceinst.getComposite() == composite_a);

    }

    @Test
    @Ignore
    public void CompositeContentMngtDependencyHide_tc065() {

	CompositeType ctaroot = (CompositeType) CST.apamResolver
		.findImplByName(null, "composite-a-hide");

	Composite composite_root = (Composite) ctaroot.createInstance(null,
		null);// composite_root

	CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
		null, "composite-a-hide");

	Composite composite_a = (Composite) cta.createInstance(composite_root,
		null);// inner composite with hide='true'

	Instance instanceApp1 = composite_a.getMainInst();

	S3GroupAImpl ga1 = (S3GroupAImpl) instanceApp1.getServiceObject();
	// force injection
	ga1.getElement();

	auxListInstances("\t");

	List<Instance> instancesOfImplementation = auxLookForInstanceOf("fr.imag.adele.apam.pax.test.implS3.S3GroupAImpl");

	String messageTemplate = "Using hiding into a dependency of a composite should cause the instance of this component to be removed in case of an dependency of such componenent was satisfiable, instead the its instance is still visible. There are %d instances, and should be only 1 (the root composite that encloses the dependency with hide='true')";

	String message = String.format(messageTemplate,
		instancesOfImplementation.size());

	Assert.assertTrue(message, instancesOfImplementation.size() == 1);

    }

    @Test
    public void CompositeContentMngtDisputeAmongInjectionAndOwn_tc047() {

	Implementation sharedDependencyImpl = (Implementation) CST.apamResolver
		.findImplByName(null, "BoschSwitch");

	Instance sharedDependency = sharedDependencyImpl.createInstance(null,
		null);

	CompositeType compositeAImpl = (CompositeType) CST.apamResolver
		.findImplByName(null, "composite-a");

	Composite compositeA = (Composite) compositeAImpl.createInstance(null,
		null);

	S3GroupAImpl s3b = (S3GroupAImpl) compositeA.getMainInst()
		.getServiceObject();
	s3b.getElement();

	System.out.println("Original composite:"
		+ sharedDependency.getComposite());

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	CompositeType compositeBImpl = (CompositeType) CST.apamResolver
		.findImplByName(null, "composite-a-dispute-inject-own");

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

	Implementation sharedDependencyImpl = (Implementation) CST.apamResolver
		.findImplByName(null, "BoschSwitch");

	CompositeType compositeAImpl = (CompositeType) CST.apamResolver
		.findImplByName(null, "composite-a");

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

	CompositeType compositeBImpl = (CompositeType) CST.apamResolver
		.findImplByName(null, "composite-a-dispute-inject-own");

	Composite compositeB = (Composite) compositeBImpl.createInstance(null,
		null);

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	System.out.println("Composite after the own composite instantiation:"
		+ sharedDependency.getComposite());

	String message = "Class A needs the instance (that is already located inside another composite) IC, when B composite (declaring that owns IC) is instantiated, the IC should receive as parent composite the composite B. This did not happened";

	Assert.assertTrue(message,
		sharedDependency.getComposite() == compositeB);

    }

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

    @Test
    public void CompositeContentGrantTest_tct013() {
	CompositeType ct = (CompositeType) CST.apamResolver.findImplByName(
		null, "Yard_tct013");

	Implementation impl_daystate = (Implementation) CST.apamResolver
		.findImplByName(null, "DayState");
	Implementation impl_jackhammer = (Implementation) CST.apamResolver
		.findImplByName(null, "JackHammer_singleton");
	Implementation impl_worker = (Implementation) CST.apamResolver
		.findImplByName(null, "Worker_waiting");
	Implementation impl_toolmanager = (Implementation) CST.apamResolver
		.findImplByName(null, "ToolManager");

	Composite yard = (Composite) ct.createInstance(null, null);
	impl_jackhammer.createInstance(null, null);

	Worker worker1 = (Worker) impl_worker.createInstance(yard, null)
		.getServiceObject();

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	Instance dayinst = null;
	Instance managerinst = null;
	for (Instance inst : yard.getContainInsts()) {
	    if (inst.getImpl().equals(impl_daystate))
		dayinst = inst;
	    else if (inst.getImpl().equals(impl_toolmanager))
		managerinst = inst;
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
    public void CompositeContentSimpleReleaseGrantTest_tct014() {
	CompositeType ct = (CompositeType) CST.apamResolver.findImplByName(
		null, "Yard_tct013");

	Implementation impl_daystate = (Implementation) CST.apamResolver
		.findImplByName(null, "DayState");
	Implementation impl_jackhammer = (Implementation) CST.apamResolver
		.findImplByName(null, "JackHammer_singleton");
	Implementation impl_worker = (Implementation) CST.apamResolver
		.findImplByName(null, "Worker_waiting");
	Implementation impl_toolmanager = (Implementation) CST.apamResolver
		.findImplByName(null, "ToolManager");

	Composite yard = (Composite) ct.createInstance(null, null);
	impl_jackhammer.createInstance(null, null);

	Worker worker1 = (Worker) impl_worker.createInstance(yard, null)
		.getServiceObject();

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	Instance dayinst = null;
	Instance managerinst = null;
	for (Instance inst : yard.getContainInsts()) {
	    if (inst.getImpl().equals(impl_daystate))
		dayinst = inst;
	    else if (inst.getImpl().equals(impl_toolmanager))
		managerinst = inst;
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

    /**
     * This test should be changed accordingly to a new feature allowing to
     * force breaking a grant the link (some kind of \<deny\> markup )
     */
    @Test
    public void CompositeContentForcedReleaseGrantTest_tct015() {
	CompositeType ct = (CompositeType) CST.apamResolver.findImplByName(
		null, "Yard_tct015");

	Implementation impl_daystate = (Implementation) CST.apamResolver
		.findImplByName(null, "DayState_15");
	Implementation impl_jackhammer = (Implementation) CST.apamResolver
		.findImplByName(null, "JackHammer_singleton");
	Implementation impl_worker = (Implementation) CST.apamResolver
		.findImplByName(null, "Worker_waiting");
	Implementation impl_toolmanager = (Implementation) CST.apamResolver
		.findImplByName(null, "ToolManager");

	Composite yard = (Composite) ct.createInstance(null, null);
	impl_jackhammer.createInstance(null, null);

	Worker worker1 = (Worker) impl_worker.createInstance(yard, null)
		.getServiceObject();

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	Instance dayinst = null;
	Instance managerinst = null;
	for (Instance inst : yard.getContainInsts()) {
	    if (inst.getImpl().equals(impl_daystate))
		dayinst = inst;
	    else if (inst.getImpl().equals(impl_toolmanager))
		managerinst = inst;
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
    public void CompositeContentGrantToExternalTest_tct016() {
	CompositeType ct = (CompositeType) CST.apamResolver.findImplByName(
		null, "Yard_tct013");

	Implementation impl_daystate = (Implementation) CST.apamResolver
		.findImplByName(null, "DayState");
	Implementation impl_jackhammer = (Implementation) CST.apamResolver
		.findImplByName(null, "JackHammer_singleton");
	Implementation impl_worker = (Implementation) CST.apamResolver
		.findImplByName(null, "Worker_waiting");
	Implementation impl_toolmanager = (Implementation) CST.apamResolver
		.findImplByName(null, "ToolManager");

	Composite yard = (Composite) ct.createInstance(null, null);
	impl_jackhammer.createInstance(null, null);

	Worker worker1 = (Worker) impl_worker.createInstance(null, null)
		.getServiceObject();

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	Instance dayinst = null;
	Instance managerinst = null;
	for (Instance inst : yard.getContainInsts()) {
	    if (inst.getImpl().equals(impl_daystate))
		dayinst = inst;
	    else if (inst.getImpl().equals(impl_toolmanager))
		managerinst = inst;
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
	CompositeType ct = (CompositeType) CST.apamResolver.findImplByName(
		null, "Yard_tct017");

	Implementation impl_daystate = (Implementation) CST.apamResolver
		.findImplByName(null, "DayState_17");
	Implementation impl_jackhammer = (Implementation) CST.apamResolver
		.findImplByName(null, "JackHammer_multiple");
	Implementation impl_worker = (Implementation) CST.apamResolver
		.findImplByName(null, "Worker_waiting_exists");
	Implementation impl_toolmanager = (Implementation) CST.apamResolver
		.findImplByName(null, "ToolManager_17");

	Composite yard = (Composite) ct.createInstance(null, null);
	impl_jackhammer.createInstance(null, null);

	Worker worker1 = (Worker) impl_worker.createInstance(yard, null)
		.getServiceObject();

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	Instance dayinst = null;
	Instance managerinst = null;
	for (Instance inst : yard.getContainInsts()) {
	    if (inst.getImpl().equals(impl_daystate))
		dayinst = inst;
	    else if (inst.getImpl().equals(impl_toolmanager))
		managerinst = inst;
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
    public void DependencyRelease_tct018 () {
	Implementation implSource = CST.apamResolver.findImplByName(null,
		"ServiceDependencySource_tct018");
	Implementation implTarget = CST.apamResolver.findImplByName(null,
		"ServiceDependencyTarget_tct018");
	apam.waitForIt(Constants.CONST_WAIT_TIME);
	
	Instance instSourceA = implSource.createInstance(null, null);
	Instance instSourceB = implSource.createInstance(null, null);
	Instance instTarget = implTarget.createInstance(null, null);
	
	ServiceDependencySource_tct018 sourceA= (ServiceDependencySource_tct018) instSourceA.getServiceObject();
	ServiceDependencySource_tct018 sourceB= (ServiceDependencySource_tct018) instSourceB.getServiceObject();
	
	boolean exceptionThrown=false;
	
	  try {
	      sourceA.getAndKeepTarget();
	      System.out.println("Source A resolved target, but keeping");
	      sourceB.getAndReleaseTarget();
	      System.out.println("Source B resolved target");
	    } catch (Throwable ex) {
		System.out.println("Exception thrown : "+ex.getMessage());
	      exceptionThrown=true;
	    }
	  Assert.assertTrue("Usual Case if A resolve and use a target (not shared), B cannot use it", exceptionThrown);
	  
	  exceptionThrown=false;
	  try {
	      sourceA.getAndReleaseTarget();
	      System.out.println("Source A resolved target, and released");
	      sourceB.getAndReleaseTarget();
	      System.out.println("Source B resolved target, and released");
	    } catch (Throwable ex) {
		System.out.println("Exception thrown : "+ex.getMessage());
	      exceptionThrown=true;
	    }
	  Assert.assertFalse("If A  set its field to null, dependency should be available for B", exceptionThrown);

    }    
    
    @Test
    public void CompositeContentGrantWrongImplementationTest_tct019() {
	CompositeType ct = (CompositeType) CST.apamResolver.findImplByName(
		null, "Yard_tct013");

	Implementation impl_daystate = (Implementation) CST.apamResolver
		.findImplByName(null, "DayState");
	Implementation impl_jackhammer = (Implementation) CST.apamResolver
		.findImplByName(null, "JackHammer_singleton");
	Implementation impl_worker = (Implementation) CST.apamResolver
		.findImplByName(null, "Worker_waiting_bis");
	Implementation impl_toolmanager = (Implementation) CST.apamResolver
		.findImplByName(null, "ToolManager");

	Composite yard = (Composite) ct.createInstance(null, null);
	impl_jackhammer.createInstance(null, null);

	Worker worker1 = (Worker) impl_worker.createInstance(yard, null)
		.getServiceObject();

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	Instance dayinst = null;
	Instance managerinst = null;
	for (Instance inst : yard.getContainInsts()) {
	    if (inst.getImpl().equals(impl_daystate))
		dayinst = inst;
	    else if (inst.getImpl().equals(impl_toolmanager))
		managerinst = inst;
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
    public void CompositeContentGrantWrongDependencyTest_tct020() {
	CompositeType ct = (CompositeType) CST.apamResolver.findImplByName(
		null, "Yard_tct013");

	Implementation impl_daystate = (Implementation) CST.apamResolver
		.findImplByName(null, "DayState");
	Implementation impl_jackhammer = (Implementation) CST.apamResolver
		.findImplByName(null, "JackHammer_singleton");
	Implementation impl_worker = (Implementation) CST.apamResolver
		.findImplByName(null, "Night_worker");
	Implementation impl_toolmanager = (Implementation) CST.apamResolver
		.findImplByName(null, "ToolManager");

	Composite yard = (Composite) ct.createInstance(null, null);
	impl_jackhammer.createInstance(null, null);

	apam.waitForIt(Constants.CONST_WAIT_TIME);
	
	Worker worker1 = (Worker) impl_worker.createInstance(yard, null)
		.getServiceObject();


	Instance dayinst = null;
	Instance managerinst = null;
	for (Instance inst : yard.getContainInsts()) {
	    if (inst.getImpl().equals(impl_daystate))
		dayinst = inst;
	    else if (inst.getImpl().equals(impl_toolmanager))
		managerinst = inst;
	    System.out.println("Contains : " + inst.getName());
	}
	System.out.println();

	Assert.assertNotNull("DayState is not found in the composite", dayinst);
	ToolManager manager = (ToolManager) managerinst.getServiceObject();
	DayState state = (DayState) dayinst.getServiceObject();
//	manager.printTools();
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
    

}
