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

import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.CompositeOption;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.ComponentBroker;

public abstract class ExtensionAbstract extends TestUtils {

	// Based on the current running, no test should take longer than 2 minute
	@Rule
	public TestRule globalTimeout = new ApamTimeoutRule(isDebugModeOn() ? null
			: 120000);

	@Rule
	public TestName name = new TestName();

	@Inject
	public BundleContext context;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public ApAMHelper apam;

	protected ComponentBroker broker;

	public List<Option> config() {

		List<Option> config = new ArrayList<Option>();

		config.add(packInitialConfig());
		config.add(packOSGi());
		config.add(packPax());
		config.add(packApamCore());
		//config.add(packApamObrMan());
		config.add(packAppForTestBundles());
		config.add(packLog());
		config.add(junitBundles());
		config.add(packDebugConfiguration());
		config.add(vmOption("-ea"));

		return config;
	}
	
	public List<Option> configWithoutTests() {

		List<Option> config = new ArrayList<Option>();

		config.add(packInitialConfig());
		config.add(packOSGi());
		config.add(packPax());
		config.add(packApamCore());
		config.add(packLog());
		config.add(junitBundles());
		config.add(packDebugConfiguration());
		config.add(vmOption("-ea"));

		return config;
	}

	// public List<Option> config() {
	//
	// List<Option> config = new ArrayList<Option>();
	// config.add(systemProperty("org.osgi.service.http.port").value("8080"));
	// config.add(cleanCaches());
	// config.add(systemProperty("logback.configurationFile").value(
	// "file:" + PathUtils.getBaseDir() + "/log/logback.xml"));
	// config.add(systemProperty(
	// "org.ops4j.pax.logging.DefaultServiceLog.level").value("NONE"));
	// config.add(mavenBundle().groupId("org.apache.felix")
	// .artifactId("org.apache.felix.ipojo").versionAsInProject());
	// config.add(mavenBundle().groupId("org.ow2.chameleon.testing")
	// .artifactId("osgi-helpers").versionAsInProject());
	// config.add(mavenBundle().groupId("org.osgi")
	// .artifactId("org.osgi.compendium").version("4.2.0"));
	// config.add(mavenBundle().groupId("org.apache.felix")
	// .artifactId("org.apache.felix.bundlerepository").versionAsInProject());
	// config.add(mavenBundle().groupId("org.ops4j.pax.url")
	// .artifactId("pax-url-mvn").versionAsInProject());
	// config.add(mavenBundle().groupId("fr.imag.adele.apam")
	// .artifactId("apam-bundle").versionAsInProject());
	// config.add(mavenBundle().groupId("fr.imag.adele.apam")
	// .artifactId("obrman").versionAsInProject());
	// config.add(mavenBundle("org.slf4j", "slf4j-api").versionAsInProject());
	//
	// config.add(mavenBundle("ch.qos.logback",
	// "logback-core").versionAsInProject());
	// config.add(mavenBundle("ch.qos.logback",
	// "logback-classic").versionAsInProject());
	// config.add(junitBundles());
	// config.add(mavenBundle("fr.imag.adele.apam.tests", "apam-helpers")
	// .versionAsInProject());
	//
	// config.add(vmOption("-ea"));
	// config.add(when(isDebugModeOn())
	// .useOptions(
	// vmOption(String
	// .format("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%d",
	// Constants.CONST_DEBUG_PORT)),
	// systemTimeout(0)));
	//
	// return config;
	// }

	protected CompositeOption packInitialConfig() {

		CompositeOption initial = new DefaultCompositeOption(systemProperty(
				"org.osgi.service.http.port").value("8080"), cleanCaches(),
				systemProperty("logback.configurationFile").value(
						"file:" + PathUtils.getBaseDir() + "/log/logback.xml"),
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level")
						.value("NONE"));

		return initial;
	}

	protected CompositeOption packApamCore() {

		CompositeOption apamCoreConfig = new DefaultCompositeOption(
				mavenBundle().groupId("fr.imag.adele.apam")
						.artifactId("apam-bundle").versionAsInProject(),
				mavenBundle("fr.imag.adele.apam.tests", "apam-helpers")
						.versionAsInProject());

		return apamCoreConfig;
	}

	protected CompositeOption packApamObrMan() {
		CompositeOption apamObrmanConfig = new DefaultCompositeOption(
				mavenBundle().groupId("fr.imag.adele.apam")
						.artifactId("obrman").versionAsInProject());

		return apamObrmanConfig;
	}

	protected CompositeOption packApamDynaMan() {
		CompositeOption apamObrmanConfig = new DefaultCompositeOption(
				mavenBundle("fr.imag.adele.apam", "dynaman")
						.versionAsInProject());

		return apamObrmanConfig;
	}

	protected CompositeOption packPax() {
		CompositeOption paxConfig = new DefaultCompositeOption(mavenBundle()
				.groupId("org.ops4j.pax.url").artifactId("pax-url-mvn")
				.versionAsInProject());
		return paxConfig;
	}

	protected CompositeOption packOSGi() {
		CompositeOption osgiConfig = new DefaultCompositeOption(mavenBundle()
				.groupId("org.apache.felix")
				.artifactId("org.apache.felix.ipojo").versionAsInProject(),
				mavenBundle().groupId("org.ow2.chameleon.testing")
						.artifactId("osgi-helpers").versionAsInProject(),
				mavenBundle().groupId("org.osgi")
						.artifactId("org.osgi.compendium").version("4.2.0"),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.bundlerepository")
						.versionAsInProject());

		return osgiConfig;

	}

	protected CompositeOption packLog() {
		CompositeOption logConfig = new DefaultCompositeOption(mavenBundle(
				"ch.qos.logback", "logback-core").versionAsInProject(),
				mavenBundle("ch.qos.logback", "logback-classic")
						.versionAsInProject(), mavenBundle("org.slf4j",
						"slf4j-api").versionAsInProject());

		return logConfig;
	}

	protected CompositeOption packDebugConfiguration() {
		CompositeOption debugConfig = new DefaultCompositeOption(
				when(isDebugModeOn())
						.useOptions(
								vmOption(String
										.format("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%d",
												Constants.CONST_DEBUG_PORT)),
								systemTimeout(0)));

		return debugConfig;
	}

	protected CompositeOption packAppForTestBundles() {

		CompositeOption testAppBundle = new DefaultCompositeOption(
				mavenBundle("fr.imag.adele.apam.tests", "apam-helpers").versionAsInProject(), 
				
				
				mavenBundle("fr.imag.adele.apam.tests.obrman.app1.private","APP1-MainImpl").versionAsInProject(),
				mavenBundle("fr.imag.adele.apam.tests.obrman.app1.private","APP1-MainSpec").versionAsInProject(), 
				mavenBundle("fr.imag.adele.apam.tests.obrman.app1.private","APP1-S1-Spec").versionAsInProject(), 
				mavenBundle("fr.imag.adele.apam.tests.obrman.app1.private","APP1-S2-Spec").versionAsInProject(), 
				mavenBundle("fr.imag.adele.apam.tests.obrman.app1.private","APP1-S3-Spec").versionAsInProject(), 
				mavenBundle("fr.imag.adele.apam.tests.obrman.app1.public","APP1-Spec").versionAsInProject(), 
				mavenBundle("fr.imag.adele.apam.tests.obrman.app2.private","APP2-MainImpl").versionAsInProject(), 
				mavenBundle("fr.imag.adele.apam.tests.obrman.app2.private","APP2-MainSpec").versionAsInProject(), 
				mavenBundle("fr.imag.adele.apam.tests.obrman.app2.public","APP2-Spec").versionAsInProject(),
				
				mavenBundle("fr.imag.adele.apam.tests.services","apam-pax-samples-iface").versionAsInProject(),
				mavenBundle("fr.imag.adele.apam.tests.services","apam-pax-samples-impl-s1").versionAsInProject(),
				mavenBundle("fr.imag.adele.apam.tests.services","apam-pax-samples-impl-s2").versionAsInProject(),
				mavenBundle("fr.imag.adele.apam.tests.services","apam-pax-samples-impl-s3").versionAsInProject(),
				
				mavenBundle("fr.imag.adele.apam.tests.messages","apam-pax-samples-msg").versionAsInProject(),
				mavenBundle("fr.imag.adele.apam.tests.messages","apam-pax-samples-impl-m1").versionAsInProject(),
				mavenBundle("fr.imag.adele.apam.tests.messages","apam-pax-samples-impl-m2").versionAsInProject(),
				mavenBundle("fr.imag.adele.apam.tests.messages","apam-pax-samples-impl-m3").versionAsInProject());
		return testAppBundle;

	}

	@Configuration
	public Option[] apamConfig() {

		Option conf[] = config().toArray(new Option[0]);

		return conf;
	}
	
	//@Configuration
	public Option[] apamConfig2(){
		List<Option> config = new ArrayList<Option>();

		config.add(packInitialConfig());
		config.add(packOSGi());
		config.add(packPax());
		config.add(packApamCore());
		config.add(packApamObrMan());
		config.add(packApamDynaMan());
		config.add(packLog());
		config.add(junitBundles());
		config.add(packDebugConfiguration());
		config.add(vmOption("-ea"));
		
		return config.toArray(new Option[0]);
	}

	@Before
	public void setUp() {
		apam = new ApAMHelper(context);
		broker = CST.componentBroker;
		logger.info("[Run Test : " + name.getMethodName() + "]");
		apam.waitForIt(1000);
	}

	@After
	public void tearDown() {
		apam.dispose();
	}

	private static boolean isDebugModeOn() {
		RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
		List<String> arguments = RuntimemxBean.getInputArguments();

		boolean debugModeOn = false;

		for (String string : arguments) {
			debugModeOn = string.indexOf("jdwp") != -1;
			if (debugModeOn)
				break;
		}

		return debugModeOn;
	}

}
