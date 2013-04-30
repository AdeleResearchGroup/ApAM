package fr.imag.adele.histMan;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.PropertyManager;
import fr.imag.adele.apam.Link;
import fr.imag.adele.apam.impl.CompositeTypeImpl;

public class HistMan implements PropertyManager, DynamicManager {

	// Link compositeType with it instance of obrManager
	private final Map<String, String> histDbURLs;

	private final Logger logger = LoggerFactory.getLogger(HistMan.class);

	private static final String DB_NAME_KEY = "DBName";
	private static final String DB_NAME_VALUE_DEFAULT = "ApamRootHistory";

	private static final String DB_URL_KEY = "DBUrl";
	private static final String DB_URL_VALUE_DEFAULT = "localhost";

	private static final String DB_CONNECT_TIMEOUT_KEY = "DBTimeout";
	private static final String DB_CONNECT_TIMEOUT_VALUE_DEFAULT = "3000";
	private static final String DB_DROP_START = "dropCollectionsOnStart";

	private String histURL = null;
	private String histDBName = null;
	private Integer histDBTimeout = null;
	private String dropCollections = null;
	private LinkedProperties histModel = new LinkedProperties();
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

	private DB db = null;

	/**
	 * HISTMAN activated, register with APAM
	 */

	public HistMan(BundleContext context) {
		histDbURLs = new HashMap<String, String>();
	}

	public void start() throws Exception {

		ManagerModel model = CompositeTypeImpl.getRootCompositeType().getModel(
				this.getName());

		/*
		 * if no model for the compositeType, set the default values
		 */
		if (model == null) {
			histURL = DB_URL_VALUE_DEFAULT;
			histDBName = DB_NAME_VALUE_DEFAULT;
			histDBTimeout = Integer.parseInt(DB_CONNECT_TIMEOUT_VALUE_DEFAULT);
		} else {
			try {// try to load the compositeType model
				logger.info("Loading properties from {}", model.getURL());
				histModel.load(model.getURL().openStream());
				histURL = histModel.getProperty(DB_URL_KEY);
				histDBName = histModel.getProperty(DB_NAME_KEY,
						DB_NAME_VALUE_DEFAULT);

				// Case a non number has been assigned to the timeout property
				// in the properties file
				try {
					histDBTimeout = Integer.parseInt(histModel.getProperty(
							DB_CONNECT_TIMEOUT_KEY,
							DB_CONNECT_TIMEOUT_VALUE_DEFAULT));
				} catch (NumberFormatException e) {
					histDBTimeout = Integer
							.parseInt(DB_CONNECT_TIMEOUT_VALUE_DEFAULT);
				}

			} catch (IOException e) {// if impossible to load the model for the
										// compositeType, set the root composite
				logger.error("Invalid OBRMAN Model. Cannot be read stream "
						+ model.getURL(), e.getCause());
				throw e;
			}
		}

		try {

			Builder options = new MongoClientOptions.Builder();

			options.connectTimeout(histDBTimeout);

			mongoClient = new MongoClient(histURL, options.build());

			logger.info("trying to connect with database {} in host {}",
					histDBName, histURL);

			//force connection to be established
			mongoClient.getDatabaseNames();
			
			db = mongoClient.getDB(histDBName);
			
			ApamManagers.addPropertyManager(this);
			ApamManagers.addDynamicManager(this);

		} catch (Exception e) {
			logger.error("{} is inactive, it was unable to find the DB in {}",this.getName(),histURL);
		} 

	}

	public void stop() {
		ApamManagers.removePropertyManager(this);
		ApamManagers.removeDynamicManager(this);
		histDbURLs.clear();
	}

	@Override
	public void newComposite(ManagerModel model, CompositeType compositeType) {

		histDbURLs.put(compositeType.getName(), histURL);

		try {

			//force connection to be established
			mongoClient.getDatabaseNames();
			
			/*
			 * if attribute dropComection is true, drop all collections
			 */
			dropCollections = histModel.getProperty(DB_DROP_START, "true");
			if ("true".equals(dropCollections)) {
				db.getCollection(Entities).drop();
				db.getCollection(ChangedAttributes).drop();
				db.getCollection(Links).drop();
			}

		} catch (MongoException e) {
			logger.error("no Mongo Database at URL {} name {}", model.getURL(),
					histDBName);
			stop();
		}

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
	public void addedComponent(Component comp) {
		logger.info("Adding component");

		try {

			//force connection to be established
			mongoClient.getDatabaseNames();
			
			DBCollection ME = db.getCollection(Entities);

			BasicDBObject created = new BasicDBObject("name", comp.getName())
					.append("time", System.currentTimeMillis()).append("op",
							"created");

			for (Map.Entry<String, Object> e : comp.getAllProperties()
					.entrySet()) {
				created.append(e.getKey().replace('.','_'), e.getValue().toString());
			}

			ME.insert(created);

		} catch (MongoException e) {

			stop();
		}

	}

	@Override
	public void removedComponent(Component comp) {

		try {

			//force connection to be established
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
			
			//force connection to be established
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

	@Override
	public void addedLink(Link wire) {

		try {
			
			//force connection to be established
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
	public void attributeChanged(Component comp, String attr, String newValue,
			String oldValue) {

		try {
			
			//force connection to be established
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
			
			//force connection to be established
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
	public void attributeAdded(Component comp, String attr, String newValue) {

		try {
			
			//force connection to be established
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
	public boolean equals(Object obj) {

		if (obj instanceof HistMan) {

			return this.getName().equals(((HistMan) obj).getName());

		}

		return false;

	}

}
