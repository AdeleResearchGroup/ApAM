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

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(JUnit4TestRunner.class)
public class MetaSubstitutionTest extends ExtensionAbstract {

	@Override
	public List<Option> config(){
		List<Option> neu=super.config();
		neu.add(mavenBundle("fr.imag.adele.apam.tests.services","apam-pax-samples-iface").versionAsInProject());
		neu.add(mavenBundle("fr.imag.adele.apam.tests.services","apam-pax-samples-impl-s6").versionAsInProject());
		return neu;
	}
	
	@Test
	public void SubstitutionGetPropertyString_tc089() {
		Implementation impl = CST.apamResolver.findImplByName(null,
				"MetasubstitutionStringTest");
		
		Instance samsungInst = impl.createInstance(null, null);
		
		auxListProperties("\t", samsungInst);
		
		Assert.assertTrue("geting property didnt work as expected",samsungInst.getProperty("meta_string_retrieve").equals("goethe"));
		Assert.assertTrue("prefixing didnt work as expected",samsungInst.getProperty("meta_string_prefix").equals("pregoethe"));
		Assert.assertTrue("postfixing didnt work as expected",samsungInst.getProperty("meta_string_suffix").equals("goethepost"));
		Assert.assertTrue("applying prefix and sufix at same time didnt work as expected",samsungInst.getProperty("meta_string_prefix_suffix").equals("pregoethepost"));
		
	}
	
}
