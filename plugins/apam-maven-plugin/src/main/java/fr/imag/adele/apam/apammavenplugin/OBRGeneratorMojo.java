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
package fr.imag.adele.apam.apammavenplugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.manipulator.render.MetadataRenderer;
import org.apache.felix.ipojo.manipulator.store.JarFileResourceStore;
import org.apache.felix.ipojo.manipulator.store.builder.DefaultManifestBuilder;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.plugin.ManipulatorMojo;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import edu.emory.mathcs.backport.java.util.Arrays;
import fr.imag.adele.apam.apammavenplugin.helpers.EnrichElementsHelper;
import fr.imag.adele.apam.apammavenplugin.helpers.JarHelper;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.util.ApamMavenProperties;
import fr.imag.adele.apam.util.CoreParser.ErrorHandler;

/**
 * Packages an OSGi jar "iPOJO bundle" as an "APAM bundle".
 * 
 * @version $Rev$, $Date$
 * @extendsPlugin maven-ipojo-plugin
 * @goal apam-bundle
 * @extendsGoal ipojo-bundle
 * @phase package
 * @requiresrelationResolution runtime
 * @description manipulate an OSGi bundle jar to include the obr.xml file and
 *              build APAM bundle
 * 
 * @author ApAM Team
 */
public class OBRGeneratorMojo extends ManipulatorMojo implements ErrorHandler {

	public static Map<String, VersionRange> versionRange = new HashMap<String, VersionRange>();

	public static String thisBundleVersion;

	/**
	 * The Maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * The project groupID
	 */
	public static String currentProjectGroupId;

	/**
	 * The project artifactID
	 */
	public static String currentProjectArtifactId;

	/**
	 * The project version
	 */
	public static String currentProjectVersion;

	/**
	 * The project file
	 * 
	 * @parameter default-value="${project.artifact}"
	 * @required
	 * @readonly
	 */
	private Artifact artifact;

	/**
	 * @parameter default-value="${basedir}
	 */
	private File baseDirectory;

	private boolean parsingFailed = false;

	/**
	 * Contains all classes and resources
	 */
	public static ClasspathDescriptor classpathDescriptor;

	/**
	 * Execute method : this method launches the OBR generation.
	 * 
	 * @throws MojoExecutionException
	 *             : an exception occurs during the OBR generation..
	 * 
	 */
	@Override
	public void execute() throws MojoExecutionException {

		currentProjectArtifactId = project.getArtifact().getArtifactId();
		currentProjectGroupId = project.getArtifact().getGroupId();
		currentProjectVersion = project.getArtifact().getVersion();

		thisBundleVersion = currentProjectVersion.replace('-', '.');
		classpathDescriptor = new ClasspathDescriptor();

		try {

			super.execute();

			JarHelper myHelper = new JarHelper(artifact.getFile(), this, getLog());

			getLog().info("ApAM metadata manipulator");

			// The jar to compile
			List<ComponentDeclaration> components = myHelper.getApAMComponents();
			if (components.isEmpty()) {
				throw new InvalidApamMetadataException("No Apam metadata");
			}

			classpathDescriptor.add(artifact.getFile());
			/*
			 * Get the definition of the components needed to compile
			 */
			List<ComponentDeclaration> dependencies = new ArrayList<ComponentDeclaration>();

			/*
			 * Clear statics
			 */
			versionRange.clear();


			
			/*
			 * Get all COMPILE scope dependencies transitively
			 * 
			 */
			Set<Artifact> requiredArtifacts = new HashSet<Artifact>();
			for (Object artifact : project.getArtifacts()) {
				if (artifact instanceof Artifact) {
					requiredArtifacts.add((Artifact) artifact);
				}				
			}

			/*
			 * Add also directly referenced SYSTEM scope dependencies, as they may
			 * contain required declarations or classes
			 */
			for (Object dependency : project.getDependencyArtifacts()) {
				if (artifact instanceof Artifact) {
					Artifact artifact = (Artifact) dependency;
					if ("system".equalsIgnoreCase(artifact.getScope())) {
						requiredArtifacts.add((Artifact) artifact);
					}
				}				
			}
			
			/*
			 * Load APAM declarations from required artifacts and calculate a classpath to look for
			 * referenced classes
			 */
			for (Artifact requiredArtifact : requiredArtifacts) {

					VersionRange range = requiredArtifact.getVersionRange();

					OBRGeneratorMojo.versionRange.put(requiredArtifact.getArtifactId(), range);

					List<ComponentDeclaration> subcomponents = new JarHelper(requiredArtifact.getFile(), this, getLog()).getApAMComponents();
					if (subcomponents != null)
						dependencies.addAll(subcomponents);

					classpathDescriptor.add(requiredArtifact.getFile());
			}

			CheckObr.setLogger(getLog());
			
			ApamRepoBuilder arb = new ApamRepoBuilder(components, dependencies);
			StringBuffer obrContent = arb.writeOBRFile();
			if (CheckObr.getFailedChecking()) {
				throw new MojoExecutionException(
						"Metadata Apam compilation failed.");
			}
			if (parsingFailed) {
				throw new MojoExecutionException(
						"Invalid xml Apam Metadata syntax");
				// error(Severity.ERROR, "Invalid xml Apam Metadata syntax");
			}

			OutputStream obr;
			String obrFileStr = baseDirectory.getAbsolutePath()
					+ File.separator + "src" + File.separator + "main"
					+ File.separator + "resources" + File.separator + "obr.xml";
			File obrFile = new File(obrFileStr);

			// maven ?? copies first in target/classes before to look in
			// src/resources
			// and copies src/resources/obr.xml to target/classes *after* obr
			// modification
			// Thus we delete first target/classes/obr.xml to be sure the newly
			// generated obr.xml file will be used

			String oldObrFileStr = baseDirectory.getAbsolutePath()
					+ File.separator + "target" + File.separator + "classes"
					+ File.separator + "obr.xml";
			File oldObrFile = new File(oldObrFileStr);
			if (oldObrFile.exists()) {
				oldObrFile.delete();
			}

			if (!obrFile.exists()) {
				obrFile.getParentFile().mkdirs();
			}
			obr = new FileOutputStream(obrFile);
			obr.write(obrContent.toString().getBytes());
			obr.flush();

			// Map<String, Element> map =
			// ObrAdditionalProperties.parseFile(obrFile,getLog());
			// System.err.println("Obr file : " + obrFile.getAbsolutePath());
			obr.close();
			
			updateJarFile(myHelper);

		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
			error(Severity.ERROR, e.getMessage());
			throw new MojoExecutionException(e.getMessage());
		}

		getLog().info(" obr.xml File generation - SUCCESS ");
	}

	@Override
	public void error(Severity severity, String message) {
		switch (severity) {
		case ERROR : 
			getLog().error("error parsing component declaration : " + message);
			parsingFailed = true;
			break;
		case WARNING : 
		case SUSPECT :
			getLog().info("warning parsing component declaration : " + message);
			break;
		}
	}

	public void updateJarFile(JarHelper myHelper) throws MojoExecutionException {
		try {
			File newOutput = new File(baseDirectory.getAbsolutePath()
					+ File.separator + "target" + File.separator + "_temp.jar");
			if (newOutput.exists()) {
				newOutput.delete();
			}

			JarFileResourceStore store = new JarFileResourceStore(myHelper.getJarFile(), newOutput);
			store.setManifest(myHelper.getManifest());

			DefaultManifestBuilder builder = new DefaultManifestBuilder();
			builder.setMetadataRenderer(new MetadataRenderer());

			Element metadata = myHelper.getiPojoMetadata();
			Set<PropertyDefinition> addedDefinitions = new HashSet<PropertyDefinition>();
			Map<String, String> addedProperties = new HashMap<String, String>();

			additionalProperties(addedDefinitions,addedProperties);
			

			EnrichElementsHelper.addPropertiesToChildrenApAMComponents(
					metadata, addedDefinitions, addedProperties);

			@SuppressWarnings("unchecked")
			Collection<Element> myCollec = Arrays
					.asList(metadata.getElements());

			builder.addMetada(myCollec);
			store.setManifestBuilder(builder);
			store.close();

			artifact.getFile().delete();

			newOutput.renameTo(artifact.getFile());
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
			error(Severity.ERROR, e.getMessage());
			throw new MojoExecutionException(e.getMessage());
		}

	}
	
	private void additionalProperties(Set<PropertyDefinition> addedDefinitions,
			Map<String, String> addedProperties) {
		
		//TODO Ugly hardcoded way to add built properties, check another way
		
		addedDefinitions.add(new PropertyDefinition(
				new SpecificationDeclaration("Dummy"), "apam.version",
				"version", null, null, null, null));
		addedProperties.put("apam.version",
				ApamMavenProperties.mavenVersion.replace('-', '.'));

		addedDefinitions.add(new PropertyDefinition(
				new SpecificationDeclaration("Dummy"), "maven.groupId",
				"string", null, null, null, null));
		addedProperties.put("maven.groupId",
				OBRGeneratorMojo.currentProjectGroupId);
		
		addedDefinitions.add(new PropertyDefinition(
				new SpecificationDeclaration("Dummy"), "maven.artifactId",
				"string", null, null, null, null));
		addedProperties.put("maven.artifactId",
				OBRGeneratorMojo.currentProjectArtifactId);
		
		addedDefinitions.add(new PropertyDefinition(
				new SpecificationDeclaration("Dummy"), "maven.version",
				"string", null, null, null, null));
		addedProperties.put("maven.version",
				OBRGeneratorMojo.currentProjectVersion);
		
		addedDefinitions.add(new PropertyDefinition(
				new SpecificationDeclaration("Dummy"), "version",
				"version", null, null, null, null));
		addedProperties.put("version",
				OBRGeneratorMojo.thisBundleVersion);			
		
	}

}
