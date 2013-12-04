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
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ObrUtil {

	public static class SaxHandler extends DefaultHandler {

		/*
		 * The configured local repository in the settings file
		 */
		File localRepository = null;

		/*
		 * Whether we are parsing the local repository tag
		 */
		private boolean parsingRepositoryPath = false;

		/*
		 * Temporary buffering of path, only used if parsing location path
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

		public URL getRepo() throws MalformedURLException {

			String repositoryDirectoryName = repositoryPath.toString().trim();

			if (repositoryDirectoryName.isEmpty()) {
				return null;
			}

			File repositoryDirectory = new File(repositoryDirectoryName);
			File repository = new File(repositoryDirectory, "repository.xml");

			if (!repository.exists()) {
				return null;
			}

			return repository.toURI().toURL();
		}

	}

	public static final String LOCAL_MAVEN_REPOSITORY = "LocalMavenRepository";
	public static final String DEFAULT_OSGI_REPOSITORIES = "DefaultOSGiRepositories";
	public static final String REPOSITORIES = "Repositories";
	public static final String COMPOSITES = "Composites";

	public static final String OSGI_OBR_REPOSITORY_URL = "obr.repository.url";

	public static final String ROOT_MODEL_URL = "apam.root.model.url";

	private static Logger logger = LoggerFactory.getLogger(ObrUtil.class);

	public static File getM2Folder() {
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

	public static void printCap(Capability aCap) {
		System.out.println("   Capability name: " + aCap.getName());
		for (Property prop : aCap.getProperties()) {
			System.out.println("     " + prop.getName() + " type= " + prop.getType() + " val= " + prop.getValue());
		}
	}

	public static void printRes(Resource aResource) {
		System.out.println("\n\nRessource SymbolicName : " + aResource.getSymbolicName());
		for (Capability aCap : aResource.getCapabilities()) {
			printCap(aCap);
		}
	}

	public static URL searchMavenRepoFromSettings(File pathSettings) {
		// Look for <localRepository>
		try {

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();

			SaxHandler handler = new SaxHandler();

			saxParser.parse(pathSettings, handler);

			return handler.getRepo();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static URL searchRepositoryFromDefaultLinux() {
		try {
			File m2Folder = getM2Folder();
			if (m2Folder == null) {
				return null;
			}
			File repositoryFile = new File(new File(m2Folder, "repository"), "repository.xml");
			if (repositoryFile.exists()) {
				URL repo = repositoryFile.toURI().toURL();
				logger.info("Default Linux repository :" + repo);
				return repo;
			}
		} catch (MalformedURLException e) {
			logger.error(e.getMessage());
		}
		return null;
	}

	public static File searchSettingsFromM2Home() {
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

	public static File searchSettingsFromUserHome() {
		File m2Folder = getM2Folder();
		if (m2Folder == null) {
			return null;
		}
		File settings = new File(m2Folder, "settings.xml");
		if (settings.exists()) {
			return settings;
		}
		return null;
	}

	public String printProperties(Property[] props) {
		StringBuffer ret = new StringBuffer();
		for (Property prop : props) {
			ret.append(prop.getName() + "=" + prop.getValue() + ",  ");
		}
		return ret.toString();
	}

}
