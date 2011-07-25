package fr.imag.adele.apam;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Instance;

public class ApplicationImpl implements Application {

    private final Set<Composite> composites    = new HashSet<Composite>();

    private final String         name;
    private Composite            mainImplCompo = null;
    private Composite            mainInstCompo = null;
    private ASMImpl              mainImpl      = null;
    private ASMSpec              mainSpec      = null;

    // To have different names
    private static int           nbSameName    = 0;

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
    public ApplicationImpl(String appliName, Set<ManagerModel> models, String implName, URL url, String type,
            String specName, Attributes properties) {
        name = appliName;
        mainImplCompo = new CompositeImpl(appliName, null, this, models);
        mainInstCompo = new CompositeImpl(appliName, null, this, models);
        mainImpl = CST.ASMImplBroker.createImpl(mainImplCompo, implName, url, type, specName, properties);
        mainSpec = mainImpl.getSpec();
        mainImpl.createInst(mainInstCompo, properties);

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
        mainImplCompo = new CompositeImpl(appliName, null, this, models);
        mainInstCompo = new CompositeImpl(appliName, null, this, models);
        if (specUrl == null)
            mainSpec = CST.ASMSpecBroker.createSpec(mainImplCompo, specName, interfaces, properties);
        else
            mainSpec = CST.ASMSpecBroker.createSpec(mainImplCompo, specName, specUrl, specType, interfaces, properties);
        //mainSpec = CST.ASMSpecBroker.createSpec(mainCompo, specName, interfaces, properties);
        mainImpl = null;
    }

    /**
     * Creation from an implementation known by its name. The implementation will be searched by the various available
     * managers,
     * and deployed if needed.
     * 
     * @param appliName
     * @param models
     * @param samImplName
     * @param implName
     * @param specName
     * @param properties
     */
    public ApplicationImpl(String appliName, Set<ManagerModel> models, String samImplName, String implName,
            String specName, Attributes properties) {
        name = appliName;
        mainImplCompo = new CompositeImpl(appliName, null, this, models);
        mainInstCompo = new CompositeImpl(appliName, null, this, models);
        ASMInst mainInst = null;
        Implementation samImpl = null;

        if (samImplName != null) {
            try { //if allready existing in SAM
                samImpl = CST.SAMImplBroker.getImplementation(samImplName);
            } catch (Exception e) {
            }
            if (samImpl != null) {
                mainImpl = CST.ASMImplBroker.addImpl(mainImplCompo, implName, samImplName, specName, properties);
                Instance samInst = null;
                //reuse existing instance if possible (avoid to loop !)
                try {
                    samInst = samImpl.getInstance();
                } catch (ConnectionException e) {
                    e.printStackTrace();
                }
                if (samInst != null) {
                    CST.ASMInstBroker.addInst(mainImplCompo, mainInstCompo, samInst, implName, specName, properties);
                } else
                    mainImpl.createInst(mainInstCompo, properties);
                mainSpec = mainImpl.getSpec();
                return;
            }
        }
        //This implem does not exist in Sam nor Apam. We only have the name. Look for it and install.
        if (implName == null)
            implName = samImplName;
        samImplName = null;
        mainInst = ((CST.apam)).resolveAppli(mainImplCompo, mainInstCompo, samImplName, implName);
        if (mainInst == null) {
            System.err.println("cannot find " + implName);
            return;
        }
        mainImpl = mainInst.getImpl();
        mainSpec = mainImpl.getSpec();
    }

    // Not in the interface
    // In case the appli has been created from a spec only.
    public void setMainImpl(ASMImpl impl) {
        if ((mainImpl == null) && (impl.getSpec() == mainSpec))
            mainImpl = impl;
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
    public Composite getMainImplComposite() {
        return mainImplCompo;
    }

    @Override
    public Composite getMainInstComposite() {
        return mainInstCompo;
    }

    @Override
    public ASMImpl getMainImpl() {
        return mainImpl;
    }

    @Override
    public ASMSpec getMainSpec() {
        return mainSpec;
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
