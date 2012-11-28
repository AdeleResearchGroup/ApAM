package fr.imag.adele.apam.test.testcases;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.ops4j.pax.exam.Option;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.pax.test.impl.S3GroupAImpl;
import fr.imag.adele.apam.tests.helpers.ExtensionAbstract;

public class DynamanDependentTest extends ExtensionAbstract {

	@Override
	public List<Option> config() {
		// TODO Auto-generated method stub
		List<Option> defaultOptions=super.config();
		defaultOptions.add(mavenBundle("fr.imag.adele.apam", "dynaman").version(
				"0.0.1-SNAPSHOT"));
		
		return defaultOptions;

	}

	@Test
	public void CompositeContentMngtDependencyFailWait() {

		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-fail-wait");

		Composite composite_a = (Composite) cta.createInstance(null, null);

		Instance instanceApp1 = composite_a.getMainInst();

		S3GroupAImpl ga1 = (S3GroupAImpl) instanceApp1.getServiceObject();

		ThreadWrapper wrapper=new ThreadWrapper(ga1);
		wrapper.setDaemon(true);
		wrapper.start();
		
		apam.waitForIt(3000);
		
		String message="In case of composite dependency been maked as fail='wait', the thread should be blocked until the dependency is satisfied. During this test the thread did not block.";
		
		Assert.assertTrue(message,wrapper.isAlive());
	}
	
	//Require by the test CompositeContentMngtDependencyFailWait
	class ThreadWrapper extends Thread {

		final S3GroupAImpl group;
		
		public ThreadWrapper(S3GroupAImpl group){
			this.group=group;
		}
		
		@Override
		public void run() {
			System.out.println("Element injected:"+group.getElement());
		}
		
	}	
	
	@Test
	public void CompositeContentMngtOwnSpecification() {

		CompositeType cta = (CompositeType) CST.apamResolver.findImplByName(
				null, "composite-a-own-specification");

		Composite composite_a = (Composite) cta.createInstance(null, null);

		Implementation dependencyImpl = CST.apamResolver.findImplByName(null,
				"group-a");

		Instance dependencyInstance = dependencyImpl.createInstance(null, null);

		S3GroupAImpl group_a = (S3GroupAImpl) dependencyInstance
				.getServiceObject();

		Instance inst = CST.componentBroker
				.getInstService(group_a.getElement());

		String message="When a composite declares to own a specification, that means every instance of that specification should be owned by that composite. This test failed, the actual owner composite of that component and the one that declares to be the owner are different";
		
		Assert.assertTrue(message,inst.getComposite() == composite_a);

	}
	
	
}
