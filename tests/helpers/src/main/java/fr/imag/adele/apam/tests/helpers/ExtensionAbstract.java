package fr.imag.adele.apam.tests.helpers;

import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.BundleContext;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Instance;


public abstract class ExtensionAbstract {

	//Based on the current running, no test should take longer than 2 minute (some tests are freezing)
	@Rule
    public TestRule  globalTimeout= new Timeout(120000);
	
    @Inject
    public BundleContext context;

    public ApAMHelper   apam;



    protected void auxListInstances(String prefix) {
        System.out.println(String.format("%s------------ Instances -------------", prefix));
        for (Instance i : CST.componentBroker.getInsts()) {

            System.out.println(String.format("%sInstance name %s ( oid: %s ) ", prefix, i.getName(), i
                    .getServiceObject()));

        }
        System.out.println(String.format("%s------------ /Instances -------------", prefix));
    }

    protected void auxListProperties(String prefix, Component component) {
        System.out.println(String.format("%s------------ Properties -------------", prefix));
        for (String key : component.getAllProperties().keySet()) {
            System.out.println(key + "=" + component.getAllProperties().get(key.toString()));
        }
        System.out.println(String.format("%s------------ /Properties -------------", prefix));
    }

    @Before
    public void setUp() {
        apam = new ApAMHelper(context);
        apam.waitForIt(1000);
    }

    @Configuration
    public Option[] apamConfig() {
        return options(
                systemProperty("org.osgi.service.http.port").value("8080"),
                cleanCaches(),
                systemProperty("logback.configurationFile").value(
                        "file:" + PathUtils.getBaseDir() + "/log/logback.xml"),
                 systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("NONE"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").version("1.8.0"),
                mavenBundle().groupId("org.ow2.chameleon.testing").artifactId("osgi-helpers").version("0.2.0"),
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.2.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.bundlerepository").version("1.6.6"),
                mavenBundle().groupId("org.ops4j.pax.url").artifactId("pax-url-mvn").version("1.3.5"),
                mavenBundle().groupId("fr.imag.adele.apam").artifactId("apam-bundle").version("0.0.1-SNAPSHOT"),
                mavenBundle().groupId("fr.imag.adele.apam").artifactId("obrman").version("0.0.1-SNAPSHOT"),
                mavenBundle("org.slf4j", "slf4j-api").version("1.6.6"),
                mavenBundle("ch.qos.logback", "logback-core").version("1.0.7"),
                mavenBundle("ch.qos.logback", "logback-classic").version("1.0.7"),
                junitBundles(),
//                mavenBundle("fr.imag.adele.apam.tests.services", "apam-pax-samples-iface").versionAsInProject(),//version("0.0.1-SNAPSHOT"),
//                mavenBundle("fr.imag.adele.apam.tests.services", "apam-pax-samples-impl-s1").versionAsInProject(),
//                mavenBundle("fr.imag.adele.apam.tests.services", "apam-pax-samples-impl-s2").versionAsInProject(),                
                mavenBundle("fr.imag.adele.apam.tests", "apam-helpers").version("0.0.1-SNAPSHOT"),   
                when(Boolean.getBoolean("isDebugEnabled")).useOptions(
                        vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"), systemTimeout(0))
        );
    }

    @After
    public void tearDown() {
        apam.dispose();
    }


}
