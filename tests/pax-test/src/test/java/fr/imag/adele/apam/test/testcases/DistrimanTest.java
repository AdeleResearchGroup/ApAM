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
import fr.imag.adele.apam.pax.test.impl.p1.P1Impl;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

@RunWith(JUnit4TestRunner.class)
public class DistrimanTest extends ExtensionAbstract {

	@Override
	public List<Option> config() {
		List<Option> options=super.config();

		//options.add(0,packApamDistriMan());
		options.add(1,mavenBundle("fr.imag.adele.apam.tests.services","apam-pax-distriman-iface").versionAsInProject());
		options.add(1,mavenBundle("fr.imag.adele.apam.tests.services","apam-pax-distriman-P1").versionAsInProject());
		options.add(1,mavenBundle("fr.imag.adele.apam.tests.services","apam-pax-distriman-P2").versionAsInProject());
		
		return options;
		
	}
	
	@Test
	public void mes(){
		
		
		//Implementation impl=(Implementation)broker.getComponent("P1");
		
		//apam.waitForIt(6000);
		
		auxListInstances("----------");
		
//		Assert.assertTrue(impl!=null);
//		
//		Instance inst=impl.createInstance(null, null);
//		
//		Assert.assertTrue(inst!=null);
		
		//P1Impl p1=(P1Impl)CST.componentBroker.getInstService(inst).getServiceObject();
		
		//System.out.println("valueeee:"+p1.getP2());
		
	}
	
	
}
