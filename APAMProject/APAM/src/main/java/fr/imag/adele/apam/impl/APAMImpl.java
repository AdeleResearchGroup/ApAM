package fr.imag.adele.apam.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Manager;
import fr.imag.adele.apam.ManagerModel;
//import fr.imag.adele.sam.Implementation;
//import org.osgi.framework.Bundle;
//import org.osgi.framework.BundleException;
//import org.osgi.framework.BundleActivator;

public class APAMImpl implements Apam {

    public static BundleContext context;
    public  Manager       apamMan;

    Logger logger = LoggerFactory.getLogger(APAMImpl.class);
    
    //    private static Map<Manager, Integer> managersPrio = new HashMap<Manager, Integer>();
    public static List<Manager> managerList = new ArrayList<Manager>();

    public APAMImpl(BundleContext context) {
        APAMImpl.context = context;
        new CST(this);
//        APAMImpl.apamMan = new ApamMan();
        ApamManagers.addManager(apamMan, -1); // -1 to be sure it is not in the main loop
    }

    @Override
    public CompositeType createCompositeType(String inCompoType, String name, String mainImplName,
            Set<ManagerModel> models, Map<String, Object> attributes) {
        Implementation fatherCompo = null;
        if (inCompoType != null) {
            fatherCompo = CST.apamResolver.findImplByName(null, inCompoType);
            if (fatherCompo == null)
                return null;
            if (!(fatherCompo instanceof CompositeType)) {
            	logger.error(inCompoType + " is not a composite type.");
                return null;
            }
        }
        return CompositeTypeImpl.createCompositeType((CompositeType) fatherCompo, name, mainImplName, null,
                models, attributes);
    }

    @Override
    public CompositeType createCompositeType(String inCompoType, String name, String mainImplName,
            Set<ManagerModel> models, URL mainBundle, String specName, Map<String, Object> attributes) {
        Implementation fatherCompo = null;
        if (inCompoType != null) {
            fatherCompo = CST.apamResolver.findImplByName(null, inCompoType);
            if (fatherCompo == null)
                return null;
            if (!(fatherCompo instanceof CompositeType)) {
            	logger.error(inCompoType + " is not a composite type.");
                return null;
            }
        }
        return CompositeTypeImpl.createCompositeType((CompositeType) fatherCompo, name, models, mainImplName,
                mainBundle, specName,
                attributes);
    }

    @Override
    public Composite startAppli(String compositeName) {
        Implementation compoType = CST.apamResolver.findImplByName(null, compositeName);
        if (compoType == null)
            return null;
        if (compoType instanceof CompositeType)
            return startAppli((CompositeType) compoType);
        logger.error("ERROR : " + compoType.getName() + " is not a composite.");
        return null;
    }

    @Override
    public Composite startAppli(URL compoURL, String compositeName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Composite startAppli(CompositeType composite) {
        return (Composite) ((CompositeTypeImpl) composite).createInst(null, null);
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

	public Manager getApamMan() {
		return apamMan;
	}

}
