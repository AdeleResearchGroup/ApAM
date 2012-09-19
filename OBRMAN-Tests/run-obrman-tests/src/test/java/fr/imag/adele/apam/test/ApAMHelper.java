package fr.imag.adele.apam.test;

import static junit.framework.Assert.assertNotNull;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.ManagerModel;

public class ApAMHelper {

    private final BundleContext context;

    protected Apam              apam;

    private final OSGiHelper    osgi;

    public ApAMHelper(BundleContext pContext, OSGiHelper osgi) {
        context = pContext;
        this.osgi = osgi;
    }

    public CompositeType runApplication(String name, String main) {
        waitForIt(100);

        URL obrModelAppUrl = context.getBundle().getResource(name + ".OBRMAN.cfg");

        System.out.println(name + " >>> " + obrModelAppUrl);

        ManagerModel model = new ManagerModel("OBRMAN", obrModelAppUrl);

        Set<ManagerModel> models = new HashSet<ManagerModel>();

        models.add(model);

        apam = (Apam) osgi.getServiceObject(Apam.class.getName(), null);

        assertNotNull(apam);

        CompositeType app = apam.createCompositeType(null, name, main, models, null);

        return app;
    }

    public static void waitForIt(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            assert false;
        }
    }

}
