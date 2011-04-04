package fr.imag.adele.appam.test.create;

import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.apamAPI.Application;

public class CreateAppliTest {
    // iPOJO injected
    Apam apam;

    @SuppressWarnings("unused")
    private void start() {

        Application appli = apam.createAppli("monAppli", null, "DependencyTest", null, null, null);
        apam.dumpApam();
        appli.execute(null);
        apam.dumpApam();

    }

}
