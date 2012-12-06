package fr.imag.adele.apam.tests.helpers;

import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.ComponentBroker;

public abstract class ExtensionAbstract extends TestUtils {

    // Based on the current running, no test should take longer than 2 minute
    @Rule
    public TestRule globalTimeout = new ApamTimeoutRule(isDebugModeOn() ? null
            : 120000);

    @Rule
    public TestName name = new TestName();

    @Inject
    public BundleContext context;

    private Logger logger = LoggerFactory.getLogger(getClass());

    public ApAMHelper apam;

    protected ComponentBroker broker;

    public List<Option> config() {

        List<Option> config = new ArrayList<Option>();
        config.add(systemProperty("org.osgi.service.http.port").value("8080"));
        config.add(cleanCaches());
        config.add(systemProperty("logback.configurationFile").value(
                "file:" + PathUtils.getBaseDir() + "/log/logback.xml"));
        config.add(systemProperty(
                "org.ops4j.pax.logging.DefaultServiceLog.level").value("NONE"));
        config.add(mavenBundle().groupId("org.apache.felix")
                .artifactId("org.apache.felix.ipojo").versionAsInProject());
        config.add(mavenBundle().groupId("org.ow2.chameleon.testing")
                .artifactId("osgi-helpers").versionAsInProject());
        config.add(mavenBundle().groupId("org.osgi")
                .artifactId("org.osgi.compendium").version("4.2.0"));
        config.add(mavenBundle().groupId("org.apache.felix")
                .artifactId("org.apache.felix.bundlerepository").versionAsInProject());
        config.add(mavenBundle().groupId("org.ops4j.pax.url")
                .artifactId("pax-url-mvn").versionAsInProject());
        config.add(mavenBundle().groupId("fr.imag.adele.apam")
                .artifactId("apam-bundle").versionAsInProject());
        config.add(mavenBundle().groupId("fr.imag.adele.apam")
                .artifactId("obrman").versionAsInProject());
        config.add(mavenBundle("org.slf4j", "slf4j-api").versionAsInProject());

        config.add(mavenBundle("ch.qos.logback", "logback-core").versionAsInProject());
        config.add(mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject());
        config.add(junitBundles());
        config.add(mavenBundle("fr.imag.adele.apam.tests", "apam-helpers")
                .versionAsInProject());

        config.add(vmOption("-ea"));
        config.add(when(isDebugModeOn())
                .useOptions(
                		vmOption(String
                                .format("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%d",
                                        Constants.CONST_DEBUG_PORT)),
                        systemTimeout(0)));

        return config;
    }

    @Configuration
    public Option[] apamConfig() {

        Option conf[] = config().toArray(new Option[0]);

        return conf;
    }

    @Before
    public void setUp() {
        apam = new ApAMHelper(context);
        broker = CST.componentBroker;
        logger.info("[Run Test : " + name.getMethodName() + "]");
        apam.waitForIt(1000);
    }

    @After
    public void tearDown() {
        apam.dispose();
    }

    private static boolean isDebugModeOn() {
        RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = RuntimemxBean.getInputArguments();

        boolean debugModeOn = false;

        for (String string : arguments) {
            debugModeOn = string.indexOf("jdwp") != -1;
            if (debugModeOn)
                break;
        }

        return debugModeOn;
    }

}
