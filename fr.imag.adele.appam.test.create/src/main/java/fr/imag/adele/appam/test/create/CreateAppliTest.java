package fr.imag.adele.appam.test.create;

import java.io.File;
import java.net.MalformedURLException;

import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.apamAPI.Application;

public class CreateAppliTest implements Runnable {
    // iPOJO injected
    Apam apam;
    
    /**
     * The test is performed in its own thread triggered when the component is activated
     */
    public void run() {
		try {
			Application appli = apam.createAppli("monAppliADeployer", null,"DependencyTest", new File("C:/Users/vega/workspace/fr.imag.adele.appam.test.dependency/target/test.dependency-1.0.0.jar").toURI().toURL(), "bundle",null, null);
	        apam.dumpApam();
	        appli.execute(null);
	        apam.dumpApam();
	        
	        appli = apam.createAppli("monAppliDejaDeploye", null, "DependencyTest", null, null, null);
	        apam.dumpApam();
	        appli.execute(null);
	        apam.dumpApam();
	        
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
        
    	
    }

    @SuppressWarnings("unused")
    private void start() {
    	new Thread(this,"APAM test").start();
    }

}
