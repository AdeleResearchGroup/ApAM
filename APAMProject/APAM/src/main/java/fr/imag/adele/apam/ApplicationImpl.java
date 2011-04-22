package fr.imag.adele.apam;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.Attributes;

public class ApplicationImpl implements Application {

    private final Set<Composite> composites = new HashSet<Composite>();

    private final String         name;
    private Composite            mainCompo  = null;
    private ASMImpl              mainImpl   = null;
    private ASMSpec              mainSpec   = null;

    // To have different names
    private static int           nbSameName = 0;

    public String getNewName() {
        ApplicationImpl.nbSameName++;
        String newName = name + "-" + ApplicationImpl.nbSameName;
        return newName;
    }

    @Override
    public String toString() {
        return name;
    }

    public ApplicationImpl(String appliName, Set<ManagerModel> models, String implName, URL url, String type,
            String specName, Attributes properties) {
        name = appliName;
        mainCompo = new CompositeImpl(appliName, this, models);
        composites.add(mainCompo);
        mainImpl = CST.ASMImplBroker.createImpl(mainCompo, implName, url, type, specName, properties);
        mainSpec = mainImpl.getSpec();
    }

    public ApplicationImpl(String appliName, Set<ManagerModel> models, String specName, URL specUrl, String specType,
            String[] interfaces, Attributes properties) {
        name = appliName;
        mainCompo = new CompositeImpl(appliName, this, models);
        composites.add(mainCompo);
        if (specUrl == null)
            CST.ASMSpecBroker.createSpec(mainCompo, specName, interfaces, properties);
        else
            CST.ASMSpecBroker.createSpec(mainCompo, specName, specUrl, specType, interfaces, properties);
        mainSpec = CST.ASMSpecBroker.createSpec(mainCompo, specName, interfaces, properties);
        mainImpl = null;
    }

    public ApplicationImpl(String appliName, Set<ManagerModel> models, String samImplName, String implName,
            String specName, Attributes properties) {
        name = appliName;
        mainCompo = new CompositeImpl(appliName, this, models);
        composites.add(mainCompo);
        mainImpl = CST.ASMImplBroker.addImpl(mainCompo, implName, samImplName, specName, properties);
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

    @Override
    public void execute(Attributes properties) {
        mainImpl.createInst(properties);
    }

    @Override
    public Composite getMainComposite() {
        return mainCompo;
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
