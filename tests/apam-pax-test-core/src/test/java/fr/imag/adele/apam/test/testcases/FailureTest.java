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
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.BootDelegationOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.pax.test.iface.device.Eletronic;
import fr.imag.adele.apam.pax.test.implS3.FailException;
import fr.imag.adele.apam.pax.test.implS3.S3GroupAImpl;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class FailureTest extends ExtensionAbstract {

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
    public void CompositeContentMngtDependencyFailException_tc040() {

	CompositeType ctroot = (CompositeType) waitForImplByName(null,
		"composite-a-fail-exception");

	CompositeType cta = (CompositeType) waitForImplByName(null,
		"composite-a-fail-exception");

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

    @Test
    public void CompositeContentMngtDependencyFailWait_tc039() {

	CompositeType cta = (CompositeType) waitForImplByName(null,
		"composite-a-fail-wait");

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
    public void CompositeDependencyFailException_tc045() {

	Implementation group_a = waitForImplByName(null,
		"group-a-fail-exception");

	Instance instance_a = group_a.createInstance(null, null);

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

	System.err
	.println("system boot delegation:"
		+ context
			.getBundle(0)
			.getBundleContext()
			.getProperty(
				org.osgi.framework.Constants.FRAMEWORK_BOOTDELEGATION));
	
	Implementation group_a = waitForImplByName(null,
		"group-a-fail-exception-native");

	Instance instance_a = group_a.createInstance(null, null);

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
    public void CompositeDependencyFailWait_tc044() {

	Implementation cta = waitForImplByName(null, "group-a-fail-wait");

	Instance instanceApp1 = cta.createInstance(null, null);

	S3GroupAImpl ga1 = (S3GroupAImpl) instanceApp1.getServiceObject();

	ThreadWrapper wrapper = new ThreadWrapper(ga1);
	wrapper.setDaemon(true);
	wrapper.start();

	apam.waitForIt(3000);

	String message = "In case of dependency been marked as fail='wait', the thread should be blocked until the dependency is satisfied. During this test the thread did not block.";

	Assert.assertTrue(message, wrapper.isAlive());
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

	List<Option> addon = super.config(mapOfRequiredArtifacts, false);

	addon.add(systemPackage("javax.xml.parsers"));
	addon.add(bootDelegationPackage("javax.xml.parsers"));
	addon.add(0, packApamConflictManager());
	return addon;
    }


}
