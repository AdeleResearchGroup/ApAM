package fr.imag.adele.apam.test;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.test.s3.S3_1;
import fr.imag.adele.apam.test.s3.S3_2;
import fr.imag.adele.apam.test.s4.S4;

//import fr.imag.adele.apam.test.s4.S4;

@RunWith(JUnit4TestRunner.class)
public class PaxTest {

	// S4 s4_1;
	// S4 s4_2;
	// S4 s4_3;
	// Set<S3_1> s3_1set;
	// S3_2[] s3_2array;
	// S3_1 s3;
	// S3_2 s3bis = null;;
	//
	// Set<S3_1> s3_1;
	// S3_2[] s3_2;
	//
	// List<S3_1> s3s2;
	// Set<S3_1> s3s;
	//
	// Instance myInst;
	// String name;

	@Inject
	protected BundleContext context;
	OSGiHelper help;

	private static void waitForIt(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			assert false;
		}
	}

	/**
	 * Done some initializations.
	 */
	@Before
	public void setUp() {

		help = new OSGiHelper(context);

		waitForIt(1000);
	}

	@Configuration
	public static Option[] apamConfig() {

		Option[] platform = options(felix(),
				systemProperty("org.osgi.service.http.port").value("8080"));

		Option[] bundles = options(provision(
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.ipojo").version("1.8.0"),
				mavenBundle().groupId("org.ow2.chameleon.testing")
						.artifactId("osgi-helpers").version("0.2.0"),
				mavenBundle().groupId("org.osgi")
						.artifactId("org.osgi.compendium").version("4.2.0"),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.bundlerepository")
						.version("1.6.6"),
				mavenBundle().groupId("org.ops4j.pax.url")
						.artifactId("pax-url-mvn").version("1.3.5"),
				mavenBundle().groupId("fr.imag.adele.apam")
						.artifactId("APAMBundle").version("0.0.1-SNAPSHOT"),
				mavenBundle().groupId("fr.imag.adele.apam")
						.artifactId("OBRMAN").version("0.0.1-SNAPSHOT"),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-api")
						.version("1.6.6"),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-log4j12")
						.version("1.6.6"),
				mavenBundle().groupId("log4j").artifactId("log4j")
						.version("1.2.17"),
				mavenBundle().groupId("fr.imag.adele.apam").artifactId("S3")
						.version("0.0.1-SNAPSHOT"),
				mavenBundle().groupId("fr.imag.adele.apam").artifactId("S4")
						.version("0.0.1-SNAPSHOT")

		));

		Option[] r = OptionUtils.combine(platform, bundles);

		Option[] debug = options(vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"));
		
		//r = OptionUtils.combine(r, debug);

		// Option[] log =
		// options(vmOption("-Dlog4j.file=./am.log4j.properties"));
		// r = OptionUtils.combine(r, log);
		return r;
	}

	@After
	public void tearDown() {
		help.dispose();
	}

	@Test
	public void InstanceCreationWithoutInjection(){
			    
		Implementation s3Impl = CST.apamResolver.findImplByName(null, "S3Impl");
		
		Instance inst=s3Impl.createInstance(null, null);
		
		Apam apam = (Apam) help.getServiceObject(Apam.class.getName(), null);
		
		boolean found=false;
		
		for(Instance i:CST.componentBroker.getInsts()){

			
			if(inst==i) {
				found=true;
				break;
			}
			
		}
		
		Assert.assertTrue(found);
		
	}
	
	
	public void testRootModel() {

//		S3_1 s3;
//		S3_2 s3bis=null;
//		
//		S4 s4_1;
//		S4 s4_2;
//		S4 s4_3;
//		Set<S3_1> s3_1set;
//		S3_2[] s3_2array;
//
//		Set<S3_1> s3_1;
//		S3_2[] s3_2;
//
//		List<S3_1> s3s2;
//		Set<S3_1> s3s;
//
//		Instance myInst;
//		String name;
//
//		Apam apam = (Apam) help.getServiceObject(Apam.class.getName(), null);
//
//		Map<String, Instance> S3Insts = new HashMap<String, Instance>();

		// Implementation s3Impl = CST.apamResolver.findImplByName(null,
		// "apam.test.dependency.S3Impl");

//		Implementation s3Impl = CST.apamResolver.findImplByName(null, "S3Impl");

//		Instance s3Inst=s3Impl.createInstance(null, null);
		
		
//		Assert.assertTrue(s3Inst != null);
		
//		Assert.assertTrue(s3bis != null);
		// assertTrue(s3 != null);

		// assertEquals (CST.componentBroker.getInstService(s3).getName(),
		// s3.getName()) ;
		// assertEquals (CST.componentBroker.getInstService(s3bis).getName(),
		// s3bis.getName()) ;
		//
		// //Checking constraints
		// s3Inst = CST.componentBroker.getInstService(s3bis) ;
		//
		// assertTrue (s3Inst.match("(OS*>Android)" )) ;
		// assertTrue (s3Inst.match("(&(location=living)(MyBool=true))")) ;
		//
		// //multiple dependencies
		// assertTrue (s3_1set.size() != 0) ;
		// assertTrue (s3_1set.containsAll (Arrays.asList(s3_2array))) ;
		//
		// //Checking Dynamic addition to multiple dependency
		// System.out.println("/nChecking Dynamic addition to multiple dependency"
		// ) ;
		// s3Inst = s3Impl.createInstance(null, null);
		// assertTrue (s3_1set.contains(s3Inst.getServiceObject())) ;
		// assertTrue (s3_1set.containsAll (Arrays.asList(s3_2array))) ;
		//
		// //Checking Dynamic Wire deletion to multiple dependency
		// System.out.println("Checking Dynamic Wire deletion to multiple dependency"
		// ) ;
		// Wire w = (Wire)myInst.getWires().toArray()[0] ;
		// Instance rmInst = w.getDestination() ;
		// myInst.removeWire(w) ;
		// //S3Insts.remove(s3Inst.getName()) ;
		// assertTrue (!s3_1set.contains(rmInst.getServiceObject()));
		// assertTrue (s3_1set.containsAll (Arrays.asList(s3_2array))) ;

		// test delete instances
		// Instance inst=CST.componentBroker.getInstService(s3bis) ;
		// Wire wiresInst = (Wire)inst.getWires();
		// for(Wire se:inst.getWires()){
		// inst.removeWire(se);
		// }

		// contraintes multiple

		// contraintes implementations
		// contraintes instances

		// heritage de contraintes
		// contraintes générique

		// preferences

		// instantiable

		// shared

		// singleton

		// resolution interface
		// resolution message
		// resolution Spec
		// resolution Implem
		// resolution instance

		// fail
		// exception
		// override exception
		// override hidden
		// wait

	}

}
