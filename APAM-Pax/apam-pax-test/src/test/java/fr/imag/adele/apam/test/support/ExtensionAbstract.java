package fr.imag.adele.apam.test.support;

import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import org.apache.felix.ipojo.util.Logger;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Instance;

public abstract class ExtensionAbstract {
	
	@Inject
	public BundleContext context;

	OSGiHelper OSGihelper;

	Logger logger;

	/**
	 * This method allows to verify the state of the bundle to make sure that we can perform tasks on it
	 * @param time
	 */
	protected void waitForIt(int time) {
		
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			System.err.println("waitForIt failed.");
		}

		while (
				//context.getBundle().getState() != Bundle.STARTING && 
				context.getBundle().getState() != Bundle.ACTIVE //&&
				//context.getBundle().getState() != Bundle.STOPPING
				) {
			try {
				Thread.sleep(time);
			} catch (InterruptedException e) {
				System.err.println("waitForIt failed.");
			}
		}
		
	}
	
	protected void auxListInstances(String prefix) {
		System.out.println(String.format("%s------------ Instances -------------",prefix));
		for (Instance i : CST.componentBroker.getInsts()) {

			System.out.println(String.format("%sInstance name %s ( oid: %s ) ",prefix,i.getName(),i.getServiceObject()));

		}
		System.out.println(String.format("%s------------ /Instances -------------",prefix));
	}
	
	protected void auxListProperties(String prefix,Component component){
		System.out.println(String.format("%s------------ Properties -------------",prefix));
		for(String key:component.getAllProperties().keySet()){
			System.out.println(key+"="+component.getAllProperties().get(key.toString()));
		}
		System.out.println(String.format("%s------------ /Properties -------------",prefix));
	}
	
	@Before
	public void setUp() {
		
		OSGihelper = new OSGiHelper(context);

//		context.addBundleListener(new SynchronousBundleListener() {
//			
//			@Override
//			public void bundleChanged(BundleEvent arg0) {
//				System.out.println("type-active:"+(arg0.getBundle().getState()==Bundle.ACTIVE));
//				System.out.println("type-installed:"+(arg0.getBundle().getState()==Bundle.INSTALLED));
//				System.out.println("type-resolved:"+(arg0.getBundle().getState()==Bundle.RESOLVED));
//				System.out.println("type-starting:"+(arg0.getBundle().getState()==Bundle.STARTING));
//				System.out.println("type-stoping:"+(arg0.getBundle().getState()==Bundle.STOPPING));
//				System.out.println("type-uninstalled:"+(arg0.getBundle().getState()==Bundle.UNINSTALLED));
//			
//				if(arg0.getBundle().getState()==Bundle.STOPPING)
//					try {
//						Thread.sleep(100);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//			}
//		}); 
		
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
						.version("1.2.17")
		));

		Option[] r = OptionUtils.combine(platform, bundles);

		Option[] debug = options(vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"));


		
		// r = OptionUtils.combine(r, debug);

		// Option[] log =
		// options(vmOption("-Dlog4j.file=./am.log4j.properties"));
		// r = OptionUtils.combine(r, log);
		return r;
	}

	@After
	public void tearDown() {
		OSGihelper.dispose();
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			System.err.println("waitForIt failed.");
		}
		
	}
	
}
