package fr.imag.adele.apam.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import fr.imag.adele.apam.test.testcases.ConstraintTest;
import fr.imag.adele.apam.test.testcases.InjectionInstantiationTest;
import fr.imag.adele.apam.test.testcases.OBRMANTest;
import fr.imag.adele.apam.test.testcases.PropertyTest;

@RunWith(Suite.class)
@SuiteClasses(value = { InjectionInstantiationTest.class, PropertyTest.class, ConstraintTest.class, OBRMANTest.class})
public class AllSuites {

	
	
}
