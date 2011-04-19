package fr.imag.adele.appam.test.create;

import java.io.File;
import java.net.MalformedURLException;

import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.test.s1.S1;

public class CreateAppliTest implements Runnable {
    // iPOJO injected
    Apam apam;
    S1   s1;

    /**
     * The test is performed in its own thread triggered when the component is activated
     */
    public void run() {
        try {
            Application appli = apam.createAppliDeployImpl("monAppliADeployer", null, "DependencyTest", new File(
                    "F:/APAM/fr.imag.adele.appam.test.dependency/target/test.dependency-1.0.0.jar").toURI().toURL(),
                    "bundle", null, null);
            // apam.dumpApam();
            appli.execute(null);
            apam.dumpApam();

            appli = apam.createAppli("TestS1", null, "S1Impl", null, null, null);
            // apam.dumpApam();
            appli.execute(null);
            apam.dumpApam();

            s1.callS1("premier appel depuis createAppli");

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    @SuppressWarnings("unused")
    private void start() {
        new Thread(this, "APAM test").start();
    }

}
