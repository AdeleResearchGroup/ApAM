package fr.imag.adele.apam.tests.helpers;

import static junit.framework.Assert.assertNotNull;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;

public class ApAMHelper {

    private final BundleContext context;

    private final OSGiHelper    osgi;

    private final IPOJOHelper   ipojo;

    public ApAMHelper(BundleContext pContext) {
        context = pContext;
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);

    }

    public void dispose() {
        osgi.dispose();
        ipojo.dispose();
    }

    public <T> T createInstance(CompositeType CompoType, Class<T> class1) {

        Composite instanceApp = (Composite) CompoType.createInstance(null, null);

        assertNotNull(instanceApp);

        T appSpec = class1.cast(instanceApp.getServiceObject());

        return appSpec;

    }
    
    /**
     * This method allows to verify the state of the bundle to make sure that we can perform tasks on it
     * 
     * @param time
     */  
    public void waitForIt(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            assert false;
        }

        if(context!=null)
        while (
        // context.getBundle().getState() != Bundle.STARTING &&
        context.getBundle().getState() != Bundle.ACTIVE // &&
        // context.getBundle().getState() != Bundle.STOPPING
        ) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                System.err.println("waitForIt failed.");
            }
        }
    }

    public <S> S getAService(Class<S> clazz) {
        Object o = osgi.getServiceObject(clazz.getName(), null);
        S s = clazz.cast(o);
        assertNotNull(s);
        return s;
    }

    public OSGiHelper getOSGiHelper() {
        return osgi;
    }

    public IPOJOHelper getIpojoHelper() {
        return ipojo;
    }
    
    public void printBundleList(){
        System.out.println("---------List of installed bundle--------");
        for (Bundle bundle : context.getBundles()) {
            System.out.println("- " + bundle.getLocation() + " ["+bundle.getState()+"]" );
        }
        System.out.println("---------End of List of installed bundle--------");
    }
}
