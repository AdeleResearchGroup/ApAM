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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.pax.test.lifecycle.Service;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class LifeCycleTest extends ExtensionAbstract {


	@Override
	public List<Option> config() {
		Map<String, String> mapOfRequiredArtifacts = new HashMap<String, String>();

		mapOfRequiredArtifacts.put("apam-pax-samples-lifecycle",
				"fr.imag.adele.apam.tests.app");

		List<Option> addon = super.config(mapOfRequiredArtifacts, false);
		return addon;
	}
	
	private Map<String,String> configuration;
	
	@Before
	public void initialize() {
		configuration = new HashMap<String, String>();
		configuration.put("instance.name", "LifecycleTest-Instance");
		configuration.put("selfDestroy","false");
	}
	

	@Test
	public void testFailedConstructor() {

		Implementation implem = waitForImplByName(null, "LifeCycleConstructorTest");
		Instance instance = implem.createInstance(null,configuration);
		Assert.assertNull("instance must not be created",instance);
	}

	@Test
	public void testNormalStart() {

		Implementation implem = waitForImplByName(null, "LifeCycleTest");
		Instance instance = implem.createInstance(null,configuration);
		Assert.assertNotNull("instance must be created",CST.apamResolver.findInstByName(null,configuration.get("instance.name")));
	}

	@Test
	public void testFailedStart() {

		Implementation implem = waitForImplByName(null, "LifeCycleTest");
		configuration.put("selfDestroy","true");
		Instance instance = implem.createInstance(null,configuration);
		Assert.assertNull("instance must not be created",CST.apamResolver.findInstByName(null,configuration.get("instance.name")));
	}

	@Test
	public void testNormalInvocation() {

		Implementation implem = waitForImplByName(null, "LifeCycleTest");
		Instance instance = implem.createInstance(null,configuration);
		
		Service service = (Service) instance.getServiceObject();
		service.action();
		
		Assert.assertNotNull("instance must not be destroyed",CST.apamResolver.findInstByName(null,configuration.get("instance.name")));
	}

	@Test
	public void testSelfDestroyInvocation() {

		Implementation implem = waitForImplByName(null, "LifeCycleTest");
		Instance instance = implem.createInstance(null,configuration);

		instance.setProperty("selfDestroy",true);

		Service service = (Service) instance.getServiceObject();
		service.action();
		
		Assert.assertNull("instance must be destroyed",CST.apamResolver.findInstByName(null,configuration.get("instance.name")));
	}

}
