package fr.imag.adele.apam.test.testcases;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.InvalidSyntaxException;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.pax.test.implS1.S1Impl;
import fr.imag.adele.apam.tests.helpers.Constants;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(JUnit4TestRunner.class)
public class PropertyTest extends ExtensionAbstract {

	/**
	 * Ensures that inherited properties cannot be changed and inherited definitions can change
	 */
	@Test
	public void PropertyInheritedCannotBeChanged_tc001(){
		
				
		Implementation samsungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");
		final Instance samsungInst = samsungImpl.createInstance(null, null);
		
		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.pax.test.impl.S1Impl");
		
		Instance s1Inst = s1Impl.createInstance(null, null);

		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();
	
		//this should be updated correctly
		samsungInst.setProperty("currentVoltage", "999");
		//this should stay with the old value
		samsungInst.setProperty("made", "deutschland");
		
		//this property should be updated since its not inherited
		Assert.assertTrue("Non-inherited properties shall be updateable",samsungInst.getProperty("currentVoltage").equals("999")) ;

		Assert.assertTrue("Inherited property shall not be changed",samsungInst.getProperty("made").equals("china")) ;
		
	}
	
	/**
	 * Ensures that initial properties are configured in the instance properly
	 */
	@Test
	public void PropertyConfiguredWithInitialParameter_tc002(){
		
				
		Implementation samsungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");
		
		Map<String,String> initialProperties=new HashMap<String, String>(){{
			put("property-01", "configured");
			put("property-02", "configured");
			put("property-03", "configured");
			put("property-04", "configured");
			put("property-05", "configured");
		}};
		
		Instance samsungInst = samsungImpl.createInstance(null, initialProperties);
		
		Assert.assertNotNull("Instance could not be create through the API", samsungInst);
		
		//all the initial properties should be inside of the instance
		for(String key:initialProperties.keySet()){
			
			Assert.assertNotNull("Instance did not receive the initial property", samsungInst.getAllProperties().containsKey(key));
			
			Assert.assertNotNull("Instance did not receive the initial property",samsungInst.getAllProperties().get(key));
			
			Assert.assertTrue(samsungInst.getAllProperties().get(key).equals(initialProperties.get(key)));
		}
	}

	/**
	 * Ensures that initial properties are configured in the instance properly
	 */
	@Test
	public void PropertyConfiguredWithSetProperty_tc003(){
		
				
		Implementation samsungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");
		
		Map<String,String> initialProperties=new HashMap<String, String>(){{
			put("property-01", "configured-01");
			put("property-02", "configured-02");
			put("property-03", "configured-03");
			put("property-04", "configured-04");
			put("property-05", "configured-05");
			
		}};
		
		Instance samsungInst = samsungImpl.createInstance(null, null);
		
		samsungInst.setProperty("property-01", "configured-01");
		samsungInst.setProperty("property-02", "configured-02");
		samsungInst.setProperty("property-03", "configured-03");
		samsungInst.setProperty("property-04", "configured-04");
		samsungInst.setProperty("property-05", "configured-05");
		
		final String message="Instance did not receive the property defined by setProperty method call";
		
		for(String key:initialProperties.keySet()){
			
			System.out.println(key+":"+samsungInst.getAllProperties().get(key));
			
			Assert.assertTrue(message,samsungInst.getAllProperties().containsKey(key));
			Assert.assertTrue(message,samsungInst.getProperty(key).equals(initialProperties.get(key)));
		}
	}
	
	@Test
	public void PropertyDefinitionInternalAndNotInternalAreAPIVisible_tc004(){
		
		
		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.pax.test.impl.S1Impl");
		
		Instance s1Inst = s1Impl.createInstance(null, null);
		
		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();
				
		s1Inst.setProperty("stateInternal", "default");
		s1Inst.setProperty("stateNotInternal", "default");

		for(String key:s1Inst.getAllProperties().keySet()){
			System.out.println(key+"="+s1Inst.getAllProperties().get(key.toString()));
		}
		
		Assert.assertTrue("Internal property not visible through API", s1Inst.getAllProperties().get("stateInternal")!=null);
		Assert.assertTrue("Non-Internal property not visible through API", s1Inst.getAllProperties().get("stateNotInternal")!=null);
		
		Assert.assertTrue("Internal property not visible through API with the right value", s1Inst.getAllProperties().get("stateInternal").equals("default"));
		Assert.assertTrue("Non-Internal property not visible through API with the right value", s1Inst.getAllProperties().get("stateNotInternal").equals("default"));
		
		s1Inst.setProperty("stateInternal", "changed");
		s1Inst.setProperty("stateNotInternal", "changed");
		
		Assert.assertTrue("Internal property shall not be changeble through API", s1Inst.getAllProperties().get("stateInternal").equals("default"));
		Assert.assertTrue("Non-Internal property shall be changeble through API", s1Inst.getAllProperties().get("stateNotInternal").equals("changed"));
		
		s1.setStateInternal("changed2");
		s1.setStateNotInternal("changed2");
		
		Assert.assertTrue("Internal property shall be changeble through the application", s1Inst.getAllProperties().get("stateInternal").equals("changed2"));
		Assert.assertTrue("Non-Internal property shall be changeble through the application", s1Inst.getAllProperties().get("stateNotInternal").equals("changed2"));
		
	}

	@Test
	public void PropertyDefinitionIsVisibleWithValPropertySetXML_tc005(){
		
		Implementation s1Impl = CST.apamResolver.findImplByName(null,
				"fr.imag.adele.apam.pax.test.impl.S1Impl");
		
		Instance s1Inst = s1Impl.createInstance(null, null);
		
		S1Impl s1 = (S1Impl) s1Inst.getServiceObject();
		
		Assert.assertTrue("Internal property not visible through API", s1Inst.getAllProperties().get("stateInternal")!=null);
		Assert.assertTrue("Non-Internal property not visible through API", s1Inst.getAllProperties().get("stateNotInternal")!=null);
		
		Assert.assertTrue("Internal property value not visible through API", s1Inst.getAllProperties().get("stateInternal").equals("default"));
		Assert.assertTrue("Non-Internal property value not visible through API", s1Inst.getAllProperties().get("stateNotInternal").equals("default"));
		
	}
	
	@Test
	public void PropertiesDataTypeAndLDAPFilteringForIntegers_tc006()
			throws InvalidSyntaxException {

		Implementation samsungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");
		final Instance samsungInst = samsungImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "95");
					}
				});

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		auxListProperties("\t", samsungInst);

		// int

		String templateMessage = "Calling match method with filter = %s, should result in True since currentVoltage is %n";
		String message = "";
		
		message = String
				.format(templateMessage,
						"(currentVoltage>=95)",
						samsungInst.getProperty("currentVoltage"));

		Assert.assertTrue(message,samsungInst.match("(currentVoltage>=95)"));

		message = String
				.format(templateMessage,
						"(currentVoltage<=95)",
						samsungInst.getProperty("currentVoltage"));
		
		Assert.assertTrue(message,samsungInst.match("(currentVoltage<=95)"));

		message = String
				.format(templateMessage,
						"(currentVoltage<=101)",
						samsungInst.getProperty("currentVoltage"));
		
		Assert.assertTrue(message,samsungInst.match("(currentVoltage<=101)"));

		message = String
				.format(templateMessage,
						"(currentVoltage<=96)",
						samsungInst.getProperty("currentVoltage"));
		
		Assert.assertTrue(message,samsungInst.match("(currentVoltage<=96)"));
		
		message = String
				.format(templateMessage,
						"(currentVoltage>=94)",
						samsungInst.getProperty("currentVoltage"));

		Assert.assertTrue(message,samsungInst.match("(currentVoltage>=94)"));

	}

	@Test
	public void PropertiesDataTypeAndLDAPFilteringForBoolean_tc007()
			throws InvalidSyntaxException {

		
		Implementation samsungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");
		final Instance samsungInst = samsungImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "95");
					}
				});

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		auxListProperties("\t", samsungInst);

		String message = "";
		
		message = String
				.format("Calling match method with filter = %s, should result in True since hasDisplay is %b",
						"(hasDisplay=false)",
						samsungInst.getProperty("hasDisplay"));

		Assert.assertTrue(message,samsungInst.match("(hasDisplay=false)"));

		message = String
				.format("Calling match method with filter = %s, should result in False since hasDisplay is %b",
						"(hasDisplay=true)",
						samsungInst.getProperty("hasDisplay"));

		Assert.assertFalse(message,samsungInst.match("(hasDisplay=true)"));

	}
	
	@Test
	public void PropertiesDataTypeAndLDAPFilteringForString_tc008()
			throws InvalidSyntaxException {

		
		Implementation samsungImpl = CST.apamResolver.findImplByName(null,
				"SamsungSwitch");
		final Instance samsungInst = samsungImpl.createInstance(null,
				new HashMap<String, String>() {
					{
						put("currentVoltage", "95");
					}
				});

		apam.waitForIt(Constants.CONST_WAIT_TIME);

		auxListProperties("\t", samsungInst);

		String templateMessage = "Calling match method with filter = %s, should result in True since impl-name is %s";
		String message = "";
		
		message = String
				.format(templateMessage,
						"(impl-name=Samsung*)",
						samsungInst.getProperty("impl-name"));

		Assert.assertTrue(message,samsungInst.match("(impl-name=Samsung*)"));

		message = String
				.format(templateMessage,
						"(impl-name=*amsungSwitch)",
						samsungInst.getProperty("impl-name"));

		Assert.assertTrue(message,samsungInst.match("(impl-name=*amsungSwitch)"));

		message = String
				.format(templateMessage,
						"(impl-name=*amsung*)",
						samsungInst.getProperty("impl-name"));

		Assert.assertTrue(message,samsungInst.match("(impl-name=*amsung*)"));
		
		templateMessage = "Calling match method with filter = %s, should result in False since impl-name is %s";
		
		message = String
				.format(templateMessage,
						"(impl-name=SamsunG*)",
						samsungInst.getProperty("impl-name"));

		Assert.assertFalse(message,samsungInst.match("(impl-name=SamsunG*)"));

	}
	
	@Test
	@Ignore
	public void PropertiesDataTypeListInt_tc053(){
		
		final String propertyName="setInt";
		
		Implementation implementation=CST.apamResolver.findImplByName(null, "SamsungSwitch");
		
		Map<String,Object> allproperties=implementation.getAllProperties();
		
		Object RawType=allproperties.get(propertyName);
		
		String message="Retrieving a set of (%s) from the properties, but the return do not correspond to a type that represents a set of elements(List,Set nor array).";
		
		Assert.assertTrue(String.format(message, "Int"),RawType instanceof String[] || RawType instanceof Collection); 
		
		String messageCollection="Retrieving a set of (%s) from the properties, the return correspond to a Collection type but not a Collection containing the type %s.";
		
		if(RawType instanceof Collection){
			Object sample=((Collection)RawType).iterator().next();
			Class properType=Integer.class;
			Assert.assertTrue(String.format(messageCollection,propertyName,properType),properType.isInstance(sample));
		}
		
	}
	
	@Test
	@Ignore
	public void PropertiesDataTypeListInteger_tc054(){
		
		final String propertyName="setInteger"; 
		
		Implementation implementation=CST.apamResolver.findImplByName(null, "SamsungSwitch");
		
		Map<String,Object> allproperties=implementation.getAllProperties();
		
		Object RawType=allproperties.get(propertyName);
		
		String message="Retrieving a set of (%s) from the properties, but the return do not correspond to a type that represents a set of elements(List,Set nor array).";
		
		Assert.assertTrue(String.format(message, "Integer"),RawType instanceof String[] || RawType instanceof Collection); 
		
		String messageCollection="Retrieving a set of (%s) from the properties, the return correspond to a Collection type but not a Collection containing the type %s.";
		
		if(RawType instanceof Collection){
			Object sample=((Collection)RawType).iterator().next();
			Class properType=Integer.class;
			Assert.assertTrue(String.format(messageCollection,propertyName,properType),properType.isInstance(sample));
		}
		
	}
	
	@Test
	@Ignore
	public void PropertiesDataTypeListString_tc055(){
		
		final String propertyName="setString"; 
		
		Implementation implementation=CST.apamResolver.findImplByName(null, "SamsungSwitch");
		
		Map<String,Object> allproperties=implementation.getAllProperties();
				
		Object RawType=allproperties.get(propertyName);
		
		String message="Retrieving a set of (%s) from the properties, but the return do not correspond to a type that represents a set of elements(List,Set nor array).";
		
		Assert.assertTrue(String.format(message, "String"),RawType instanceof String[] || RawType instanceof Collection); 
		
		String messageCollection="Retrieving a set of (%s) from the properties, the return correspond to a Collection type but not a Collection containing the type %s.";
		
		if(RawType instanceof Collection){
			Object sample=((Collection)RawType).iterator().next();
			Class properType=String.class;
			Assert.assertTrue(String.format(messageCollection,propertyName,properType),properType.isInstance(sample));
		}
		
	}	
	
	@Test
	@Ignore
	public void PropertiesDataTypeListBoolean_tc056(){
		
		final String propertyName="setBoolean"; 
		
		Implementation implementation=CST.apamResolver.findImplByName(null, "SamsungSwitch");
		
		Map<String,Object> allproperties=implementation.getAllProperties();
		
		Object RawType=allproperties.get(propertyName);
		
		String message="Retrieving a set of (%s) from the properties, but the return do not correspond to a type that represents a set of elements(List,Set nor array).";
		
		Assert.assertTrue(String.format(message, "Boolean"),RawType instanceof Boolean[] || RawType instanceof Collection); 
		
		String messageCollection="Retrieving a set of (%s) from the properties, the return correspond to a Collection type but not a Collection containing the type %s.";
		
		if(RawType instanceof Collection){
			Object sample=((Collection)RawType).iterator().next();
			Class properType=Boolean.class;
			Assert.assertTrue(String.format(messageCollection,propertyName,properType),properType.isInstance(sample));
		}
		
	}

	@Test
	public void PropertyFilterOSGiImplementationSuperSet_Integer_Set_Operator_Set(){
		
		Implementation implementation=CST.apamResolver.findImplByName(null, "SpecFilterSwitch");
		Instance inst=implementation.createInstance(null, null);
		
		String messageTemplate="%s [expanded expression: %s %s %s] should be %b. By definition the A superset('*>') B operator means that A must contain all B elements, although it may contain more.";
		
		String expression="(setInt *> setInt)";
		String message=String.format(messageTemplate,expression,inst.getProperty("setInt"),"*>",inst.getProperty("setInt"),true);
		
		//Test
		Assert.assertTrue(message,inst.match(expression));
		
	}
	
	@Test
	public void PropertyFilterOSGiImplementationSuperSet_Integer_Set_Operator_Set_unordered(){
		
		Implementation implementation=CST.apamResolver.findImplByName(null, "SpecFilterSwitch");
		Instance inst=implementation.createInstance(null, null);
		
		String messageTemplate="%s [expanded expression: %s %s %s] should be %b. By definition the A superset('*>') B operator means that A must contain all B elements, although it may contain more.";
		
		String expression="(setInt *> setIntUnordered)";
		String message=String.format(messageTemplate,expression,inst.getProperty("setInt"),"*>",inst.getProperty("setIntUnordered"),true);
		
		//Test
		Assert.assertTrue(message,inst.match(expression));
		
	}

	@Test
	public void PropertyFilterOSGiImplementationSuperSet_Integer_Set_Operator_Set_less(){
		
		Implementation implementation=CST.apamResolver.findImplByName(null, "SpecFilterSwitch");
		Instance inst=implementation.createInstance(null, null);
		
		String messageTemplate="%s [expanded expression: %s %s %s] should be %b. By definition the A superset('*>') B operator means that A must contain all B elements, although it may contain more.";
		
		String expression="(setInt *> setIntLessElements)";
		String message=String.format(messageTemplate,expression,inst.getProperty("setInt"),"*>",inst.getProperty("setIntLessElements"),true);
		
		//Test
		Assert.assertTrue(message,inst.match(expression));
		
	}
	
	@Test
	public void PropertyFilterOSGiImplementationSuperSet_Integer_Set_Operator_Set_more(){
		
		Implementation implementation=CST.apamResolver.findImplByName(null, "SpecFilterSwitch");
		Instance inst=implementation.createInstance(null, null);
		
		String messageTemplate="%s [expanded expression: %s %s %s] should be %b. By definition the A superset('*>') B operator means that A must contain all B elements, although it may contain more.";
		
		String expression="(setInt *> setIntMoreElements)";
		String message=String.format(messageTemplate,expression,inst.getProperty("setInt"),"*>",inst.getProperty("setIntMoreElements"),false);
		
		//Test
		Assert.assertTrue(message,!inst.match(expression));
		
	}
	
	@Test
	public void PropertyFilterOSGiImplementationSuperSet_Integer_Set_Operator_Value(){
		
		Implementation implementation=CST.apamResolver.findImplByName(null, "SpecFilterSwitch");
		Instance inst=implementation.createInstance(null, null);
		
		String messageTemplate="%s [expanded expression: %s %s %s] should be %b. By definition the A superset('*>') B operator means that A must contain all B elements, although it may contain more.";
		
		String expression="(setInt *> {12,15,254, 0})";
		String message=String.format(messageTemplate,expression,inst.getProperty("setInt"),"*>",inst.getProperty("setInt"),true);
		
		//Test
		Assert.assertTrue(message,inst.match(expression));
		
	}
	
	@Test
	public void PropertyFilterOSGiImplementationSuperSet_Integer_Set_Operator_Value_unordered(){
		
		Implementation implementation=CST.apamResolver.findImplByName(null, "SpecFilterSwitch");
		Instance inst=implementation.createInstance(null, null);
		
		String messageTemplate="%s [expanded expression: %s %s %s] should be %b. By definition the A superset('*>') B operator means that A must contain all B elements, although it may contain more.";
		
		String expression="(setInt *> {254,15,12,0})";
		String message=String.format(messageTemplate,expression,inst.getProperty("setInt"),"*>","{254,15,12,0}",true);
		
		//Test
		Assert.assertTrue(message,inst.match(expression));
		
	}
	
	@Test
	public void PropertyFilterOSGiImplementationSuperSet_Integer_Set_Operator_Value_less(){
		
		Implementation implementation=CST.apamResolver.findImplByName(null, "SpecFilterSwitch");
		Instance inst=implementation.createInstance(null, null);
		
		String messageTemplate="%s [expanded expression: %s %s %s] should be %b. By definition the A superset('*>') B operator means that A must contain all B elements, although it may contain more.";
		
		String expression="(setInt *> {12,15, 0})";
		String message=String.format(messageTemplate,expression,inst.getProperty("setInt"),"*>","{12,15, 0}",true);
		
		//Test
		Assert.assertTrue(message,inst.match(expression));
		
	}
	
	@Test
	public void PropertyFilterOSGiImplementationSuperSet_Integer_Set_Value_Set_more(){
		
		Implementation implementation=CST.apamResolver.findImplByName(null, "SpecFilterSwitch");
		Instance inst=implementation.createInstance(null, null);
		
		String messageTemplate="%s [expanded expression: %s %s %s] should be %b. By definition the A superset('*>') B operator means that A must contain all B elements, although it may contain more.";
		
		String expression="(setInt *> {12,15,254, 0,27})";
		String message=String.format(messageTemplate,expression,inst.getProperty("setInt"),"*>","{12,15,254, 0,27}",false);
		
		//Test
		Assert.assertTrue(message,!inst.match(expression));
		
	}
	
}
