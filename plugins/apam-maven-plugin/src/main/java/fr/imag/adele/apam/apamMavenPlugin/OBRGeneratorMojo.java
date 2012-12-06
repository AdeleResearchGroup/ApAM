/*
 * Copyright 2010-2011 Universite Joseph Fourier
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.imag.adele.apam.apamMavenPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.felix.ipojo.plugin.ManipulatorMojo;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.util.Util;

/**
 * Packages an OSGi jar "iPOJO bundle" as an "APAM bundle".
 * 
 * @version $Rev$, $Date$
 * @extendsPlugin maven-ipojo-plugin
 * @goal apam-bundle
 * @extendsGoal ipojo-bundle 
 * @phase package
 * @requiresDependencyResolution runtime
 * @description manipulate an OSGi bundle jar to include the obr.xml file and build APAM bundle
 * 
 * @author ApAM Team
 */
public class OBRGeneratorMojo extends ManipulatorMojo {

	/**
	 * Local Repository.
	 * 
	 * @parameter expression="${localRepository}"
	 * @required
	 * @readonly
	 */
	protected ArtifactRepository            localRepository;

	/**
	 * 
	 * 
	 * @parameter expression="${dependencyObrList}"
	 */
//	private List<URL>                       dependencyObrList;

	/**
	 * 
	 * 
	 * @parameter expression="${noLocalObr}"
	 */
//	private boolean                         noLocalObr;

	// The list of bundle dependencies of the form "groupId.name.version"
//	public static Set<String>               bundleDependencies = new HashSet<String>();
//
	public static Map<String, VersionRange> versionRange       = new HashMap<String, VersionRange>();
//
	public static String                    thisBundleVersion;

	Logger                                  logger             = LoggerFactory.getLogger(OBRGeneratorMojo.class);

	public static String currentProjectGroupId ;
	public static String currentProjectArtifactId ;
	public static String currentProjectVersion ;
	/**
	 * Execute method : this method launches the OBR generation.
	 * 
	 * @throws MojoExecutionException
	 *             : an exception occurs during the OBR generation..
	 * @see fr.imag.adele.obr.ipojo.plugin.OBRGeneratorMojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException {
		super.execute();

		currentProjectGroupId= getProject().getGroupId() ;
		currentProjectArtifactId = getProject().getArtifactId();
		currentProjectVersion = getProject().getVersion();
		thisBundleVersion = getProject().getVersion().replace('-', '.');

		try {

			getLog().info("Start bundle header manipulation");
			File jar = getProject().getArtifact().getFile();
			//The jar to compile
			List<ComponentDeclaration> components = getComponentFromJar(jar);

			/*
			 * Get the definition of the components needed to compile
			 */
			List <ComponentDeclaration> dependencies = new ArrayList <ComponentDeclaration> ();
			for (Object artifact : getProject().getDependencyArtifacts()) {
				if (artifact instanceof Artifact) {
					Artifact dependency = (Artifact) artifact;
					VersionRange range = dependency.getVersionRange();
					OBRGeneratorMojo.versionRange.put(dependency.getArtifactId(), range);
					dependencies.addAll(getComponentFromJar(((Artifact) artifact).getFile()));
				}
			}

			ApamRepoBuilder arb = new ApamRepoBuilder(components, dependencies);
			StringBuffer obrContent = arb.writeOBRFile();
			if (CheckObr.getFailedChecking()) {
				throw new MojoExecutionException("Metadata Apam compilation failed.");
			}
			if (Util.getFailedParsing()) {
				throw new MojoExecutionException("Invalid xml Apam Metadata syntax");
			}

			OutputStream obr;
			String obrFileStr = getProject().getBasedir().getAbsolutePath()
			+ File.separator + "src"
			+ File.separator + "main"
			+ File.separator + "resources"
			+ File.separator + "obr.xml";
			File obrFile = new File(obrFileStr);

			// maven ?? copies first in target/classes before to look in src/resources
			// and copies src/resources/obr.xml to target/classes *after* obr modification
			// Thus we delete first target/classes/obr.xml to be sure the newly generated obr.xml file will be used

			String oldObrFileStr = getProject().getBasedir().getAbsolutePath()
			+ File.separator + "target"
			+ File.separator + "classes"
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
			obr.close();

		} catch (FileNotFoundException e) {
			getLog().error(e.getMessage(), e);
			// System.err.println("Cannot open for writing : " + obrFile.getAbsolutePath());
		} catch (MalformedURLException e) {
			getLog().error(e.getMessage(), e);
		} catch (IOException e) {
			getLog().error(e.getMessage(), e);
//		} catch (ParseException e) {
//			getLog().error(e.getMessage(), e);
		}
		getLog().info(" obr.xml File generation - SUCCESS ");
	}

	private List<ComponentDeclaration> getComponentFromJar (File jar) {
		try {
			JarFile jarFile = new JarFile(jar);
			Manifest manifest = jarFile.getManifest();
			// manifest.getAttributes("").
			Attributes iPOJOmetadata = manifest.getMainAttributes();
			String ipojoMetadata = iPOJOmetadata.getValue("iPOJO-Components");

			iPOJOmetadata = null;
			manifest = null;
			jarFile.close();
			if (ipojoMetadata == null) {
				getLog().info(" No Apam metadata for " + jar );
				return Collections.EMPTY_LIST;
			}
			getLog().info("Parsing Apam metadata for " + jar + " - SUCCESS ");
			Element root = ManifestMetadataParser
			.parseHeaderMetadata(ipojoMetadata);
			List<ComponentDeclaration> ret = Util.getComponents(root);
			String contains = "    contains components: " ;
			for (ComponentDeclaration comp : ret) {
				contains += comp.getName() + " ";
			}
			getLog().info(contains) ;
			return ret ;
			
		} catch (Exception e) {
			getLog().info("Parsing Apam metadata for " + jar + " - FAILED ");
			e.printStackTrace() ;
		}
		return null ;
	}



}
