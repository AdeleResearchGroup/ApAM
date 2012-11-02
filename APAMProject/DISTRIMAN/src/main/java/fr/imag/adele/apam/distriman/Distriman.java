package fr.imag.adele.apam.distriman;

import fr.imag.adele.apam.*;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@org.apache.felix.ipojo.annotations.Component(name = "Apam::Distriman")
@Instantiate
public class Distriman  {

    //Default logger
    private static Logger logger = LoggerFactory.getLogger(Distriman.class);


    private final BundleContext context;

    public Distriman(BundleContext context) {
        this.context = context;
    }

    public String getName() {
        return CST.DISTRIMAN;
    }

    @Validate
    private void init(){
        logInfo("Starting...");
        //Add Distriman to Apam
        logInfo("Successfully initialized");
    }

    @Invalidate
    private void stop(){
        logInfo("Stopping...");
        //Remove Distriman from Apam
        logInfo("Successfully stopped");
    }


    protected static void logInfo(String message,Throwable t){
        logger.info("["+CST.DISTRIMAN+"]"+message,t);
    }

    protected static void logInfo(String message){
        logger.info("["+CST.DISTRIMAN+"]"+message);
    }
}
