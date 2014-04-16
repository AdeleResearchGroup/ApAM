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

import java.util.Collections;
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

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.declarations.PackageReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

/**
 * @author thibaud
 * 
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ResourcesTest extends ExtensionAbstract {
	
	
	final String exported="fr.imag.adele.apam.tests.app.exportpack1";

	@Override
	public List<Option> config() {

		Map<String, String> mapOfRequiredArtifacts = new HashMap<String, String>();
		mapOfRequiredArtifacts.put("export-pack1",
				"fr.imag.adele.apam.tests.services");

		List<Option> defaults = super.config(mapOfRequiredArtifacts, true);

		return defaults;
	}

	@Override
	public void setUp() {
		super.setUp();
		waitForApam();
	}

	@Test
	public void testExportedResource() {
		Implementation implementation = waitForImplByName(null,
				"Export-Pack1");

		Instance inst = implementation.createInstance(null,
				Collections.<String, String> emptyMap());
		
		PackageReference ref = new PackageReference(exported);
				
		Assert.assertNotNull("Implementation should provide resources (the package)", implementation.getProvidedResources());
		Assert.assertTrue("Implementation should provides "+exported,implementation.getProvidedResources().contains(ref));

		Assert.assertNotNull("Instance should provide resources (the package is inherited)", inst.getProvidedResources());
		Assert.assertTrue("Instance should provides "+exported,inst.getProvidedResources().contains(ref));		
	}

}
