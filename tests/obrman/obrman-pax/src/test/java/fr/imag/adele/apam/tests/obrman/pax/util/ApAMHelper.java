package fr.imag.adele.apam.tests.obrman.pax.util;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.obrMan.OBRManCommand;

public class ApAMHelper {

    private final BundleContext context;

    private final OSGiHelper    osgi;
    
    private final IPOJOHelper    ipojo;

    public ApAMHelper(BundleContext pContext) {
        context = pContext;
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);
        
    }

    public void dispose(){
        osgi.dispose();
        ipojo.dispose();
    }
    
    public void setObrManInitialConfig(String modelPrefix, String[] repos, int expectedSize ) throws IOException {
        URL obrModelAppUrl = context.getBundle().getResource(modelPrefix + ".OBRMAN.cfg");

        System.out.println(modelPrefix + " >>> " + obrModelAppUrl);

        OBRManCommand obrman = getAService(OBRManCommand.class);
        
        obrman.setInitialConfig(obrModelAppUrl);
            
        Set<String> rootRepos = getCompositeRepos(CST.ROOT_COMPOSITE_TYPE);
        for (String repo : repos) {
            assertTrue(rootRepos.contains(repo));
        }
        
        assertEquals(expectedSize, rootRepos.size());

    }

    public CompositeType createCompositeType(String name, String main, String mainSpec) {
        waitForIt(100);

        URL obrModelAppUrl = context.getBundle().getResource(name + ".OBRMAN.cfg");

        System.out.println(name + " >>> " + obrModelAppUrl);

        ManagerModel model = new ManagerModel("OBRMAN", obrModelAppUrl);

        Set<ManagerModel> models = new HashSet<ManagerModel>();

        models.add(model);

        Apam apam = getAService(Apam.class);

        CompositeType app = apam.createCompositeType(null, name, mainSpec, main, models, null);

        assertNotNull(app);

        assertNotNull(app.getMainImpl().getApformImpl());

        return app;
    }

    public <T> T createInstance(CompositeType CompoType, Class<T> class1) {

        Composite instanceApp = (Composite) CompoType.createInstance(null, null);

        assertNotNull(instanceApp);

        T appSpec = class1.cast(instanceApp.getServiceObject());

        return appSpec;

    }

    public static void waitForIt(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            assert false;
        }
    }

 

    public <S> S getAService(Class<S> clazz) {
        S s = clazz.cast(osgi.getServiceObject(clazz.getName(), null));
        assertNotNull(s);
        return s;
    }

    public Set<String> getCompositeRepos(String compositeName){
        OBRManCommand obrman = getAService(OBRManCommand.class);
        return obrman.getCompositeRepositories(compositeName);
    }
    
    public OSGiHelper getOSGiHelper(){
        return osgi;
    }
    
    public IPOJOHelper getIpojoHelper(){
        return ipojo;
    }
}
