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
package fr.imag.adele.apam.mainApam;

//import static junit.framework.assertEquals.assertNotNull;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import apam.test.attr.TestAttr;


import fr.imag.adele.apam.ApamComponent;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.test.s1.S1;

public class MainApam implements Runnable, ApamComponent {
	// injected
	Apam apam;

	public void assertTrue (boolean test) {
		if (!test) {
			new Exception ("Assertion failed. Not true.").printStackTrace();
		}
	}

	public void assertEquals (Object left, Object right) {
		if (left == right) return ;
		if (left == null) {
			new Exception ("Assertion Equals failed: left side is null; right side = " + right).printStackTrace();
			return ;
		}
		if (right == null) {
			new Exception ("Assertion Equals failed right side is null; left side = " + left).printStackTrace();	
			return ;
		}
		if (left instanceof String && right instanceof String) {
			if (!left.equals(right)) {
				new Exception ("Assertion Equals failed: " + left + " != " + right).printStackTrace();
				return ;
			} else return ;
		} else {
			new Exception ("Assertion arguments not same type: " + left + " != " + right).printStackTrace();			
		}
	}

	public void assertNotEquals (Object left, Object right) {
		if (left != null && right != null) {
			if (left instanceof String && left.equals(right)) {
				new Exception ("Assertion NotEquals failed: " + left + " = " + right).printStackTrace();
			}
		}
		if (left == right) 
			new Exception ("Assertion NotEquals failed: " + left + " = " + right).printStackTrace();
	}


	public void testFindImplByName () {
		System.out.println("=====MODIFIED====== start testFindImplByName");
		System.out.println("testing findImplByName in OBR");

		System.out.println("Empty spec");
		Specification spec = CST.apamResolver.findSpecByName(null,"dummySpec");
		assertTrue(spec != null);

		System.out.println("Abstract composite with empty spec");
		Implementation implem = CST.apamResolver.findImplByName(null,"AbstractCompositeType");
		assertTrue(implem != null);

		System.out.println("Abstract composite without spec");
		implem = CST.apamResolver.findImplByName(null,"AbstractCompositeTypeNoSpec");
		assertTrue(implem != null);

		implem = CST.apamResolver.findImplByName(null,"S1toS2Final");
		assertTrue(implem != null);

		System.out.println("Deploying S1Impl bundle should deploy also the implems and composites. Composite S1CompoFinal is created and started.");
		System.out.println("Shoud appear the message \"S1toS2Final is sarted\" ");
		try {
			Thread.sleep(1000) ;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		assertTrue (CST.componentBroker.getInst("S1CompoFinal-Instance") != null) ;
		assertTrue (implem != null );	 	

		System.out.println("testing findImplByName in ASM and unused");
		Implementation implem2 = CST.apamResolver.findImplByName(null,"S2Final");
		assertTrue(implem2!= null);

		System.out.println("testing findImplByName in ASM  and used");
		implem2 = CST.apamResolver.findImplByName(null,"S2Simple");
		assertTrue(implem2!= null);
		System.out.println("=========== passed testFindImplByName\n\n");
	}

	@Deprecated
	public void testCreateCompoRootS1toS2Final () {
		System.out.println("=========== start testCreateCompoRootS1toS2Final");
		if (apam != null) {
			System.out.println("apam is Ok");
		}
		System.out.println("testing a root createCompositeType by name existing in ASM. will call S2Final.");
		CompositeType appliTest00 = apam.createCompositeType(null,  "Test00", null, "S1toS2Final", null,null);
		assertTrue(appliTest00!= null);

		System.out.println("testing create instance root");
		Instance a = appliTest00.createInstance(null /* composite */, null/* properties */);
		assertTrue(a!= null);

		System.out.println("testing call to S1toS2Final");
		S1 s01 = (S1) a.getServiceObject();
		s01.callS1("createAppli by API by name. Should call S2Final.");

		System.out.println("=========== passed testCreateCompoRootS1toS2Final\n\n");

		System.out.println("=========== start nested composite instance by API");
		System.out.println("Testing the manual creation of a composite instance inside another composite instance of same type");
		System.out.println("Weird but should work.");
		Instance test00_instance0 = appliTest00.createInstance((Composite) a /* composite */, null/* properties */);
		assertTrue (test00_instance0 != null);
		assertTrue (((Composite)a).containsInst(test00_instance0)) ;
		System.out.println("composite in composite same type !! " + test00_instance0 );

		System.out.println("=========== passed nested composite instance by API\n\n");
	}

	@Deprecated
	public void testCreateCompoBySpec () {
		System.out.println("=========== start testCreateCompoBySpec");
		System.out.println(" Alternatively, a specification (S1) can be provided instead; it is resolved to find the main implem.\n");

		// The system will look for an atomic implementations of "S1" as the main implementation
		CompositeType appli3 = apam.createCompositeType(null, "TestS1Bis", null,  "S1", null /* models */, null /* properties */);
		assertTrue (appli3 != null) ;

		// Create an instance of that composite type
		Instance inst = appli3.createInstance(null /* composite */, null/* properties */);
		assertTrue (inst != null );

		System.out.println("=========== passed testCreateCompoBySpec\n\n");
	}

	public void testInitialAttributes () {
		System.out.println("=========== start testInitialAttributes");
		Map<String, String> props = new HashMap<String, String>();
		props.put("testMain", "valeurTestMain"); // not declared
		props.put("scope", "5"); // redefined
		props.put("impl-name", "5"); // final
		//props.put("location", "living"); // ok
		props.put("location", "anywhere"); // value not defined
		props.put("testEnumere", "v1");

		CompositeType appliTestAttr = apam.createCompositeType(null,  "TestInitAttr", null, "TestAttr", null, props);
		assertTrue(appliTestAttr != null);

		Instance appliTestAttr_0 = appliTestAttr.createInstance(null /* composite */, props/* properties */);
		System.out.println("Tested instance : " + appliTestAttr_0.getName());

		//since the composite type has no spec, all initial values are valid.
		//No longer ! all attr must be defined now.
		assertTrue (appliTestAttr_0 != null) ;
		assertNotEquals (appliTestAttr_0.getProperty("testMain"), "valeurTestMain") ;
		assertNotEquals (appliTestAttr_0.getProperty("scope"), "5") ;
		assertNotEquals (appliTestAttr_0.getProperty("impl-name"), "5") ;
		assertNotEquals (appliTestAttr_0.getProperty("location") , "anywhere") ;
		//		assertTrue (appliTestAttr_0.getProperty("testEnumere") == null) ;

		System.out.println("=========== passed testInitialAttributes\n\n");
	}

	public void testSettingAttributes () {  	   	
		System.out.println("=========== start testSettingAttributes");

		/*
 		<specification name="STestAttr" interfaces="fr.imag.adele.apam.test.testAttr.TestAttr"  >
			<property name="S1-Enum" value="s1-2" type="{s1-1, s1-2, s1-3}"/>
			<property name="S1-Attr" value="coucou" type="string"/>
			<definition name="s1b" type="boolean" value="true" />
			<definition name="s1c" type="string" />
			<definition name="s1i" type="int" />
			<definition name="location" type="{living, kitchen, bedroom}" value="bedroom" />
			<definition name="testEnumere" type="{v1, v2, v3, v4}" />
			<definition name="OS" type="{Linux, Windows, Android, IOS}" />

	<implementation name="TestAttr"
		classname="fr.imag.adele.apam.test.testAttrImpl.TestAttrImpl" specification="STestAttr">
		<property name="spec-name" value="yyy"  />
		<property name="testEnumere" value="v2" />
		<definition name="fieldAttr" field="theFieldAttr" internal="true" type="string" value="bidon"/>
		<definition name="S1toS2Final-Bool" type="boolean" value="true" />
		<definition name="S1toS2Final-String1" type="string" />
		<definition name="S1toS2Final-location" type="{FinalLiving, FinalKitchen, FinalLedroom}" />
		<definition name="enumeration" type="{f1, f2, f3, f4}" />

	<instance implementation="S1toS2Final" name="S1toS2Final-instance" >
		<property S1toS2Final-Bool="xxx" />
		<property S1toS2Final-String1="a String Value" />
		<property badAttr="yyy" />

		 */

		CompositeType appliSetAttr = apam.createCompositeType(null,  "TestSetAttr", null, "TestAttr", null,null);
		assertTrue(appliSetAttr != null);

		Instance appliSetAttr_0 = appliSetAttr.createInstance(null /* composite */, null/* properties */);
		assertTrue (appliSetAttr_0 != null);
		//assertEquals ((CompositeImpl)appliSetAttr_0.getMainInst () != null);

		System.out.println(" Testing attributes on main implem declared: location = {living, kitchen, bedroom}");
		Implementation impl = appliSetAttr.getMainImpl(); //S1toS2Final
		Specification spec = impl.getSpec() ;
		Instance inst = null ;
		inst = impl.getInst() ; // any instance 
		System.out.println("Tested instance : " + inst + ". TestedImpl : " + impl + " Tested spec : " + spec);


		//check attributes defined in the xml
		System.out.println("=========== start testing xml attributes");
		assertEquals (spec.getProperty("S1-Attr"), "coucou") ; 
		assertNotEquals (spec.getProperty("s1-attr"), "coucou") ; //case sensitive for attr
		assertNotEquals (spec.getProperty("S1-Attr"), "Coucou") ; // case sensitive for value
		assertEquals (impl.getProperty("S1-Attr"), "coucou") ;
		assertEquals (inst.getProperty("S1-Attr"), "coucou") ;

		assertEquals (spec.getProperty("S1-Enum"), "s1-2") ; 
		assertEquals (impl.getProperty("S1-Enum"), "s1-2") ;
		assertEquals (inst.getProperty("S1-Enum"), "s1-2") ;

		//default values S1toS2Final-Attr
		assertNotEquals (spec.getProperty("s1b"), "true") ;
		assertEquals (impl.getProperty("s1b"), "true") ;
		assertEquals (inst.getProperty("s1b"), "true") ;
		assertEquals (impl.getProperty("location"), "bedroom") ;
		assertEquals (inst.getProperty("S1toS2Final-Bool"), "true") ;
		assertNotEquals (impl.getProperty("S1toS2Final-Bool"), "true") ;

		assertTrue (impl.getProperty("S1toS2Final-Attr") == null) ;
		assertTrue (inst.getProperty("S1toS2Final-Attr") == null) ;

		assertEquals (impl.getProperty("testEnumere"), "v2") ;
		assertEquals (inst.getProperty("testEnumere"), "v2") ;

		//		assertTrue (inst.getProperty("S1toS2Final-Bool") == null) ;
		//		assertEquals (inst.getProperty("S1toS2Final-String"), "a String Value") ;
		//		assertTrue (inst.getProperty("badAttr") == null) ;
		System.out.println("=========== passed testing xml attributes");


		System.out.println("=========== start testing setting attributes");

//		<property   name="autoString" value="aValue" type="string"/>
//		<property   name="autoSet" value="Z-1, Z-2" type="{Z-0, Z-1, Z-2, Z-3}"/>

		//setting auto defined attributes
		assertEquals (impl.getProperty("autoString"), "aValue") ;
		assertEquals (inst.getProperty("autoString"), "aValue") ;
		assertEquals (impl.getProperty("autoSet"), "Z-1, Z-2") ;
		assertEquals (inst.getProperty("autoSet"), "Z-1, Z-2") ;

		impl.setProperty("autoString", "New-value") ;
		assertEquals (impl.getProperty("autoString"), "New-value") ;
		assertEquals (inst.getProperty("autoString"), "New-value") ;
		impl.setProperty("autoSet", "Z-3, Z-2") ;
		assertEquals (impl.getProperty("autoSet"), "Z-3, Z-2") ;
		assertEquals (inst.getProperty("autoSet"), "Z-3, Z-2") ;

		impl.setProperty("S1toS2Final-String1", "falseVal") ;
		assertNotEquals(impl.getProperty("S1toS2Final-String1"), "falseVal"); 
		
		//Setting spec attributes. 
		spec.setProperty("xxx", "value") ;
		assertTrue (spec.getProperty("xxx") == null) ;

		spec.setProperty("S1-Attr", "New-value") ;
		assertEquals (spec.getProperty("S1-Attr"), "New-value") ;
		assertEquals (impl.getProperty("S1-Attr"), "New-value") ;
		assertEquals (inst.getProperty("S1-Attr"), "New-value") ;

		spec.setProperty("S1-Enum", "New-value") ; 
		assertEquals (spec.getProperty("S1-Enum"), "s1-2") ;
		spec.setProperty("S1-Enum", "s1-3") ;
		assertEquals (spec.getProperty("S1-Enum"), "s1-3") ;
		assertEquals (impl.getProperty("S1-Enum"), "s1-3") ;
		assertEquals (inst.getProperty("S1-Enum"), "s1-3") ;

		impl.setProperty("S1-Enum", "s1-1") ;
		assertEquals (spec.getProperty("S1-Enum"), "s1-3") ;
		assertEquals (impl.getProperty("S1-Enum"), "s1-3") ;
		assertEquals (inst.getProperty("S1-Enum"), "s1-3") ;

		impl.setProperty("location", "living"); // good
		assertEquals(impl.getProperty("location"), "living");
		assertEquals(inst.getProperty("location"), "living");

		impl.setProperty("location", "Living"); // error: Value is case sensitive
		assertEquals(impl.getProperty("location"), "living");

		impl.setProperty("location", "anywhere"); // error: false value
		assertEquals(impl.getProperty("location"), "living");

		impl.setProperty("testMain", "valeurTestMain"); // error: not defined
		assertTrue(impl.getProperty("testMain") == null);

		impl.setProperty("impl-name", "5"); // error: final attribute
		assertEquals(impl.getProperty("impl-name"), impl.getName());

		//boolean
		impl.setProperty("s1b", "5"); // error: bool attribute
		assertNotEquals(impl.getProperty("s1b"), "5");

		impl.setProperty("s1b", "true"); // Ok
		assertEquals(impl.getProperty("s1b"), "true");
		assertEquals(inst.getProperty("s1b"), "true");

		//integer
		impl.setProperty("s1i", "entier"); // error: int attribute
		assertTrue(impl.getProperty("s1i") == null);

		impl.setProperty("s1i", "5"); // Ok
		assertEquals(impl.getProperty("s1i"), "5");
		assertEquals(inst.getProperty("s1i"), "5");
		impl.setProperty("s1i", "5, 7, 55,985"); // Ok
		assertNotEquals(impl.getProperty("s1i"), "5, 7, 55,985");
		//		assertEquals(inst.getProperty("s1i"), "5, 7, 55,985");

		//		impl.setProperty("s1i", "5, zz, 55,985"); // wrong
		//		assertNotEquals(impl.getProperty("s1i"), "5, zz, 55,985");

		boolean ok  ;
		impl.setProperty("s1i", "5"); // for match tests
		assertTrue ( inst.match("(s1i>=4)")) ;
		assertTrue (!inst.match("(s1i<=4)")) ;
		assertTrue (!inst.match("(s1i>=6)")) ;
		assertTrue ( inst.match("(s1i<=6)")) ;
		assertTrue (!inst.match("(s1i>=10)")) ;
		assertTrue ( inst.match("(s1i<=10)")) ;
		assertTrue ( inst.match("(s1i>=01)")) ;
		assertTrue (!inst.match("(s1i<=01)")) ;
		assertTrue ( inst.match("(s1i>=-61)")) ;
		assertTrue (!inst.match("(s1i<=-61)")) ;

		impl.setProperty("S1-Attr", "5"); // error: cannot redefine
		assertEquals(spec.getProperty("S1-Attr"), "New-value");

		//field and internal. Set by program when starting
		assertEquals(inst.getProperty("fieldAttr"), "initial set by program");
		String s = "to set the field attribute" ;
		((TestAttr)inst.getServiceObject()).callS1 (s) ; //callS1 sets the attribute to s 
		assertEquals(inst.getProperty("fieldAttr"), s);
		s = "second value" ;
		((TestAttr)inst.getServiceObject()).callS1 (s) ; //callS1 sets the attribute to s 
		assertEquals(inst.getProperty("fieldAttr"), s);
		inst.setProperty("fieldAttr", "a Value") ;
		assertEquals(inst.getProperty("fieldAttr"), s);


		inst.setProperty("fieldAttr", "test"); // error: cannot set
		assertNotEquals(impl.getProperty("fieldAttr"), "test");

		//Instances can set spec attributes, if not defined by the implem
		inst.setProperty("OS", "Linux") ; // ok
		assertEquals(inst.getProperty("OS"), "Linux");
		inst.setProperty("OS", "Android, Linux, IOS") ; // ok
		assertEquals(inst.getProperty("OS"), "Android, Linux, IOS");
		assertTrue(impl.getProperty("OS") == null);

		System.out.println("\n");
		System.out.println("OS value is : " + inst.getProperty("OS"));
		System.out.println("toto does not exist. Its value is null");

		//non instantiated attributes
		ok = inst.match("(toto= Android  , Linux  , IOS)") ;
		System.out.println("Matching: (toto= Android  , Linux  , IOS): " + ok);
		ok = inst.match("(toto<*Android  , Linux  , IOS)") ;
		System.out.println("Matching: (toto<*Android  , Linux  , IOS): " + ok);
		ok = inst.match("(toto=testnull)") ;
		System.out.println("Matching: (toto=testnull): " + ok);
		ok = inst.match("(toto<*testnull,)") ;
		System.out.println("Matching: (toto<*testnull,): " + ok);
		ok = inst.match("(toto=testnull)") ;
		System.out.println("Matching: (toto<*testnull): " + ok);

		ok = inst.match("(toto=*)") ;
		System.out.println("Matching: (toto=*): " + ok);
		ok = inst.match("(OS=*)") ;
		System.out.println("Matching: (OS=*): " + ok);
		ok = inst.match("(OS=And*)") ;
		System.out.println("Matching: (OS=And*): " + ok);

		ok = inst.match("(OS*>And*)") ;
		System.out.println("Matching: (OS*>And*): " + ok);
		ok = inst.match("(OS<*And*,)") ;
		System.out.println("Matching: (OS<*And*,): " + ok);
		ok = inst.match("(OS<*And*,L*)") ;
		System.out.println("Matching: (OS<*And*,L*): " + ok);
		ok = inst.match("(OS<*And*)") ;
		System.out.println("Matching: (OS<*And*): " + ok);
		ok = inst.match("(OS=zz*)") ;
		System.out.println("Matching: (OS=zz*): " + ok);
		ok = inst.match("(OS<*zz*)") ;
		System.out.println("Matching: (O*>zz*): " + ok);

		System.out.println("");
		System.out.println("Matching: (OS *> Linux, Android, IOS, Windows): " + ok);
		ok = inst.match("(OS <* Linux, Android, IOS, Windows)")  ;
		System.out.println("Matching: (OS <* Linux, Android, IOS, Windows): " + ok);
		ok = inst.match("(OS *> Linux, Android)")  ;
		System.out.println("Matching: (OS *> Linux, Android): " + ok);
		ok = inst.match("(OS *> Android)")  ;
		System.out.println("Matching: (OS *> Android): " + ok);
		ok = inst.match("(OS *> Android,)")  ;
		System.out.println("Matching: (OS *> Android,): " + ok);
		ok = inst.match("(OS=Android)")  ;
		System.out.println("Matching: (OS=Android): " + ok);		
		ok = inst.match("(OS >=Android)")  ;
		System.out.println("Matching: (OS>=Android): " + ok);		

		ok = inst.match("(OS >=Linux, Android, IOS)")  ;
		System.out.println("Matching: (OS>=Linux, Android, IOS): " + ok);		
		ok = inst.match("(OS >=Android, Linux, IOS)")  ;
		System.out.println("Matching: (OS>=Android, Linux, IOS): " + ok);		
		ok = inst.match("(OS >=Android, Linux   , IOS)")  ;
		System.out.println("Matching: (OS>=Android, Linux   , IOS): " + ok);		


		ok = inst.match("(OS *> Linux, Android, IOS)") ;
		System.out.println("Matching: (OS *> Linux, Android, IOS): " + ok);
		ok = inst.match("(OS <* Linux, Android, IOS)") ;
		System.out.println("Matching: (OS <* Linux, Android, IOS): " + ok);

		ok = inst.match("(OS = Linux, Android, IOS)") ;
		System.out.println("Matching: (OS = Linux, Android, IOS): " + ok);

		ok = inst.match("(OS = Android  , Linux  , IOS)") ;
		System.out.println("Matching: (OS = Android  , Linux  , IOS): " + ok);

		ok = inst.match("(OS *> Linux, Android, IOS,)") ;
		System.out.println("Matching: (OS *> Linux, Android, IOS,): " + ok);
		ok = inst.match("(OS <* Linux, Android, IOS,)") ;
		System.out.println("Matching: (OS <* Linux, Android, IOS,): " + ok);
		ok = inst.match("(OS *> {Linux, Android, IOS})") ;
		System.out.println("Matching: (OS *> {Linux, Android, IOS}): " + ok);
		ok = inst.match("(OS *> [Linux, Android, IOS])") ;
		System.out.println("Matching: (OS *> [Linux, Android, IOS]): " + ok);
		ok = inst.match("(OS *> Linux, Android, IOS)") ;
		System.out.println("Matching: (OS *> Linux, Android, IOS): " + ok);

		inst.setProperty("OS", "Linux, Android, IOS,") ; // ok
		System.out.println("\nOS value is : " + inst.getProperty("OS"));
		ok = inst.match("(OS = Android)")  ;
		System.out.println("Matching: (OS=Android): " + ok);		
		ok = inst.match("(OS >=Android)")  ;
		System.out.println("Matching: (OS >=Android): " + ok);		

		inst.setProperty("OS", "Android,") ; // ok
		System.out.println("\nOS value is : " + inst.getProperty("OS"));
		ok = inst.match("(OS=Android)")  ;
		System.out.println("Matching: (OS=Android): " + ok);		
		ok = inst.match("(OS >=Android)")  ;
		System.out.println("Matching: (OS >=Android): " + ok);		
		System.out.println("\n");


		inst.setProperty("OS", "vxxx") ; // 
		assertNotEquals(inst.getProperty("OS"), "vxxx");
		assertTrue(impl.getProperty("OS") == null);

		inst.setProperty("s1c", "s1c-value") ; // ok
		assertEquals(inst.getProperty("s1c"), "s1c-value");

		inst.setProperty("location", "kitchen");  // redefine
		assertEquals(impl.getProperty("location"), "living");
		assertEquals(inst.getProperty("location"), "living");


		//Instance has its own defs, and can define spec definiiton if not set in implem
		inst.setProperty("S1toS2Final-Bool", "6") ;
		assertNotEquals(inst.getProperty("S1toS2Final-Bool"), "6");

		inst.setProperty("S1toS2Final-Bool", "false") ;
		assertEquals(inst.getProperty("S1toS2Final-Bool"), "false");

		inst.setProperty("S1toS2Final-location", "xxx") ;
		assertTrue(inst.getProperty("S1toS2Final-location") == null);

		inst.setProperty("S1toS2Final-location", "FinalLiving, FinalKitchen") ;
		assertEquals(inst.getProperty("S1toS2Final-location"), "FinalLiving, FinalKitchen");
		System.out.println("=========== start testing setting attributes");


		System.out.println("=========== start test Remove Attributes");
		inst.removeProperty ("name") ;
		assertTrue (inst.getProperty("name") != null) ;

		inst.removeProperty ("spec-name") ;
		assertTrue (inst.getProperty("spec-name") != null) ;

		inst.removeProperty ("inst-name") ;
		assertTrue (inst.getProperty("inst-name") != null) ;

		inst.removeProperty ("testEnumere") ;
		assertTrue (inst.getProperty("testEnumere") != null) ;

		inst.removeProperty ("OS") ;
		assertTrue (inst.getProperty("OS") == null) ;

		inst.removeProperty ("xxx") ;
		assertTrue (inst.getProperty("xxx") == null) ;

		// implem
		impl.removeProperty ("name") ;
		assertTrue (impl.getProperty("name") != null) ;
		impl.removeProperty ("spec-name") ;
		assertTrue (impl.getProperty("spec-name") != null) ;
		impl.removeProperty ("OS") ;
		assertTrue (impl.getProperty("OS") == null) ;
		impl.removeProperty ("xxx") ;
		assertTrue (impl.getProperty("xxx") == null) ;

		impl.removeProperty ("S1-Attr") ;
		assertTrue (impl.getProperty("S1-Attr") != null) ;

		impl.removeProperty ("testenumere") ;
		assertTrue (impl.getProperty("testenumere") == null) ;
		assertTrue (inst.getProperty("testenumere") == null) ;

		//spec 
		spec.removeProperty ("name") ;
		assertTrue (spec.getProperty("name") != null) ;
		spec.removeProperty ("spec-name") ;
		assertTrue (spec.getProperty("spec-name") != null) ;
		spec.removeProperty ("OS") ;
		assertTrue (spec.getProperty("OS") == null) ;
		spec.removeProperty ("xxx") ;
		assertTrue (spec.getProperty("xxx") == null) ;
		spec.removeProperty ("enumeration") ;
		assertTrue (spec.getProperty("enumeration") == null) ;

		spec.removeProperty ("S1-Attr") ;
		assertTrue (spec.getProperty("S1-Attr") == null) ;
		assertTrue (impl.getProperty("S1-Attr") == null) ;
		assertTrue (inst.getProperty("S1-Attr") == null) ;

		System.out.println("=========== passed test Remove Attributes");


		System.out.println("=========== passed testSettingAttributes\n\n");

	}

	public void testImplemWithoutSpec () {

		System.out.println("=========== start test Implem without spec (dummy spec)");

		//	<implementation name="S1Main"
		//		classname="fr.imag.adele.apam.test.s1Impl.S1Main" shared="false" singleton="true">
		//		<property name="S1Main-Attr" value="whatever"/>
		//		<property name="testAttr" value="false"/>
		//		<property name="shared" value="true"/>

		Implementation impl= CST.apamResolver.findImplByName(null,"S1Main");
		//		assertEquals(impl.getProperty("S1Main-Attr"), "whatever");
		//		assertEquals(impl.getProperty("testAttr"), "false");
		assertEquals(impl.getProperty("shared"), "false");
		assertEquals(impl.getProperty("singleton"), "true");
		System.out.println("=========== passed test Implem without spec (dummy spec)");
	}


	public void otherTests () {

		Map<String, String> props = new HashMap<String, String>();
		Instance test00_instance0 = null ;
		CompositeType appli3 ; 
		// Calling that application from an external program (this instance).
		S1 s11 = (S1) test00_instance0.getServiceObject();
		s11.callS1("createAppli-1");

		// setting visibilities
		// composite a3 should not be shared
		//		test00_instance0.setProperty(CST.SHARED, CST.V_FALSE);

		System.out.println("\n\n===================================== Testing promotions\n"
				+ " creating composite on S1 containing an S2 composite \n");

		props.clear();
		//		props.put(CST.A_LOCALIMPLEM, CST.V_TRUE);
		//		props.put(CST.A_LOCALINSTANCE, CST.V_TRUE);
		//		props.put(CST.A_BORROWIMPLEM, CST.V_FALSE);
		//		props.put(CST.A_BORROWINSTANCE, CST.V_FALSE);

		CompositeType mainCompo = apam.createCompositeType("Test00", "TestS1Promotions", null, "S1Main", null /* models */,
				props);

		System.err.println("TestS1Promotions inside Test00 and black box");
		mainCompo.createInstance((Composite) test00_instance0, props);

		System.out.println("\n\n\n========================== deuxieme exec ===");
		//        test00_instance0 = appli3.createInstance(null /* composite */, null/* properties */);

		// Calling that application from an external program (this instance).
		s11 = (S1) test00_instance0.getServiceObject();
		s11.callS1("createAppli-2");


		System.out.println("\n\n\n== troisieme exec ===");
		//        test00_instance0 = appli3.createInstance(null /* composite */, null/* properties */);
		// Calling that application from an external program (this instance).
		s11 = (S1) test00_instance0.getServiceObject();
		s11.callS1("createAppli-3");

	}

	public void run() {

		System.out.println("Starting new mainApam " );

//		testFindImplByName () ;
		//testCompoURL () ;
//		testCreateCompoRootS1toS2Final () ;
//		testCreateCompoBySpec () ;
		testInitialAttributes () ;
		testSettingAttributes () ;
//		testImplemWithoutSpec () ;
	}



	public void apamInit(Instance apamInstance) {
		new Thread(this, "APAM test").start();
	}

	public void apamRemove() {

	}

}
