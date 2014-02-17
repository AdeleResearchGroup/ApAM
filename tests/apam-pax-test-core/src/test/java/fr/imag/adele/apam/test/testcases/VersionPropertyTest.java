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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.impl.APAMImpl;
import fr.imag.adele.apam.pax.test.impl.deviceSwitch.PropertyChangeNotificationSwitch;
import fr.imag.adele.apam.pax.test.impl.deviceSwitch.PropertyInjectionTypeSwitch;
import fr.imag.adele.apam.pax.test.impl.deviceSwitch.PropertyTypeBooleanChangeNotificationSwitch;
import fr.imag.adele.apam.pax.test.impl.deviceSwitch.PropertyTypeIntChangeNotificationSwitch;
import fr.imag.adele.apam.pax.test.implS1.S1Impl;
import fr.imag.adele.apam.pax.test.implS1.S1Impl_tct021;
import fr.imag.adele.apam.pax.test.implS1.S1Impl_tct025;
import fr.imag.adele.apam.tests.app.Implems1tct026;
import fr.imag.adele.apam.tests.helpers.Constants;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class VersionPropertyTest extends ExtensionAbstract {

	private static Logger logger = LoggerFactory.getLogger(VersionPropertyTest.class);

	@Override
	public List<Option> config() {
		Map<String, String> mapOfRequiredArtifacts = new HashMap<String, String>();

		mapOfRequiredArtifacts.put("implem-server-v1",
				"fr.imag.adele.apam.tests.services");
		mapOfRequiredArtifacts.put("implem-server-v2",
				"fr.imag.adele.apam.tests.services");
		mapOfRequiredArtifacts.put("implem-client",
				"fr.imag.adele.apam.tests.services");

		List<Option> addon = super.config(mapOfRequiredArtifacts, false);
		return addon;
	}
	
	@Test
	public void getNewPredefinedProperties_tct039()
			throws InvalidSyntaxException {

		Implementation server1 = waitForImplByName(null, "implem-server");
		System.err.println("properties, "+server1.getImplDeclaration().getProperties());
		Assert.assertNotNull("server v1 must exists", server1);
		Assert.assertNotNull("Property maven.groupId must exist ",server1.getPropertyObject("maven.groupId"));
		Assert.assertNotNull("Property maven.artifactId must exist ",server1.getPropertyObject("maven.artifactId"));
		Assert.assertNotNull("Property maven.version must exist ",server1.getPropertyObject("maven.version"));
		Assert.assertNotNull("Property apam.version must exist ",server1.getPropertyObject("apam.version"));
	}
	
	@Test
	public void getPropertyVersion_tct040()
			throws InvalidSyntaxException {

		Implementation server1 = waitForImplByName(null, "implem-server");
		Assert.assertNotNull("server v1 must exists", server1);
		System.err.println("version : "+server1.getPropertyObject("version"));		
		Assert.assertNotNull("Property version must exist ",server1.getPropertyObject("version"));
		


	}

}
