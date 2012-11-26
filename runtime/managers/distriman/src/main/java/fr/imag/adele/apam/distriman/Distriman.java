package fr.imag.adele.apam.distriman;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.tracker.ComponentTrackerCustomizer;
import fr.imag.adele.apam.util.tracker.InstanceTracker;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.ipojo.api.composite.CompositeComponentType;
import org.apache.felix.ipojo.api.composite.ImportedService;
import org.apache.felix.ipojo.composite.CompositeServiceContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@org.apache.felix.ipojo.annotations.Component(name = "Apam::Distriman")
@Instantiate
public class Distriman  implements ComponentTrackerCustomizer<Instance>{

    //Default logger
    private static Logger logger = LoggerFactory.getLogger(Distriman.class);

    private final InstanceTracker tracker;


    private CompositeServiceContext roseContext;

    private final CompositeComponentType roseComp;
    private final BundleContext context;

    public Distriman(BundleContext context) {
        this.tracker = new InstanceTracker(ApamFilter.newInstance(""),this);
        this.context = context;

        //define the RoSe composite
        roseComp = new CompositeComponentType();
        roseComp.setBundleContext(context);
        roseComp.setComponentTypeName("Apam::Distriman:rose");
        roseComp.setPublic(true);

        //Import all RoSe factory
        ImportedService roseFacDep = new ImportedService().setAggregate(true).setOptional(false);
        roseFacDep.setFilter("(factory.name=RoSe_*)");
        roseFacDep.setSpecification(Factory.class.getName());
        roseFacDep.setId("RoSe_Factories");
        roseComp.addSubService(roseFacDep);

        //Import the HttpService
        ImportedService httpService = new ImportedService().setAggregate(false).setOptional(false);
        httpService.setSpecification(HttpService.class.getName());
        httpService.setId("http");
        roseComp.addSubService(httpService);

        //Import the LogService
        ImportedService logService = new ImportedService().setAggregate(false).setOptional(true);
        logService.setSpecification(LogService.class.getName());
        logService.setId("logger");
        roseComp.addSubService(logService);

        roseComp.addInstance(new org.apache.felix.ipojo.api.composite.Instance("RoSe_Wui").addProperty("wui.root","/apam"));
        roseComp.addInstance(new org.apache.felix.ipojo.api.composite.Instance("MyComp"));


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
        try {
            ComponentInstance instance = roseComp.createInstance();
            roseContext = new CompositeServiceContext(context,instance);
            instance.start();

            ServiceReference[] prefs = roseContext.getAllServiceReferences(null,null);
//            for(ServiceReference ref : prefs){
//                System.out.println(ref);
//            }



        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        //roseContext = (CompositeServiceContext) roseComp.getFactory().getBundleContext();

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

    @Override
    public void addingComponent(Instance component) {
      //  Dictionary<String,?> properties = new Hashtable<String, Object>();
      //  roseContext.registerService("",component.getServiceObject(),null);
    }

    @Override
    public void removedComponent(Instance component) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
