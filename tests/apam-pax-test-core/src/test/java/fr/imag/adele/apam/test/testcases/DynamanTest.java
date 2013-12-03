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
public class DynamanTest extends ExtensionAbstract {

    @Test
    @Ignore
    public void CompositeContentMngtDependencyHide_tc065() {

	CompositeType ctaroot = (CompositeType) waitForImplByName(null,
		"composite-a-hide");

	Composite composite_root = (Composite) ctaroot.createInstance(null,
		null);// composite_root

	CompositeType cta = (CompositeType) waitForImplByName(null,
		"composite-a-hide");

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
    public void CompositeContentMngtStartTriggerByImplementation_tc043() {
	auxListInstances("INSTANCE-t1-");

	String checkingFor = "implementation";

	CompositeType composite = (CompositeType) waitForImplByName(null,
		"composite-a-start-by-" + checkingFor);
	Composite compositeInstance = (Composite) composite.createInstance(
		null, null);

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	Implementation trigger = waitForImplByName(null,
		"group-a-start-trigger");

	Instance triggerInstance = trigger.createInstance(compositeInstance,
		null);

	Implementation triggered = waitForImplByName(null,
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
    public void CompositeContentMngtStartTriggerBySpecification_tc042() {
	auxListInstances("INSTANCE-t1-");

	String checkingFor = "specification";

	CompositeType composite = (CompositeType) waitForImplByName(null,
		"composite-a-start-by-" + checkingFor);
	Composite compositeInstance = (Composite) composite.createInstance(
		null, null);

	apam.waitForIt(Constants.CONST_WAIT_TIME);

	Implementation trigger = waitForImplByName(null,
		"group-a-start-trigger");

	Implementation triggered = waitForImplByName(null,
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
    public void CompositeWithEagerDependency_tc041() {
	CompositeType ct1 = (CompositeType) waitForImplByName(null,
		"S2Impl-composite-eager");

	String message = "During this test, we enforce the resolution of the dependency by signaling dependency as eager='true'. %s";

	Assert.assertTrue(String.format(message,
		"Although, the test failed to retrieve the composite"),
		ct1 != null);

	auxListInstances("instances existing before the test-");

	Composite instanceComposite = (Composite) ct1.createInstance(null,
		new HashMap<String, String>());

	Implementation implS2 = waitForImplByName(null,
		"fr.imag.adele.apam.pax.test.implS2.S2Impl");

	Instance instance = implS2.createInstance(instanceComposite, null);

	Assert.assertTrue(String.format(message,
		"Although, the test failed to instantiate the composite"),
		instance != null);
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
	CompositeType ct1 = (CompositeType) waitForImplByName(null,
		"S2Impl-composite-eager-forceEager");

	String message = "During this test, we enforce the resolution of the dependency by signaling dependency as eager='true'. %s";

	Assert.assertTrue(String.format(message,
		"Although, the test failed to retrieve the composite"),
		ct1 != null);

	auxListInstances("instances existing before the test-");

	Composite instanceComposite = (Composite) ct1.createInstance(null,
		new HashMap<String, String>());

	Implementation implS2 = waitForImplByName(null,
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

	List<Option> addon = super.config(mapOfRequiredArtifacts, true);

	addon.add(systemPackage("javax.xml.parsers"));
	addon.add(0, packApamConflictManager());
	return addon;
    }

}
