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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;

import fr.imag.adele.apam.tests.app.ClientObject;

import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class VersionPropertyTest extends ExtensionAbstract {

	private static Logger logger = LoggerFactory.getLogger(VersionPropertyTest.class);

	@Override
	public List<Option> config() {
		Map<String, String> mapOfRequiredArtifacts = new HashMap<String, String>();

//		mapOfRequiredArtifacts.put("implem-server-v1",
//				"fr.imag.adele.apam.tests.services");
//		mapOfRequiredArtifacts.put("implem-server-v2",
//				"fr.imag.adele.apam.tests.services");
		mapOfRequiredArtifacts.put("implem-client",
				"fr.imag.adele.apam.tests.services");

		List<Option> addon = super.config(mapOfRequiredArtifacts, false);
		return addon;
	}
	
	@Test
	public void getNewPredefinedProperties_tct039()
			throws InvalidSyntaxException {

		Implementation client = waitForImplByName(null, "implem-client");
		System.err.println("properties, "+client.getImplDeclaration().getProperties());
		Assert.assertNotNull("client must exists", client);
		Assert.assertNotNull("Property maven.groupId must exist ",client.getPropertyObject("maven.groupId"));
		Assert.assertEquals("Property maven.groupId type is a string ", "string", client.getPropertyDefinition("maven.groupId").getType());
		
		Assert.assertNotNull("Property maven.artifactId must exist ",client.getPropertyObject("maven.artifactId"));
		Assert.assertEquals("Property maven.artifactId type is a string ", "string", client.getPropertyDefinition("maven.artifactId").getType());
		
		Assert.assertNotNull("Property maven.version must exist ",client.getPropertyObject("maven.version"));
		Assert.assertEquals("Property maven.version type is a string ", "string", client.getPropertyDefinition("maven.version").getType());
		
		Assert.assertNotNull("Property apam.version must exist ",client.getPropertyObject("apam.version"));
		Assert.assertEquals("Property apam.version type is a version ", "version", client.getPropertyDefinition("apam.version").getType());
		
		Assert.assertNotNull("Property version must exist ",client.getPropertyObject("version"));
		Assert.assertEquals("Property version type is a version ", "version", client.getPropertyDefinition("version").getType());
	}
	
	@Test
	public void getPropertyVersion_tct040()
			throws InvalidSyntaxException {

		Implementation client = waitForImplByName(null, "implem-client");
		Assert.assertNotNull("client must exists", client);
		System.err.println("Client implem properties, "+client.getImplDeclaration().getProperties());

		System.err.println("version : "+client.getProperty("testVersionProp"));		
		Assert.assertNotNull("Property testVersionProp must exist ",client.getPropertyObject("testVersionProp"));
		Assert.assertEquals("Property testVersionProp type is a version ", "version", client.getPropertyDefinition("testVersionProp").getType());
		Assert.assertEquals("Property testVersionProp value is 1.2.3 ", Version.parseVersion("1.2.3"), client.getPropertyObject("testVersionProp"));
		
		client.setProperty("testVersionDef", Version.parseVersion("4.5.6"));
		Assert.assertNotNull("Property testVersionDef must exist ",client.getPropertyObject("testVersionDef"));
		Assert.assertEquals("Property testVersionDef type is a version ", "version", client.getPropertyDefinition("testVersionDef").getType());
		Assert.assertEquals("Property testVersionDef value is 4.5.6 ", Version.parseVersion("4.5.6"), client.getPropertyObject("testVersionDef"));
		
		System.err.println("Creating an instance, defined properties should be inherited");

		Instance instClient = client.createInstance(null, null);
		System.err.println("Client instance properties, "+instClient.getAllProperties().entrySet());

		Assert.assertNotNull("Property testVersionProp must exist ",instClient.getPropertyObject("testVersionProp"));
		Assert.assertEquals("Property testVersionProp type is a version ", "version", instClient.getPropertyDefinition("testVersionProp").getType());
		Assert.assertEquals("Property testVersionProp value is 1.2.3 ", Version.parseVersion("1.2.3"), instClient.getPropertyObject("testVersionProp"));
		
		Assert.assertNotNull("Property testVersionDef must exist ",instClient.getPropertyObject("testVersionDef"));
		Assert.assertEquals("Property testVersionDef type is a version ", "version", instClient.getPropertyDefinition("testVersionDef").getType());
		Assert.assertEquals("Property testVersionDef value is 4.5.6 ", Version.parseVersion("4.5.6"), instClient.getPropertyObject("testVersionDef"));
		
		System.err.println("Test injection");
		ClientObject myObject = (ClientObject) instClient.getServiceObject();
		myObject.setMyVersionInjected(Version.parseVersion("7.8.9"));
		
		Assert.assertNotNull("Property testVersionInjected must exist ",instClient.getPropertyObject("testVersionInjected"));
		Assert.assertEquals("Property testVersionInjected type is a version ", "version", instClient.getPropertyDefinition("testVersionInjected").getType());
		Assert.assertEquals("Property testVersionInjected value is 7.8.9 ", Version.parseVersion("7.8.9"), instClient.getPropertyObject("testVersionInjected"));	
		
		instClient.setProperty("testVersionInjected", Version.parseVersion("1.0.1"));
		Assert.assertEquals("Property testVersionInjected value (from javaclass) ", Version.parseVersion("1.0.1"), myObject.getMyVersionInjected());

	}

}
