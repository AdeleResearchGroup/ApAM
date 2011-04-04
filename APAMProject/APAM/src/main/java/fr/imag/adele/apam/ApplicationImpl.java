package fr.imag.adele.apam;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.Attributes;

public class ApplicationImpl implements Application {

    private static Set<Composite> composites = new HashSet<Composite>();

    // A single Appli per APAM so far
    private static String         name;
    private static Composite      mainCompo  = null;
    private static ASMImpl        mainImpl   = null;

    public ApplicationImpl(String appliName, Set<ManagerModel> models, String implName, URL url, String type,
            String specName, Attributes properties) {
        ApplicationImpl.name = appliName;
        ApplicationImpl.mainCompo = new CompositeImpl(appliName, this, models);
        ApplicationImpl.mainImpl = ASM.ASMImplBroker.createImpl(ApplicationImpl.mainCompo, implName, url, type,
                specName, properties);
    }

    public ApplicationImpl(String appliName, Set<ManagerModel> models, String samImplName, String implName,
            String specName, Attributes properties) {
        ApplicationImpl.name = appliName;
        ApplicationImpl.mainCompo = new CompositeImpl(appliName, this, models);
        ApplicationImpl.mainImpl = ASM.ASMImplBroker.addImpl(ApplicationImpl.mainCompo, implName, samImplName,
                specName, properties);
    }

    @Override
    public String getName() {
        return ApplicationImpl.name;
    }

    @Override
    public void execute(Attributes properties) {
        ApplicationImpl.mainImpl.createInst(properties);
    }

    @Override
    public Composite getMainComposite() {
        return ApplicationImpl.mainCompo;
    }

    @Override
    public ASMImpl getMainImpl() {
        return ApplicationImpl.mainImpl;
    }

    @Override
    public Set<Composite> getComposites() {
        return Collections.unmodifiableSet(ApplicationImpl.composites);
    }

    public boolean addComposite(Composite composite) {
        if (ApplicationImpl.composites.contains(composite))
            return false;
        ApplicationImpl.composites.add(composite);
        return true;
    }

    @Override
    public Composite getComposite(String name) {
        for (Composite compo : ApplicationImpl.composites) {
            if (name.equals(compo.getName()))
                return compo;
        }
        return null;
    }
}
