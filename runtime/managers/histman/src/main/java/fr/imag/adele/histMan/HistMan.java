package fr.imag.adele.histMan;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DependencyManager;
import fr.imag.adele.apam.DynamicManager;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.PropertyManager;
import fr.imag.adele.apam.Resolved;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.declarations.DependencyDeclaration;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Util;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.ServerAddress;


//import fr.imag.adele.obrMan.OBRManCommand;
//import fr.imag.adele.obrMan.internal.OBRManager.Selected;
//import fr.imag.adele.obrMan.internal.ObrUtil;

public class HistMan implements DependencyManager, PropertyManager, DynamicManager {

    // Link compositeType with it instance of obrManager
    private final Map<String, String> histDbURLs;

    private final Logger  logger = LoggerFactory.getLogger(HistMan.class);

    private long timeout = 10000;
    private static final String ROOT_MODEL_URL   = "apam.root.model.url";
    private static final String DB_URL   = "DBUrl";
    private final BundleContext           m_context;
    
    
    private 
    private DBCollection ChangedLink = db.getCollection("ChangedLinks");



    /**
     * HISTMAN activated, register with APAM
     */

    public HistMan(BundleContext context) {
    	m_context = context ;
        histDbURLs = new HashMap<String, String>();
    }

    public void start() {
        ApamManagers.addDependencyManager(this, 20);
        ApamManagers.addPropertyManager(this);
        ApamManagers.addDynamicManager(this);
        logger.info("[HISTMAN] started");
    }

    public void stop() {
        ApamManagers.removeDependencyManager(this);
        ApamManagers.removePropertyManager(this);
        ApamManagers.removeDynamicManager(this);
        histDbURLs.clear();
        logger.info("[HISTMAN] stopped");
    }



    @Override
    public void newComposite(ManagerModel model, CompositeType compositeType) {
        String histURL = null;
        if (model == null) { // if no model for the compositeType, set the root composite model
            histURL = searchOBRManager(compositeType);
        } else {
            try {// try to load the compositeType model
                LinkedProperties histModel = new LinkedProperties();
                histModel.load(model.getURL().openStream());
                Enumeration<?> keys = histModel.keys();
                while (keys.hasMoreElements()) {

                    String key = (String) keys.nextElement();
                    if (DB_URL.equals(key)) {
                    	histURL = histModel.getProperty(key) ;
                    }
                }
            } catch (IOException e) {// if impossible to load the model for the compositeType, set the root composite
                                     // model
                logger.error("Invalid OBRMAN Model. Cannot be read stream " + model.getURL(), e.getCause());
                histURL = searchOBRManager(compositeType);
            }
        }
        histDbURLs.put(compositeType.getName(), histURL);
    }

    private String searchOBRManager(CompositeType compoType) {
        String dbURL = null;

        // in the case of root composite, compoType = null
        if (compoType != null) {
            dbURL = histDbURLs.get(compoType.getName());
        }

        // Use the root composite if the model is not specified
        if (dbURL == null) {
            dbURL = histDbURLs.get(CST.ROOT_COMPOSITE_TYPE);
            if (dbURL == null) { // If the root manager was never been initialized
                // lookFor root.HISTMAN.cfg and create a db for the root composite in a customized location
                String rootModelurl = m_context.getProperty(ROOT_MODEL_URL);
                try {// try to load root model from the customized location
                    if (rootModelurl != null) {
                        LinkedProperties histModel = new LinkedProperties();
                        URL urlModel = (new File(rootModelurl)).toURI().toURL();
                        histModel.load(urlModel.openStream());
                        Enumeration<?> keys = histModel.keys();
                        while (keys.hasMoreElements()) {

                            String key = (String) keys.nextElement();
                            if (DB_URL.equals(key)) {
                            	dbURL = histModel.getProperty(key) ;
                            }
                        }
                    }
                    
                } catch (Exception e) {// failed to load customized location, 
                                      
                    logger.error("Invalid Root URL Model. Cannot be read stream " + rootModelurl, e.getCause());
                    //unregister
                }
            }
        }
        if (dbURL == null) {
        	//remove history manager
        	stop () ;
        }
        return dbURL;
    }


    @Override
    public String getName() {
        return CST.HISTMAN;
    }

    // at the end
    @Override
    public void
            getSelectionPath(Instance client, DependencyDeclaration dep, List<DependencyManager> involved) {
        involved.add(involved.size(), this);
    }

    @Override
    public Instance resolveImpl(Instance client, Implementation impl, Set<String> constraints, List<String> preferences) {
        return null;
    }

    @Override
    public Set<Instance> resolveImpls(Instance client, Implementation impl, Set<String> constraints) {
        return null;
    }

    @Override
    public int getPriority() {
        return 3;
    }

    

    @Override
    public Resolved resolveDependency(Instance client, DependencyDeclaration dep, boolean needsInstances) {
        return null;
    }


    @Override
    public Component findComponentByName(Instance client, String componentName) {
        return null;
    }

    @Override
    public Specification findSpecByName(Instance client, String specName) {
        return null;
    }

    @Override
    public Implementation findImplByName(Instance client, String implName) {
        return null;
    }

    @Override
    public Instance findInstByName(Instance client, String instName) {
        return null ;
    }



    @Override
    public void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl,
            Instance inst, Set<Instance> insts) {
    	DBCollection ChangedLink = db.getCollection("ChangedLinks");

    	BasicDBObject newLink = new BasicDBObject ("name", client.getName()).
    	append("time", System.currentTimeMillis()).
    	append ("linkType", “Wire”).
    	append ("linkId", depName)) ;
    	
    	Set<Wire> wires = client.getWires(depName) ;
    	for (Wire w : client.getWires(depName)) {
    		newLink.append("target", w.getDestination().getName()) ;
    	}
    	ChangedLink.insert (newLink) ;
    }



    @Override
    public ComponentBundle findBundle(CompositeType compoType, String bundleSymbolicName, String componentName) {
    	return null ;
    }
//        if (bundleSymbolicName == null || componentName == null)
//            return null;
//
//        // Find the composite OBRManager
//        OBRManager obrManager = searchOBRManager(compoType);
//        if (obrManager == null)
//            return null;
//
//        return obrManager.lookForBundle(bundleSymbolicName, componentName);
//    }

	@Override
	public void addedComponent(Component newComponent) {
		DBCollection ME = db.getCollection("ModelingElement");

		BasicDBObject created = new BasicDBObject ("name", comp.getName()).
			append("time", System.currentTimeMillis()).
			append("op", "created")) ;
			
		for (Map.Entry<String, Object> e : newComponent.getAllProperties().entrySet()) {
		   created.append (e.getKey(), e.getValue().toString()));
		}
		ME.insert (created) ;
		
	}

	@Override
	public void removedComponent(Component lostComponent) {
		DBCollection ME = db.getCollection("ModelingElement");

		BasicDBObject created = new BasicDBObject ("name", comp.getName()).
			append("time", System.currentTimeMillis()).
			append("op", "deleted")) ;
		ME.insert (created) ;
		
	}

	@Override
	public void removedWire(Wire wire) {
    	DBCollection ChangedLink = db.getCollection("ChangedLinks");

    	BasicDBObject newLink = new BasicDBObject ("name", client.getName()).
    	append("time", System.currentTimeMillis()).
    	append ("linkType", “Wire”).
    	append ("linkId", wire.getDepName()).
    	append ("removed", wire.getDestination().getName());
    	
    	ChangedLink.insert (newLink) ;
		
	}

	@Override
	public void addedWire(Wire wire) {
    	DBCollection ChangedLink = db.getCollection("ChangedLinks");

    	BasicDBObject newLink = new BasicDBObject ("name", client.getName()).
    	append("time", System.currentTimeMillis()).
    	append ("linkType", “Wire”).
    	append ("linkId", wire.getDepName()).
    	append ("added", wire.getDestination().getName());
    	
    	ChangedLink.insert (newLink) ;
		
	}

	@Override
	public void attributeChanged(Component compo, String attr,
			String newValue, String oldValue) {
		DBCollection ChangedAttr = db.getCollection("ChangedAttributes");

		BasicDBObject newVal = new BasicDBObject ("name", comp.getName()).
		append("time", System.currentTimeMillis()).
		append("op", "changed").
		append ("attribute", attr).
		append ("value", newValue).
		append ("oldValue", oldValue);
		
		for (Map.Entry<String, Object> e : compo.getAllProperties().entrySet()) {
			newVal.append (e.getKey(), e.getValue().toString()));
		}
		ChangedAttr.insert (newVal) ;
		
	}

	@Override
	public void attributeRemoved(Component compo, String attr,
			String oldValue) {
		DBCollection ChangedAttr = db.getCollection("ChangedAttributes");

		BasicDBObject newVal = new BasicDBObject ("name", comp.getName()).
		append("time", System.currentTimeMillis()).
		append("op", "removed").
		append ("attribute", attr).
		append ("oldValue", oldValue);
		
		for (Map.Entry<String, Object> e : compo.getAllProperties().entrySet()) {
			newVal.append (e.getKey(), e.getValue().toString()));
		}
		
		ChangedAttr.insert (newVal) ;
		
	}

	@Override
	public void attributeAdded(Component compo, String attr, String newValue) {
		DBCollection ChangedAttr = db.getCollection("ChangedAttributes");

		BasicDBObject newVal = new BasicDBObject ("name", comp.getName()).
		append("time", System.currentTimeMillis()).
		append("op", "added").
		append ("attribute", attr).
		append ("value", newValue));
		for (Map.Entry<String, Object> e : compo.getAllProperties().entrySet()) {
			newVal.append (e.getKey(), e.getValue().toString()));
		}
		ChangedAttr.insert (newVal) ;
		
	}


}