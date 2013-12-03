package fr.imag.adele.histMan;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoException;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.DynamicManager;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.PropertyManager;
import fr.imag.adele.apam.impl.CompositeTypeImpl;

public class HistMan implements PropertyManager, DynamicManager {

	public static class HistManData {
		public String histURL;
		public String histDBName;
		public Integer histDBTimeout;
		public String dropCollections;
		public String dbName;
		public String dbHost;
		public int dbPort;

		public HistManData(Properties prop) {
			this.histURL = prop.getProperty(DBURL_KEY);
			this.histDBName = prop.getProperty(DBNAME_KEY);
			try {
				this.histDBTimeout = Integer.parseInt((String)prop.get(DBTIMEOUT_KEY));
			} catch (NumberFormatException e) {
				this.histDBTimeout = Integer.parseInt(DBTIMEOUT_DEFAULT);
			}

			this.dropCollections = prop.getProperty(DBDROP_KEY);
			this.dbName = prop.getProperty(DBNAME_KEY);
			try {
			this.dbPort = Integer.parseInt(prop.getProperty(DBPORT_KEY));
			} catch (NumberFormatException e) {
				this.histDBTimeout = Integer.parseInt(DBPORT_DEFAULT);
			}
			
		}
	}
	
	private int dbNameCounter=0;

	// Link compositeType with it instance of obrManager
	private final Map<String, String> histDbURLs;

	private final Logger logger = LoggerFactory.getLogger(HistMan.class);

	private Map<String, Properties> histModels = new HashMap<String, Properties>();
	private MongoClient mongoClient;

	/*
	 * The collection containing the attributes created, changed and removed.
	 */
	private static final String ChangedAttributes = "Attr";

	/*
	 * The collection containing the entities (spec, implems, instances)
	 * created, and deleted
	 */
	private static final String Entities = "ME";

	/*
	 * The collection containing the links (wires) created, and deleted
	 */
	private static final String Links = "Links";

	private static final String DEFAULT_MODEL = "default";

	private static final String DBHOST_KEY = "DBHost";

	private static final String DBHOST_DEFAULT = "localhost";

	private static final String DBPORT_KEY = "DBPort";

	private static final String DBPORT_DEFAULT = "27017";

	private static final String DBURL_KEY = "DBUrl";

	private static final String DBNAME_KEY = "DBName";
	private static final String DBNAME_DEFAULT = "ApamRootHistory";

	private static final String DBTIMEOUT_KEY = "DBTimeout";
	private static final String DBTIMEOUT_DEFAULT = "3000";
	private static final String DBDROP_KEY = "dropCollectionsOnStart";

	private static final Object DBDROP_DEFAULT = "true";

	private DB db = null;

	/**
	 * HISTMAN activated, register with APAM
	 */

	public HistMan(BundleContext context) {
		histDbURLs = new HashMap<String, String>();
	}

	@Override
	public void addedComponent(Component comp) {
		logger.info("Adding component");

		try {

			// force connection to be established
			mongoClient.getDatabaseNames();

			DBCollection ME = db.getCollection(Entities);

			BasicDBObject created = new BasicDBObject("name", comp.getName())
					.append("time", System.currentTimeMillis()).append("op",
							"created");

			for (Map.Entry<String, Object> e : comp.getAllProperties()
					.entrySet()) {
				created.append(e.getKey().replace('.', '_'), e.getValue()
						.toString());
			}

			ME.insert(created);

		} catch (MongoException e) {

			stop();
		}

	}

	@Override
	public void addedLink(Link wire) {

		try {

			// force connection to be established
			mongoClient.getDatabaseNames();

			DBCollection ChangedLink = db.getCollection(Links);

			BasicDBObject newLink = new BasicDBObject("name", wire.getSource()
					.getName()).append("time", System.currentTimeMillis())
					.append("linkType", "Wire")
					.append("linkId", wire.getName())
					.append("added", wire.getDestination().getName());

			ChangedLink.insert(newLink);
		} catch (MongoException e) {

			stop();
		}

	}

	@Override
	public void attributeAdded(Component comp, String attr, String newValue) {

		try {

			// force connection to be established
			mongoClient.getDatabaseNames();

			DBCollection ChangedAttr = db.getCollection(ChangedAttributes);

			BasicDBObject newVal = new BasicDBObject("name", comp.getName())
					.append("time", System.currentTimeMillis())
					.append("op", "added").append("attribute", attr)
					.append("value", newValue);
			for (Map.Entry<String, Object> e : comp.getAllProperties()
					.entrySet()) {
				newVal.append(e.getKey(), e.getValue().toString());
			}
			ChangedAttr.insert(newVal);
		} catch (MongoException e) {
			stop();
		}
	}

	@Override
	public void attributeChanged(Component comp, String attr, String newValue,
			String oldValue) {

		try {

			// force connection to be established
			mongoClient.getDatabaseNames();

			DBCollection ChangedAttr = db.getCollection(ChangedAttributes);

			BasicDBObject newVal = new BasicDBObject("name", comp.getName())
					.append("time", System.currentTimeMillis())
					.append("op", "changed").append("attribute", attr)
					.append("value", newValue).append("oldValue", oldValue);

			for (Map.Entry<String, Object> e : comp.getAllProperties()
					.entrySet()) {
				newVal.append(e.getKey(), e.getValue().toString());
			}
			ChangedAttr.insert(newVal);
		} catch (MongoException e) {

			stop();
		}
	}

	@Override
	public void attributeRemoved(Component comp, String attr, String oldValue) {

		try {

			// force connection to be established
			mongoClient.getDatabaseNames();

			DBCollection ChangedAttr = db.getCollection(ChangedAttributes);

			BasicDBObject newVal = new BasicDBObject("name", comp.getName())
					.append("time", System.currentTimeMillis())
					.append("op", "removed").append("attribute", attr)
					.append("oldValue", oldValue);

			for (Map.Entry<String, Object> e : comp.getAllProperties()
					.entrySet()) {
				newVal.append(e.getKey(), e.getValue().toString());
			}
			ChangedAttr.insert(newVal);
		} catch (MongoException e) {

			stop();
		}
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof HistMan) {

			return this.getName().equals(((HistMan) obj).getName());

		}

		return false;

	}

	@Override
	public String getName() {
		return CST.HISTMAN;
	}

	@Override
	public int getPriority() {
		return 10;
	}

	@Override
	public int hashCode() {
		return (this.getName() == null ? 0 : this.getName().hashCode());
	}

	private Properties addDefaultProperties(String modelName, Properties prop_model) {
		if (prop_model == null) {
			prop_model = new Properties();
		}
		
		logger.debug("For model : "+modelName);
		if (prop_model.get(DBHOST_KEY) == null)
			prop_model.put(DBHOST_KEY, DBHOST_DEFAULT);
		logger.debug(" -> loaded DB Host : "+prop_model.get(DBHOST_KEY));
		if (prop_model.get(DBPORT_KEY) == null)
			prop_model.put(DBPORT_KEY, DBPORT_DEFAULT);
		logger.debug(" -> loaded DB Port : "+prop_model.get(DBPORT_KEY));
		if (prop_model.get(DBURL_KEY) == null)
			prop_model.put(DBURL_KEY, DBHOST_DEFAULT.concat(":"+prop_model.get(DBPORT_KEY)));
		logger.debug(" -> loaded DB URL : "+prop_model.get(DBURL_KEY));
		if (prop_model.get(DBNAME_KEY) == null)
			prop_model.put(DBNAME_KEY, DBNAME_DEFAULT.concat(String.valueOf(dbNameCounter++)));
		logger.debug(" -> loaded DB Name : "+prop_model.get(DBNAME_KEY));
		if (prop_model.get(DBTIMEOUT_KEY) == null)
			prop_model.put(DBTIMEOUT_KEY, DBTIMEOUT_DEFAULT);
		logger.debug(" -> loaded DB Timeout : "+prop_model.get(DBTIMEOUT_KEY));

		if (prop_model.get(DBDROP_KEY) == null)
			prop_model.put(DBDROP_KEY, DBDROP_DEFAULT);
		logger.debug(" -> loaded DB Dropping Collection : "+prop_model.get(DBDROP_KEY));

		
		histModels.put(modelName, prop_model);
		return prop_model;
	}

	private Properties loadProperties(ManagerModel model) {
		/*
		 * if no model for the compositeType, set the default values
		 */
		if (model == null) {
			return addDefaultProperties(DEFAULT_MODEL, null);
		} else {
			try {// try to load the compositeType model
				logger.info("Loading properties from {}", model.getURL());
				Properties prop_model = new Properties();
				prop_model.load(model.getURL().openStream());
				return addDefaultProperties(model.getManagerName(), prop_model);

			} catch (IOException e) {// if impossible to load the model for the
				// compositeType, set the root composite
				logger.error(
						"Invalid Model. Cannot be read stream "
								+ model.getURL(), e.getCause());
				return addDefaultProperties(model.getManagerName(), null);
			}
		}
	}

	@Override
	public void newComposite(ManagerModel model, CompositeType compositeType) {
		logger.debug("HISTMAN, newComposite(ManagerModel model = "
					+(model==null?"null":model.getManagerName())
					+ "CompositeType compositeType = "
					+(compositeType==null?"null":compositeType.getName()));

		if (model == null) { // model is root
			model = CompositeTypeImpl.getRootCompositeType().getModel(
					this.getName());
		}

		
		HistManData data =new HistManData(loadProperties(model));

		try {

			Builder options = new MongoClientOptions.Builder();

			options.connectTimeout(data.histDBTimeout);

			if(mongoClient==null) {
				mongoClient = new MongoClient(data.histURL, options.build());
			}

			logger.info("trying to connect with database {} in host {}",
					data.histDBName, data.histURL);

			// force connection to be established
			mongoClient.getDatabaseNames();

			db = mongoClient.getDB(data.histDBName);

		} catch (Exception e) {
			logger.error("{} is inactive, it was unable to find the DB in {}",
					this.getName(), data.histURL);
		}

		histDbURLs.put(compositeType.getName(), data.histURL);

		try {

			// force connection to be established
			mongoClient.getDatabaseNames();

			/*
			 * if attribute dropComection is true, drop all collections
			 */
			if (data.dropCollections.equals("true")) {
				db.getCollection(Entities).drop();
				db.getCollection(ChangedAttributes).drop();
				db.getCollection(Links).drop();
			}

		} catch (MongoException e) {
			logger.error("no Mongo Database at URL {} name {}", model.getURL(),
					data.histDBName);
			stop();
		}

	}

	@Override
	public void removedComponent(Component comp) {

		try {

			// force connection to be established
			mongoClient.getDatabaseNames();

			logger.info("removing component");
			DBCollection ME = db.getCollection(Entities);

			BasicDBObject created = new BasicDBObject("name", comp.getName())
					.append("time", System.currentTimeMillis()).append("op",
							"deleted");
			ME.insert(created);

		} catch (MongoException e) {

			stop();
		}

	}

	@Override
	public void removedLink(Link wire) {

		try {

			// force connection to be established
			mongoClient.getDatabaseNames();

			DBCollection ChangedLink = db.getCollection(Links);

			BasicDBObject newLink = new BasicDBObject("name", wire.getSource()
					.getName()).append("time", System.currentTimeMillis())
					.append("linkType", "Wire")
					.append("linkId", wire.getName())
					.append("removed", wire.getDestination().getName());

			ChangedLink.insert(newLink);

		} catch (MongoException e) {

			stop();
		}

	}

	public void start() throws Exception {

		ApamManagers.addPropertyManager(this);
		ApamManagers.addDynamicManager(this);

	}

	public void stop() {
		ApamManagers.removePropertyManager(this);
		ApamManagers.removeDynamicManager(this);
		histDbURLs.clear();
	}

}
