/*
 *  Copyright 2010-2011 Universite Joseph Fourier
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package fr.imag.adele.apam.apamMavenPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
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
import org.apache.maven.plugin.MojoExecutionException;

import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.util.Util;

/**
 * Packages an OSGi jar "iPOJO bundle" as an "APAM bundle".
 * 
 * @version $Rev$, $Date$
 * @extendsPlugin maven-ipojo-plugin
 * @goal ipojo-bundle
 * @phase package
 * @requiresDependencyResolution runtime
 * @description manipulate an OSGi bundle jar to include the obr.xml file and build APAM bundle
 * 
 * @author SIMON Eric (eric.simon<at>imag.fr) and Jacky Estublier (jacky<at>imag.fr)
 */
public class OBRGeneratorMojo extends ManipulatorMojo {


	/**
	 * Local Repository.
	 * 
	 * @parameter expression="${localRepository}"
	 * @required
	 * @readonly
	 */
	protected ArtifactRepository localRepository;

	// The list of bundle dependencies of the form "groupId.name.version"
	public static Set<String>    bundleDependencies = new HashSet<String>();

	public static Map <String, VersionRange> versionRange = new HashMap <String, VersionRange> () ;
	
	public static String thisBundleVersion ;


	/**
	 * Execute method : this method launches the OBR generation.
	 * 
	 * @throws MojoExecutionException
	 *             : an exception occurs during the OBR generation..
	 * @see fr.imag.adele.obr.ipojo.plugin.OBRGeneratorMojo#execute()
	 */
	public void execute() throws MojoExecutionException {
		super.execute();
		//obr.xml generation
		try {
			getLog().info("Start bundle header manipulation");
			File jar = getProject().getArtifact().getFile();
			JarFile jarFile = new JarFile(jar);
			Manifest manifest = jarFile.getManifest();
			Attributes iPOJOmetadata = manifest.getMainAttributes();
			String ipojoMetadata = iPOJOmetadata.getValue("iPOJO-Components");

			iPOJOmetadata = null;
			manifest = null;
			jarFile.close();
			if (ipojoMetadata == null) {
				getLog().info(" No iPOJO metadata - Failed ");
				return;
			}
			getLog().info("Parsing iPOJO metadata - SUCCESS ");
			Element root = ManifestMetadataParser
			.parseHeaderMetadata(ipojoMetadata);

			thisBundleVersion = getProject().getVersion().replace('-', '.');
//			System.out.println("getProject().getVersion() = "+getProject().getVersion());
//			System.out.println("getProject().getArtifact().getBaseVersion() = "+getProject().getArtifact().getBaseVersion());
//			System.out.println("getProject().getArtifact().getVersion() = "+getProject().getArtifact().getVersion());
//			System.out.println("getProject().getArtifact().getArtifactId() = "+getProject().getArtifact().getArtifactId());
			for (Object artifact : getProject().getDependencyArtifacts()) {
				if (artifact instanceof Artifact) {
					Artifact dependency = (Artifact) artifact;
					// 0.0.1.SNAPSHOT not 0.0.1-SNAPSHOT
					String version = dependency.getBaseVersion().replace('-', '.');
					VersionRange range = dependency.getVersionRange() ;
					//System.out.println("component " + artifact + " artifact id = " + dependency.getArtifactId()  + " version range = " + range + " version = " + version);
					//System.out.println(dependency.getRepository().getBasedir() + "  URL  "+  dependency.getRepository().getUrl());
					OBRGeneratorMojo.bundleDependencies.add(dependency.getArtifactId() + "/" + version);
					OBRGeneratorMojo.versionRange.put(dependency.getArtifactId(), range);
				}
			}
			
			// Debug
			String validDependencies = "Valid dependencies: " ;
			System.out.print("Valid dependencies: ");
			for (String dep : OBRGeneratorMojo.bundleDependencies) {
				validDependencies += " " + dep;
			}
			getLog().debug (validDependencies) ;

			List<ComponentDeclaration> components = Util.getComponents(root);
			ApamRepoBuilder arb = new ApamRepoBuilder(components, localRepository.getBasedir());
			StringBuffer obrContent = arb.writeOBRFile();
			if (CheckObr.getFailedChecking()) {
				// throw new MojoExecutionException("Inconsistent Metadata");
			}
			if (Util.getFailedParsing()) {
				throw new MojoExecutionException("Invalid xml Metadata syntax");
			}

			OutputStream obr;
			String obrFileStr = getProject().getBasedir().getAbsolutePath()
			+ File.separator + "src"
			+ File.separator + "main"
			+ File.separator + "resources"
			+ File.separator + "obr.xml";
			File obrFile = new File(obrFileStr);
			
			//maven ?? copies first in target/classes before to look in src/resources
			//and copies src/resources/obr.xml to  target/classes *after* obr modification
			//Thus we delete first target/classes/obr.xml to be sure the newly generated obr.xml file will be used
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
			//  System.err.println("Cannot open for writing : " + obrFile.getAbsolutePath());
		} catch (MalformedURLException e) {
			getLog().error(e.getMessage(), e);
		} catch (IOException e) {
			getLog().error(e.getMessage(), e);
		} catch (ParseException e) {
			getLog().error(e.getMessage(), e);
		}
		getLog().info(" obr.xml File generation - SUCCESS ");
	}
}
