package fr.imag.adele.apam;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.apamAPI.CompExInst;
import fr.imag.adele.apam.apamAPI.CompExType;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Instance;

public class ApplicationImpl implements Application {

    private final Set<Composite>            composites    = new HashSet<Composite>();
    private static Map<String, Application> applications  = new ConcurrentHashMap<String, Application>();

    private final String                    name;
    private CompExType                      mainImplCompo = null;
    private CompExInst                      mainInstCompo = null;
    //    private ASMImpl              mainImpl      = null;
    //    private ASMSpec              mainSpec      = null;

    // To have different names
    private static int                      nbSameName    = 0;

    public String getNewName() {
        ApplicationImpl.nbSameName++;
        String newName = name + "-" + ApplicationImpl.nbSameName;
        return newName;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Creation from an implementation, from its URL
     * 
     * @param appliName
     * @param models
     * @param implName
     * @param url
     * @param type
     * @param specName
     * @param properties
     */
    public ApplicationImpl(String appliName, Set<ManagerModel> models, String implName, URL url,
            String specName, Attributes properties) {
        name = appliName;
        if (name.equals(implName)) {
            System.err.println("ERROR : application and main implementation must have different names " + name);
        }
        mainImplCompo = CompExTypeImpl.createCompExType(this, name, models, implName, url, specName, properties);
        mainInstCompo = (CompExInst) mainImplCompo.createInst(null, properties);
        ApplicationImpl.applications.put(appliName, this);
        //        mainImplCompo = new CompositeImpl(appliName, null, this, models);
        //        mainInstCompo = new CompositeImpl(appliName, null, this, models);
        //
        //        ASMImpl mainImpl;
        //        ASMInst mainInst;
        //        mainImplCompo = CompExTypeImpl.createCompExType(this, name, models);
        //        mainImpl = CST.ASMImplBroker.createImpl(mainImplCompo, implName, url, specName, properties);
        //        mainInst = mainImpl.createInst(mainInstCompo, properties);
        //
        //        ((CompositeImpl) mainImplCompo).setMainSpec(mainImpl.getSpec());
        //        ((CompositeImpl) mainImplCompo).setMainImpl(mainImpl);
        //        ((CompositeImpl) mainInstCompo).setMainInst(mainInst);
    }

    /**
     * Creation from an interface or spec only.
     * Only instanciate mainSpec and the first composite.
     * 
     * @param appliName
     * @param models
     * @param specName
     * @param specUrl
     * @param specType
     * @param interfaces
     * @param properties
     */
    public ApplicationImpl(String appliName, Set<ManagerModel> models, String specName, URL specUrl, String specType,
            String[] interfaces, Attributes properties) {
    	
        name = appliName;
        
        ASMSpec mainSpec;
        if (specUrl == null)
            mainSpec = CST.ASMSpecBroker.createSpec(mainImplCompo, specName, interfaces, properties);
        else
            mainSpec = CST.ASMSpecBroker.createSpec(mainImplCompo, specName, specUrl, specType, interfaces, properties);
        
        mainImplCompo = CompExTypeImpl.createCompExType(this,mainSpec.getASMName(),mainSpec.getInterfaceNames(),models);
        mainInstCompo = (CompExInst) mainImplCompo.createInst(null, properties);
        ApplicationImpl.applications.put(appliName, this);
    	
//        name = appliName;
//        mainImplCompo = new CompositeImpl(appliName, null, this, models);
//        mainInstCompo = new CompositeImpl(appliName, null, this, models);
//        ASMSpec mainSpec;
//
//        if (specUrl == null)
//            mainSpec = CST.ASMSpecBroker.createSpec(mainImplCompo, specName, interfaces, properties);
//        else
//            mainSpec = CST.ASMSpecBroker.createSpec(mainImplCompo, specName, specUrl, specType, interfaces, properties);
//        //mainSpec = CST.ASMSpecBroker.createSpec(mainCompo, specName, interfaces, properties);
//        ((CompositeImpl) mainImplCompo).setMainSpec(mainSpec);
//        ApplicationImpl.applications.put(appliName, this);
    }

//    //hummmm ....
//    public ApplicationImpl(String appliName) {
//        name = appliName;
//        mainImplCompo = new CompositeImpl(appliName, null, this, null);
//        mainInstCompo = new CompositeImpl(appliName, null, this, null);
//        ApplicationImpl.applications.put(appliName, this);
//    }

    //    private void setAppliComposite (ApplicationImpl appli) {
    //        ((CompositeImpl)mainImplCompo).setMainImpl(mainImpl) ;
    //        ((CompositeImpl)mainImplCompo).setMainSpec(mainImpl.getSpec()) ;
    //        ((CompositeImpl)mainInstCompo).setMainInst(mainInst) ;
    //        
    //        
    //    }

    /**
     * Creation from an implementation known by its name. The implementation will be searched by the various available
     * managers, and deployed if needed.
     * 
     * @param appliName
     * @param models
     * @param samImplName : name known by the platforms (in OBR, iPOJO, OSGi ...). If null using implName.
     * @param implName : logical name known by Apam.
     * @param specName
     * @param properties
     */
    public ApplicationImpl(String appliName, Set<ManagerModel> models, String samImplName, String implName,
            String specName, Attributes properties) {
    	name = appliName;
        String mainImplemName = (implName == null) ? samImplName : implName;
        if (name.equals(mainImplemName)) {
            System.err.println("ERROR : application must have a different name than the main implementation" + name);
        }
        mainImplCompo = CompExTypeImpl.createCompExType(this, mainImplemName, models);
        mainInstCompo = (CompExInst) mainImplCompo.createInst(null, properties);
        ApplicationImpl.applications.put(appliName, this);
        //        if (!appliName.equals(implName)) {
        //            (mainImplCompo.getMainImpl()).setASMName(implName);
        //        }
        //mainImplCompo = new CompositeImpl(appliName, null, this, models);
        //        mainInstCompo = new CompositeImpl(appliName, null, this, models);
        //        ASMSpec mainSpec;
        //        ASMImpl mainImpl;
        //        ASMInst mainInst = null;
        //
        //        Implementation samImpl = null;
        //
        //        if (samImplName != null) {
        //            try { //if allready existing in SAM
        //                samImpl = CST.SAMImplBroker.getImplementation(samImplName);
        //            } catch (Exception e) {
        //            }
        //            if (samImpl != null) {
        //                mainImpl = CST.ASMImplBroker.addImpl(mainImplCompo, implName, samImplName, specName, properties);
        //                Instance samInst = null;
        //                //reuse existing instance if possible (avoid to loop !)
        //                try {
        //                    samInst = samImpl.getInstance();
        //                } catch (ConnectionException e) {
        //                    e.printStackTrace();
        //                }
        //                if (samInst != null) {
        //                    mainInst = CST.ASMInstBroker.addInst(mainImplCompo, mainInstCompo, samInst, implName, specName,
        //                            properties);
        //                } else
        //                    mainInst = mainImpl.createInst(mainInstCompo, properties);
        //                ((CompositeImpl) mainImplCompo).setMainSpec(mainImpl.getSpec());
        //                ((CompositeImpl) mainImplCompo).setMainImpl(mainImpl);
        //                ((CompositeImpl) mainInstCompo).setMainInst(mainInst);
        //                return;
        //            }
        //        }
        //        //This implem does not exist in Sam nor Apam. We only have the name. Look for it and install.
        //        if (implName == null)
        //            implName = samImplName;
        //        samImplName = null;
        //        mainInst = ((CST.apam)).resolveAppli(mainImplCompo, mainInstCompo, samImplName, implName, null, null);
        //        if (mainInst == null) {
        //            System.err.println("cannot find " + implName);
        //            return;
        //        }
        //
        //        ((CompositeImpl) mainImplCompo).setMainSpec(mainInst.getImpl().getSpec());
        //        ((CompositeImpl) mainImplCompo).setMainImpl(mainInst.getImpl());
        //        ((CompositeImpl) mainInstCompo).setMainInst(mainInst);
    }

    //Creating applications
    //from APAM
    public static Application createAppliDeployImpl(String appliName, Set<ManagerModel> models, String implName,
            URL url, String specName, Attributes properties) {
        if ((appliName == null) || (url == null)) {
            System.err.println("ERROR : missing parameters for create application");
            return null;
        }
        if (ApplicationImpl.getApplication(appliName) != null) {
            System.out.println("Warning : Application allready existing, creating another instance");
            CompExType main = (CompExType) ApplicationImpl.getApplication(appliName).getMainImplComposite();
            ((ASMImpl) main).createInst(null, properties);
            //TODO BUG should return the instance 
            return ApplicationImpl.getApplication(appliName);
        }

        Application appli = new ApplicationImpl(appliName, models, implName, url, specName, properties);
        //            appli.getMainImpl().createInst(appli.getMainInstComposite(), properties);

        return appli;
    }

    public static Application createAppli(String appliName, Set<ManagerModel> models, String samImplName,
            String implName, String specName, Attributes properties) {
        if (appliName == null) {
            System.err.println("ERROR : appli Name is missing in create Appli");
            return null;
        }
        if (ApplicationImpl.getApplication(appliName) != null) {
            System.out.println("Warning : Application allready existing, creating another instance");
            CompExType main = (CompExType) ApplicationImpl.getApplication(appliName).getMainImplComposite();
            ((ASMImpl) main).createInst(null, properties);
            //TODO BUG should return the instance 
            return ApplicationImpl.getApplication(appliName);
        }

        Application appli = new ApplicationImpl(appliName, models, samImplName, implName, specName, properties);
        return appli;
    }

    /**
     * Creates an application from scratch, by deploying an implementation. First creates the root composites
     * (compositeName), associates its models (models). Then install an implementation (implName) from its URL,
     * considered as the application Main. All parameters are mandatory.
     * 
     * @param compositeName The name of the root composite.
     * @param models The manager models
     * @param specName optional : the logical name of the associated specification
     * @param specUrl Location of the code (interfaces) associated with the main specification.
     * @param specType Type of packaging for the code (interfaces) associated with the main specification.
     * @param properties The initial properties for the Implementation.
     * @return The new created application.
     */
    public static Application createAppliDeploySpec(String appliName, Set<ManagerModel> models, String specName,
            URL specUrl,
            String specType, String[] interfaces, Attributes properties) {
        if ((appliName == null) || (specName == null) || (specUrl == null) || (specType == null)
                || (interfaces == null)) {
            System.err.println("ERROR : missing parameters for create application");
            return null;
        }
        if (ApplicationImpl.getApplication(appliName) != null) {
            System.out.println("Warning : Application allready existing, creating another instance");
        }

        if (ApplicationImpl.applications.get(appliName) != null)
            appliName = ((ApplicationImpl) ApplicationImpl.applications.get(appliName)).getNewName();

        Application appli = new ApplicationImpl(appliName, models, specName, specUrl, specType, interfaces, properties);
        if (appli != null)
            ApplicationImpl.applications.put(appliName, appli);
        return appli;
    }

    public static Application getApplication(String name) {
        for (Application appli : ApplicationImpl.applications.values()) {
            if (name.equals(appli.getName()))
                return appli;
        }
        return null;
    }

    public static Set<Application> getApplications() {
        return new HashSet<Application>(ApplicationImpl.applications.values());
    }

    @Override
    public String getName() {
        return name;
    }

    //    @Override
    //    public void execute(Attributes properties) {
    //        mainImpl.createInst(mainInstCompo, properties);
    //    }

    @Override
    public CompExType getMainImplComposite() {
        return mainImplCompo;
    }

    @Override
    public CompExInst getMainInstComposite() {
        return mainInstCompo;
    }

    @Override
    public ASMImpl getMainImpl() {
        return mainImplCompo.getMainImpl();
    }

    @Override
    public ASMSpec getMainSpec() {
        return mainImplCompo.getMainSpec();
    }

    @Override
    public Set<Composite> getComposites() {
        return Collections.unmodifiableSet(composites);
    }

    public boolean addComposite(Composite composite) {
        if (composites.contains(composite))
            return false;
        composites.add(composite);
        return true;
    }

    @Override
    public Composite getComposite(String name) {
        for (Composite compo : composites) {
            if (name.equals(compo.getName()))
                return compo;
        }
        return null;
    }
}
