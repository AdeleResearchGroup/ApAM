package fr.imag.adele.apam.apamImpl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.ApamResolver;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Manager;
import fr.imag.adele.apam.util.Attributes;

//import fr.imag.adele.sam.Implementation;

public class APAMImpl implements Apam {

    public static Manager                apamMan;

    private static Map<Manager, Integer> managersPrio = new HashMap<Manager, Integer>();
    public static List<Manager>          managerList  = new ArrayList<Manager>();

    public APAMImpl() {
        new CST(this);
        APAMImpl.apamMan = new ApamMan();
        ApamManagers.addManager(APAMImpl.apamMan, -1); // -1 to be sure it is not in the main loop
    }

    @Override
    public CompositeType createCompositeType(String name, String mainImplName,
            Set<ManagerModel> models, Attributes attributes) {
        return CompositeTypeImpl.createCompositeType(null, name, mainImplName, null,
                models, attributes);
    }

    @Override
    public CompositeType createCompositeType(String name, String mainImplName, Set<ManagerModel> models,
            URL mainBundle, String specName, Attributes attributes) {
        return CompositeTypeImpl.createCompositeType(null, name, models, mainImplName, mainBundle, specName,
                attributes);
    }

    @Override
    public Composite startAppli(String compositeName) {
        Implementation compoType = ApamResolver.findImplByName(null, compositeName);
        if (compoType == null)
            return null;
        if (compoType instanceof CompositeType)
            return startAppli((CompositeType) compoType);
        System.err.println("ERROR : " + compoType.getName() + " is not a composite.");
        return null;
    }

    @Override
    public Composite startAppli(URL compoURL, String compositeName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Composite startAppli(CompositeType composite) {
        return (Composite) composite.createInst(null, null);
    }

    @Override
    public CompositeType getCompositeType(String name) {
        return CompositeTypeImpl.getCompositeType(name);
    }

    @Override
    public Collection<CompositeType> getCompositeTypes() {
        return CompositeTypeImpl.getCompositeTypes();
    }

    @Override
    public Collection<CompositeType> getRootCompositeTypes() {
        return CompositeTypeImpl.getRootCompositeTypes();
    }

    @Override
    public Composite getComposite(String name) {
        return CompositeImpl.getComposite(name);
    }

    @Override
    public Collection<Composite> getComposites() {
        return CompositeImpl.getComposites();
    }

    @Override
    public Collection<Composite> getRootComposites() {
        return CompositeImpl.getRootComposites();
    }

}
