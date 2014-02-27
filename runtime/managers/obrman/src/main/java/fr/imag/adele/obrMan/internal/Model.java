/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package fr.imag.adele.obrMan.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.impl.CompositeTypeImpl;

/**
 * This class represents the OBR model associated with a given composite.
 * 
 * The model specifies the bundle repositories available when a resolution is requested
 * in the specified context.
 * 
 * @author vega
 *
 */
public class Model {

	private static Logger logger = LoggerFactory.getLogger(Model.class);


	/**
	 * Global framework properties used to configure OBR
	 */
	private static final String OSGI_OBR_REPOSITORY_URL 	= "obr.repository.url";
	private static final String ROOT_MODEL_URL 				= "apam.root.model.url";

	/**
	 * Properties available in the OBRMan model associated with a composite
	 */
	private static final String LOCAL_MAVEN_REPOSITORY 		= "LocalMavenRepository";
	private static final String DEFAULT_OSGI_REPOSITORIES	= "DefaultOSGiRepositories";
	private static final String REPOSITORIES 				= "Repositories";
	private static final String COMPOSITES 					= "Composites";

	/**
	 * Loads the OBRMan model of the specified context, and return the list of configured repositories specified
	 * in the model.
	 * 
	 * Returns NULL if the model is not specified, or can not be loaded.
	 * 
	 */
	public static Model loadModel(OBRMan obrManager, CompositeType context, BundleContext osgiContext)  {

		/*
		 * Get the model, if specified
		 */
		ManagerModel model = context.getModel(obrManager);
		if (model == null || model.getURL() == null)
			return null;
		
		/*
		 * Try to load the model from the specified location, as a map of properties
		 */
		Properties configuration = null;
		try {
			configuration = new Properties();
			configuration.load(model.getURL().openStream());

			return new Model(obrManager, context, configuration, osgiContext);

		} catch (IOException e) {
			logger.error("Invalid OBRMAN Model. Cannot read stream " + model.getURL(), e.getCause());
			return null;
		}

	}

	/**
	 * Loads the OBR model associated with the root composite context
	 */
	public static Model loadDefaultRootModel(OBRMan obrManager, BundleContext osgiContext)  {
		Properties configuration = new Properties();
		
		/*
		 * Try loading from a globally specified platform location
		 */
		String rootModelLocation = osgiContext.getProperty(ROOT_MODEL_URL);
		if (configuration.isEmpty() && rootModelLocation != null) {
			try {
				configuration.load(new URL(rootModelLocation).openStream());
			} catch (IOException e) {
				logger.error("Invalid OBRMAN Model. Cannot read global root model at " + rootModelLocation, e.getCause());
			}
		}

		/*
		 * If no configuration was specified, initialize the default
		 */
		if (configuration.isEmpty()) {
			configuration.put(Model.LOCAL_MAVEN_REPOSITORY, "true");
			configuration.put(Model.DEFAULT_OSGI_REPOSITORIES, "true");
		}
		
		return new Model(obrManager, CompositeTypeImpl.getRootCompositeType(), configuration, osgiContext);
	}
	
	/**
	 * Loads the OBR model associated with the root composite context from a given location.
	 * 
	 * This is useful to force at runtime to use a given repository for the root context. We do not try
	 * to use the default behavior, but fail with a error if the location is invalid
	 */
	public static Model loadRootModel(OBRMan obrManager, BundleContext osgiContext, URL location) throws IOException  {
		
		Properties configuration = new Properties();
		configuration.load(location.openStream());
		
		return new Model(obrManager, CompositeTypeImpl.getRootCompositeType(), configuration, osgiContext);
	}

	/**
	 * The list of configured repositories
	 */
	private final Set<URI> repositories;
	
	/**
	 * Builds a new model from the specified configuration
	 */
	private Model(OBRMan obrManager, CompositeType context, Properties configuration, BundleContext osgiContext) {

		this.repositories	= new HashSet<URI>();
		
		/*
		 * Get the repository list, specified in the model 
		 */
		for (Object key : configuration.keySet()) {
			
			if (LOCAL_MAVEN_REPOSITORY.equals(key)) {
				// Add the obr repository located in the local maven repository
				boolean useMavenBundleRepository = Boolean.valueOf(configuration.getProperty(LOCAL_MAVEN_REPOSITORY));

				if (useMavenBundleRepository) {
					try {
						repositories.add(getLocalMavenBundleRepository());
					} catch (Exception e) {
						logger.error("Error when adding default local repository to obr manager", e.getCause());
					}
				}
			} 
			else if (REPOSITORIES.equals(key)) {
				// Add obr repositories declared in the composite
				for (String repositoryLocation : configuration.getProperty(REPOSITORIES).split("\\s+")) {
					try {
						repositories.add(new URI(repositoryLocation));
					} catch (Exception e) {
						logger.error("Error when adding default local repository to obr manager :" + repositoryLocation, e.getCause());
					}
				}

			} 
			else if (Model.DEFAULT_OSGI_REPOSITORIES.equals(key)) {
				// Add obr repositories declared in the osgi configuration file
				boolean useGlobalOSGiRepository = Boolean.valueOf(configuration.getProperty(DEFAULT_OSGI_REPOSITORIES));

				if (useGlobalOSGiRepository) {
					String osgiRepositoryLocations = osgiContext.getProperty(OSGI_OBR_REPOSITORY_URL);
					if (osgiRepositoryLocations != null) {
						for (String osgiRepositoryLocation : osgiRepositoryLocations.split("\\s+")) {
							try {
								repositories.add(new URI(osgiRepositoryLocation));
							} catch (Exception e) {
								logger.error("Error when adding default local repository to obr manager :" + osgiRepositoryLocations, e.getCause());
							}
						}
					}

				}
			} 
			else if (Model.COMPOSITES.equals(key)) {
				// look for obr repositories in other composites
				String[] otherCompositesRepositories = configuration.getProperty(COMPOSITES).split("\\s+");

				for (String composite : otherCompositesRepositories) {
					CompositeType importedContext = obrManager.getApam().getCompositeType(composite);
					Model importedModel = importedContext != null ? obrManager.getModel(importedContext) : null;
					if (importedContext != null && importedModel != null) {
						repositories.addAll(importedModel.getRepositoryLocations());
					} else {
						// If the compositeType is not present, do nothing
						logger.error("The composite " + context.getName() + " reference a missing composite " + composite);
					}
				}
			}
		}

	}


	/**
	 * The list of repositories configured in this model 
	 */
	public Set<URI> getRepositoryLocations() {
		return repositories;
	};
	
	public Set<String> getRepositories() {
		Set<String> result = new HashSet<String>();
		for (URI repositoryLocation :repositories) {
			result.add(repositoryLocation.toString());
		}
		return result;
	}
	
	/**
	 * The OSGi Bundle Repository associated with the local maven installation.
	 */
	private static URI getLocalMavenBundleRepository() {

		// try to find the maven settings.xml file
		File globalSettings = getGlobalMavenSettings();
		File userSettings	= getUserMavenSettings(); 
		
		/*
		 *  Extract localRepository from settings.xml
		 *  
		 *  If a global and a user settings are both specified, the
		 *  user preferences override the global
		 */
		URI userDefinedRepository 	= null;
		URI globalDefinedRepository = null;
		URI defaultRepository 		= null;

		if (userSettings != null) {
			userDefinedRepository = getMavenBundleRepository(userSettings);
		}

		if (globalSettings != null) {
			globalDefinedRepository = getMavenBundleRepository(globalSettings);
		}

		defaultRepository = getDefaultBundleRepository();
		
		if (userDefinedRepository != null) {
			return userDefinedRepository;
		}

		if (globalDefinedRepository != null) {
			return globalDefinedRepository;
		}

		if (defaultRepository != null) {
			return defaultRepository;
		}

		throw new IllegalArgumentException(new NullPointerException(
				"Could not find local repository location in : "+userSettings+" "+globalSettings+" "+defaultRepository)
		);

	}


	/**
	 * Get the OSGi Bundle Repository associated with the maven repository location specified in
	 * the maven settings file.
	 * 
	 */
	private static URI getMavenBundleRepository(File settingsFile) {
		try {

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();

			MavenSettingsParser handler = new MavenSettingsParser();

			saxParser.parse(settingsFile, handler);

			return handler.getMavenBundleRepository();
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	public static class MavenSettingsParser extends DefaultHandler {

		/*
		 * Whether we are parsing the local repository tag
		 */
		private boolean parsingRepositoryPath = false;

		/*
		 * Temporary buffering of path, only used if currently parsing location path
		 */
		StringBuilder repositoryPath = new StringBuilder();

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (qName.equalsIgnoreCase("localRepository")) {
				parsingRepositoryPath = true;
			}
		}

		@Override
		public void characters(char ch[], int start, int length) throws SAXException {
			if (parsingRepositoryPath && ch != null) {
				repositoryPath.append(ch, start, length);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (parsingRepositoryPath) {
				parsingRepositoryPath = false;
			}
		}

		public URI getMavenBundleRepository() throws MalformedURLException {

			String repositoryDirectoryName = repositoryPath.toString().trim();

			if (repositoryDirectoryName.isEmpty()) {
				return null;
			}

			File repositoryDirectory = new File(repositoryDirectoryName);
			File repository = new File(repositoryDirectory, "repository.xml");

			if (!repository.exists()) {
				return null;
			}

			return repository.toURI();
		}

	}
	
	/**
	 * Get the repository at the default maven location, when there is no specific configuration
	 */
	private static URI getDefaultBundleRepository() {

		File m2Folder = getUserMavenConfiguration();
		if (m2Folder == null) {
			return null;
		}
		
		File repositoryFile = new File(new File(m2Folder, "repository"), "repository.xml");
		if (repositoryFile.exists()) {
			URI repo = repositoryFile.toURI();
			logger.info("Default Linux repository :" + repo);
			return repo;
		}
		
		return null;
	}

	/**
	 * Get the user defined maven configuration directory
	 */
	private static File getUserMavenConfiguration() {
		String user_home = System.getProperty("user.home");
		if (user_home == null) {
			user_home = System.getProperty("HOME");
			if (user_home == null) {
				return null;
			}
		}
		File user_home_file = new File(user_home);
		File m2Folder = new File(user_home_file, ".m2");
		return m2Folder;
	}

	/**
	 * Get the global maven settings file
	 */
	private static File getGlobalMavenSettings() {
		String m2_home = System.getenv().get("M2_HOME");

		if (m2_home == null) {
			return null;
		}
		File m2_Home_file = new File(m2_home);
		File settings = new File(new File(m2_Home_file, "conf"), "settings.xml");
		if (settings.exists()) {
			return settings;
		}
		return null;
	}

	/**
	 * Get the user defined maven settings file
	 */
	private static File getUserMavenSettings() {
		File m2Folder = getUserMavenConfiguration();
		if (m2Folder == null) {
			return null;
		}
		File settings = new File(m2Folder, "settings.xml");
		if (settings.exists()) {
			return settings;
		}
		return null;
	}


}
