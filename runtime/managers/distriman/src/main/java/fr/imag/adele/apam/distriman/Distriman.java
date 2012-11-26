package fr.imag.adele.apam.distriman;

import fr.imag.adele.apam.CST;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.ipojo.api.composite.CompositeComponentType;
import org.apache.felix.ipojo.api.composite.ImportedService;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@org.apache.felix.ipojo.annotations.Component(name = "Apam::Distriman")
@Instantiate
public class Distriman {

    //Default logger
    private static Logger logger = LoggerFactory.getLogger(Distriman.class);

    private final CompositeComponentType roseComp;
    private final BundleContext context;

    public Distriman(BundleContext context) {
        this.context = context;

        //define the RoSe composite
        roseComp = new CompositeComponentType();
        roseComp.setBundleContext(context);
        roseComp.setComponentTypeName("Apam::Distriman:rose");
        roseComp.setPublic(true);

        //Import the HttpService
        ImportedService httpService = new ImportedService().setAggregate(false).setOptional(false).setScope(ImportedService.COMPOSITE_AND_GLOBAL_SCOPE);
        httpService.setSpecification(HttpService.class.getName());
        httpService.setId("http");
        roseComp.addSubService(httpService);

        //Import the PackageAdmin
        ImportedService packageAdmin = new ImportedService().setAggregate(false).setOptional(false).setScope(ImportedService.COMPOSITE_AND_GLOBAL_SCOPE);
        packageAdmin.setSpecification(PackageAdmin.class.getName());
        packageAdmin.setId("padmin");
        roseComp.addSubService(packageAdmin);

        //Import the LogService
        ImportedService logService = new ImportedService().setAggregate(false).setOptional(true).setScope(ImportedService.COMPOSITE_AND_GLOBAL_SCOPE);
        logService.setSpecification(LogService.class.getName());
        logService.setId("logger");
        roseComp.addSubService(logService);

        roseComp.addInstance(new org.apache.felix.ipojo.api.composite.Instance("InstanceBridge"));
        //roseComp.addInstance(new org.apache.felix.ipojo.api.composite.Instance("RoSe_Wui").addProperty("wui.root","/apam"));


//        //For JsonConfigurator
//        roseComp.addInstance( new org.apache.felix.ipojo.api.composite.Instance("RoSe_configurator.json"));
//
//        roseComp.addInstance(new org.apache.felix.ipojo.api.composite.Instance("json-service-provider-org.json"));
//
//        ExportedService fileListener = new ExportedService().setOptional(true).setAggregate(true);
//        fileListener.setSpecification(ArtifactListener.class.getName());
//        roseComp.addService(fileListener);
//
//        ExportedService fileInstaller = new ExportedService().setOptional(true).setAggregate(true);
//        fileInstaller.setSpecification(ArtifactInstaller.class.getName());
//        roseComp.addService(fileInstaller);
//
//        //Test
//        ExportedService roseTest = new ExportedService().setOptional(true).setAggregate(true);
//        roseTest.setSpecification(RoseMachine.class.getName());
//        roseComp.addService(roseTest);

    }

    public String getName() {
        return CST.DISTRIMAN;
    }

    @Validate
    private void init(){
        logInfo("Starting...");

        //Start the RoSe Composite
        roseComp.start();

        //Add Distriman to Apam
        logInfo("Successfully initialized");
    }

    @Invalidate
    private void stop(){
        logInfo("Stopping...");
        roseComp.stop();
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
