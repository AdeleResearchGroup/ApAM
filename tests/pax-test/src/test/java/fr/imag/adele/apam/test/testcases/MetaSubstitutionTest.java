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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.pax.test.implS6.S6Impl;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(JUnit4TestRunner.class)
public class MetaSubstitutionTest extends ExtensionAbstract {

	@Override
	public List<Option> config(){
		List<Option> neu=super.config();
		neu.add(mavenBundle("fr.imag.adele.apam.tests.services","apam-pax-samples-iface").versionAsInProject());
		neu.add(mavenBundle("fr.imag.adele.apam.tests.services","apam-pax-samples-impl-s6").versionAsInProject());
		neu.add(packApamShell());
		return neu;
	}
	
	@Test
	public void SubstitutionGetPropertyString_tc089() {
		Implementation impl = CST.apamResolver.findImplByName(null,
				"MetasubstitutionStringTest");
		
		Instance instance = impl.createInstance(null, null);
		
		auxListProperties("\t", instance);
		
		Assert.assertTrue("geting property didnt work as expected",instance.getProperty("meta_string_retrieve").equals("goethe"));
		Assert.assertTrue("prefixing didnt work as expected",instance.getProperty("meta_string_prefix").equals("pregoethe"));
		Assert.assertTrue("postfixing didnt work as expected",instance.getProperty("meta_string_suffix").equals("goethepost"));
		Assert.assertTrue("applying prefix and sufix at same time didnt work as expected",instance.getProperty("meta_string_prefix_suffix").equals("pregoethepost"));
		
	}
	
	@Test
	public void SubstitutionGetPropertyOutsideDefinitionInSpecPropertyInImpl_tc090() {
		Implementation subjectAimpl = CST.apamResolver.findImplByName(null,
				"subject-a");
		
		Instance subjectA = subjectAimpl.createInstance(null, null);
		
		auxListProperties("\t", subjectA);
		
		Assert.assertTrue("Given two composites A B, was not possible to reach the right value for a property of A through B by substituion (e.g. in B declare a property with the value '$AImpl.$property') ",subjectA.getProperty("property-case-01").equals("value-impl"));
		
	}
	
	@Test
	public void SubstitutionGetPropertyOutsideDefinictionInSpecPropertyNowhere_tc091() {
		Implementation subjectAimpl = CST.apamResolver.findImplByName(null,
				"subject-a");
		
		Instance subjectA = subjectAimpl.createInstance(null, null);
		
		auxListProperties("\t", subjectA);
		
		Assert.assertTrue("Given two composites A B, was not possible to reach the right value for a property of A through B by substituion (e.g. in B declare a property with the value '$AImpl.$property'): when there is only a definition in the Spec and no property in the Impl",subjectA.getProperty("property-case-03").equals("value-spec"));
		
	}

	@Test
	public void SubstitutionGetPropertyOutsideDefinitionNowherePropertyInImpl_tc092() {
		Implementation subjectAimpl = CST.apamResolver.findImplByName(null,
				"subject-a");
		
		Instance subjectA = subjectAimpl.createInstance(null, null);
		
		auxListProperties("\t", subjectA);
		
		System.err.println(subjectA.getProperty("property-case-08"));
		
		Assert.assertTrue("Given two composites A B, was not possible to reach the right value for a property of A through B by substituion (e.g. in B declare a property with the value '$AImpl.$property'): when there is only a definition in the Impl",subjectA.getProperty("property-case-08")!=null&&subjectA.getProperty("property-case-08").equals("value-impl"));
		
	}
	
	@Test
	public void SubstitutionGetPropertyEscaped_tc095() {
		Implementation subjectAimpl = CST.apamResolver.findImplByName(null,
				"subject-a");
		
		Instance subjectA = subjectAimpl.createInstance(null, null);
		
		auxListProperties("\t", subjectA);
		
		String templace="after fetching a property value (pointing to metasubstitution) with '$' escaped (with backslash), the content should not be processed by metasubtitution. Value was %s instead of %s";
		String message=String.format(templace,subjectA.getProperty("property-case-09"),"$impl-case-09.$property-subject-b");
		
		Assert.assertTrue(message,subjectA.getProperty("property-case-09").equals("$impl-case-09.$property-subject-b"));
		
	}
	
	@Test
	public void SubstitutionReachingMultipleNodes_tc122() {
		Implementation subjectAimpl = CST.apamResolver.findImplByName(null,
				"subject-a");
		
		Implementation implementationAlpha=CST.apamResolver.findImplByName(null,
				"impl-case-12-child");
		Instance instanceAlpha=implementationAlpha.createInstance(null, new HashMap<String, String>(){{put("property-subject-b", "alpha(child)");}});
		
		Implementation implementationBravo=CST.apamResolver.findImplByName(null,
				"impl-case-12-child");
		
		Instance instanceBravo=implementationBravo.createInstance(null, new HashMap<String, String>(){{put("property-subject-b", "bravo(child)");}});
		
		Implementation implementationCharlie=CST.apamResolver.findImplByName(null,
				"impl-case-12");
		Instance instanceCharlie=implementationCharlie.createInstance(null, new HashMap<String, String>(){{put("property-subject-b", "charlie(parent)");}});
		
		Implementation implementationDelta=CST.apamResolver.findImplByName(null,
				"impl-case-12");
		Instance instanceDelta=implementationDelta.createInstance(null, new HashMap<String, String>(){{put("property-subject-b", "delta(parent)");}});
		
		//Instance of the subject-a (parent)
		Instance subjectA = subjectAimpl.createInstance(null, null);
		
		//Force the field to be injected
		S6Impl s6parent=(S6Impl)subjectA.getServiceObject();
		s6parent.getS6();

		//Force the field to be injected
		Instance middleInstance=auxListInstanceReferencedBy(s6parent.getS6());
		S6Impl s6middle=(S6Impl)middleInstance.getServiceObject();
		s6middle.getS6();
		
		//Force the field to be injected
		Instance childInstance=auxListInstanceReferencedBy(s6middle.getS6());
		S6Impl s6child=(S6Impl)childInstance.getServiceObject();
		s6child.getS6();
		
		auxListProperties("\t", subjectA);
		
		System.err.println("-->"+subjectA.getProperty("property-case-12"));
		
		Assert.assertTrue(String.format("Substitution did not find the correct value when navigating through multiple nodes (Expecting %s as property, but found %s)",
				subjectA.getProperty("property-case-12"),childInstance.getProperty("property-subject-b")),
				subjectA.getProperty("property-case-12").equals(childInstance.getProperty("property-subject-b")));
		
	}
	
	@Test
	public void SubstitutionReachingMultipleNodesWithMembersKeyword_tc123() {

		Implementation subjectAimpl = CST.apamResolver.findImplByName(null,
				"subject-a");
		
		Implementation implementationAlpha=CST.apamResolver.findImplByName(null,
				"impl-case-12-child");
		Instance instanceAlpha=implementationAlpha.createInstance(null, new HashMap<String, String>(){{put("property-subject-b", "alpha(child)");}});
		
		Implementation implementationBravo=CST.apamResolver.findImplByName(null,
				"impl-case-12-child");
		
		Instance instanceBravo=implementationBravo.createInstance(null, new HashMap<String, String>(){{put("property-subject-b", "bravo(child)");}});
		
		Implementation implementationCharlie=CST.apamResolver.findImplByName(null,
				"impl-case-12");
		Instance instanceCharlie=implementationCharlie.createInstance(null, new HashMap<String, String>(){{put("property-subject-b", "charlie(parent)");}});
		
		Implementation implementationDelta=CST.apamResolver.findImplByName(null,
				"impl-case-12");
		Instance instanceDelta=implementationDelta.createInstance(null, new HashMap<String, String>(){{put("property-subject-b", "delta(parent)");}});
		
		//Instance of the subject-a (parent)
		Instance subjectA = subjectAimpl.createInstance(null, null);
		
		//Force the field to be injected
		S6Impl s6parent=(S6Impl)subjectA.getServiceObject();
		s6parent.getS6();

		//Force the field to be injected
		Instance middleInstance=auxListInstanceReferencedBy(s6parent.getS6());
		S6Impl s6middle=(S6Impl)middleInstance.getServiceObject();
		s6middle.getS6();
		
		//Force the field to be injected
		Instance childInstance=auxListInstanceReferencedBy(s6middle.getS6());
		S6Impl s6child=(S6Impl)childInstance.getServiceObject();
		s6child.getS6();
		
		auxListProperties("\t", subjectA);

		System.err.println(subjectA.getProperty("property-case-13"));
		Set<String> properties=(Set<String>)subjectA.getPropertyObject("property-case-13");
		
		
		Assert.assertTrue(String.format("Trying to reach all instance of a given implementation using 'members' keyword in substitution should result two instances but at least one is missing: %s",instanceAlpha.getProperty("property-subject-b")),
				properties.contains(instanceAlpha.getProperty("property-subject-b") ));
		
		Assert.assertTrue(String.format("Trying to reach all instance of a given implementation using 'members' keyword in substitution should result two instances but at least one is missing: %s",instanceBravo.getProperty("property-subject-b")),
				properties.contains(instanceBravo.getProperty("property-subject-b")));
		
	}
	
	
	@Test
	public void SubstitutionReachingMultipleNodesWithKeywordCompositeType_tc124() {

		Implementation subjectBimpl = CST.apamResolver.findImplByName(null,
				"subject-b");
		
		CompositeType subjectBCompositeType = (CompositeType)CST.apamResolver.findImplByName(null,
				"subject-b-composite");
		Composite subjectBComposite=(Composite)subjectBCompositeType.createInstance(null, Collections.<String,String>emptyMap()); 
		
		//Instance of the subject-a (parent)
		Instance subjectB = subjectBimpl.createInstance(subjectBComposite, null);
		
		auxListProperties("\t", subjectB);

		System.err.println(subjectB.getProperty("property-case-14"));
		
		Assert.assertTrue(String.format("Using metasubstituion to retrieve the name of the compositetype in which a given component is in did not fetch the right composite (%s)",subjectB.getProperty("property-case-14")),
				subjectB.getProperty("property-case-14").equals(subjectBCompositeType.getProperty("name")));
		
	}
	
	@Test
	public void SubstitutionReachingMultipleNodesWithKeywordComposite_tc125() {

		Implementation subjectCimpl = CST.apamResolver.findImplByName(null,
				"subject-c");
		
		CompositeType subjectCCompositeType = (CompositeType)CST.apamResolver.findImplByName(null,
				"subject-c-composite");
		Composite subjectCComposite=(Composite)subjectCCompositeType.createInstance(null, Collections.<String,String>emptyMap()); 
		
		//Instance of the subject-a (parent)
		Instance subjectC = subjectCimpl.createInstance(subjectCComposite, null);
		
		auxListProperties("\t", subjectC);

		System.err.println(subjectC.getProperty("property-case-15"));
		
		Assert.assertTrue(String.format("Using metasubstituion to retrieve the name of the composite in which a given component is in did not fetch the right composite (%s)",subjectC.getProperty("property-case-15")),
				subjectC.getProperty("property-case-15").equals(subjectCComposite.getProperty("name")));
		
	}
	
	@Test
	public void SubstitutionReachingMultipleNodesWithKeywordComposite_tc127() {

		CompositeType subjectECompositeType = (CompositeType)CST.apamResolver.findImplByName(null,
				"subject-e-composite");
		Composite subjectEComposite=(Composite)subjectECompositeType.createInstance(null, Collections.<String,String>emptyMap()); 
		
		Implementation implementationAlpha=CST.apamResolver.findImplByName(null,
				"impl-case-17");
		Instance instanceEcho=implementationAlpha.createInstance(subjectEComposite, null);
		
		Implementation subjectCimpl = CST.apamResolver.findImplByName(null,
				"subject-e");
		
		//Instance of the subject-a (parent)
		Instance subjectE = subjectCimpl.createInstance(null, null);
		S6Impl s6=(S6Impl) subjectE.getServiceObject();
		
		//Force injection
		s6.getS6();
		
		auxListProperties("\t", subjectE);
		
		String parentProperty=subjectE.getProperty("property-case-17-parent-composite");
		String dependencyProperty=subjectE.getProperty("property-case-17-dep-composite");
		
		String template="Using metasubstitution, with components in different composites , %s";
		
		Assert.assertTrue(String.format(template,"although the dependency do not correspond to the correct one"),dependencyProperty.equals(subjectEComposite.getName()));
		
		Assert.assertTrue(String.format(template,"although the parent do not correspond the correct one"),parentProperty==null);
		
		Assert.assertTrue(String.format(template,"although checking a dependency property we vefiried that the value do not match with the right one"),subjectE.getProperty("property-case-17-dep-property").equals(instanceEcho.getProperty("property-case-17")));
				
	}
	
	@Test
	public void SubstitutionReachingMultipleNodesWithKeywordGroup_tc126() {

		Implementation subjectDimpl = CST.apamResolver.findImplByName(null,
				"subject-d");
		
		CompositeType subjectCCompositeType = (CompositeType)CST.apamResolver.findImplByName(null,
				"subject-d-composite");
		Composite subjectDComposite=(Composite)subjectCCompositeType.createInstance(null, Collections.<String,String>emptyMap()); 
		
		//Instance of the subject-a (parent)
		Instance subjectD = subjectDimpl.createInstance(subjectDComposite, null);
		
		auxListProperties("\t", subjectD);
		
		Assert.assertTrue("Trying to recover the name of the group, but the name found do not correspond to the real group of the implem",subjectD.getProperty("property-case-16").equals("spec-case-16"));
		
	}
	
	@Test
	public void FunctionCall_tc093() {
		Implementation subjectAimpl = CST.apamResolver.findImplByName(null,
				"subject-a");
		
		Instance subjectA = subjectAimpl.createInstance(null, null);
		
		S6Impl s6=(S6Impl)subjectA.getServiceObject();
		
		auxListProperties("\t", subjectA);
		
		String template="after fetching a property value (pointing to a function) the returned value do not correspond to the returned function. Value '%s' was returned instead of '%s'";
		String message=String.format(template,subjectA.getProperty("function-case-01"),s6.functionCall(null));
		
		Assert.assertTrue(message,subjectA.getProperty("function-case-01").equals(s6.functionCall(null)));
	}
	
	@Test
	public void FunctionCallEscaped_tc094() {
		Implementation subjectAimpl = CST.apamResolver.findImplByName(null,
				"subject-a");
		
		Instance subjectA = subjectAimpl.createInstance(null, null);
		
		auxListProperties("\t", subjectA);
		
		String template="after fetching a property value (pointing to a function which the '@' was escaped with backslash) the returned value do not correspond to the returned function. Value '%s' was returned instead of '%s'";
		String message=String.format(template,subjectA.getProperty("function-case-01"),"@functionCall");
		
		Assert.assertTrue(message,subjectA.getProperty("function-case-02").equals("@functionCall"));
		
	}
	

	@Test
	public void SubstitutionGetPropertyWithDotInMiddleOfComponentName_tc117() {
		Implementation subjectAimpl = CST.apamResolver.findImplByName(null,
				"subject-a");
		
		Instance subjectA = subjectAimpl.createInstance(null, null);
		
		auxListProperties("\t", subjectA);
		
		System.err.println("--->"+subjectA.getProperty("property-case-10"));
		
		Assert.assertTrue("Substitution did not work when the component contains . (dots) in the name",subjectA.getProperty("property-case-10").equals("value-impl"));
		
	}
	
	@Test
	public void SubstitutionGetPropertyWithDotInMiddleOfComponentName_tc093() {
		Implementation subjectAimpl = CST.apamResolver.findImplByName(null,
				"impl-case-11");
		
		Instance subjectA = subjectAimpl.createInstance(null, Collections.singletonMap("property-subject-b", "bete"));
		
		auxListProperties("\t", subjectA);
		
		System.err.println("--->"+subjectA.getProperty("property-case-10"));
		
		//Assert.assertTrue("Substitution did not work when the component contains . (dots) in the name",subjectA.getProperty("property-case-10").equals("value-impl"));
		
		Assert.assertTrue(subjectA.getProperty("property-subject-b-spec").equals("mydefault"));
		Assert.assertTrue(subjectA.getProperty("property-subject-b").equals("bete"));
	}
	
}
