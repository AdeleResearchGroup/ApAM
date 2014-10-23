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
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulator.render.MetadataRenderer;
import org.apache.felix.ipojo.manipulator.store.JarFileResourceStore;
import org.apache.felix.ipojo.manipulator.store.builder.DefaultManifestBuilder;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.plugin.ManipulatorMojo;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.apammavenplugin.helpers.EnrichElementsHelper;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.Reporter;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.repository.acr.ApamComponentRepository;
import fr.imag.adele.apam.declarations.repository.maven.MavenProjectRepository;
import fr.imag.adele.apam.util.ApamMavenProperties;

/**
 * Packages an OSGi jar "iPOJO bundle" as an "APAM bundle".
 * 
 * @version $Rev$, $Date$
 * @extendsPlugin maven-ipojo-plugin
 * @goal apam-bundle
 * @extendsGoal ipojo-bundle
 * @requiresrelationResolution runtime
 * @description manipulate an OSGi bundle jar to include the obr.xml file and
 *              build APAM bundle
 * 
 * @author ApAM Team
 */
public class OBRGeneratorMojo extends ManipulatorMojo implements Reporter {

    /**
     * ACR Repository (ApAM Component Repository)
     * used as input (read only the existing component)
     * @parameter
     */
    private String[] inputAcr;


    /**
     * ACR Repository (ApAM Component Repository)
     * used as output (write the current component)
     * @parameter property="outputAcr"
     */
    private String outputAcr;


    public static final String NONE = "NONE";



    /**
     * mojo boolean property includeMavenDependencies
     * = true means that initial external ApamCapabilities are built from dependencies in the pom (default)
     * = false means that all apam-component external dependencies will only be resolved using the inputAcr
     *
     * @parameter property="includeMavenDependencies" default-value="true"
     */
    private Boolean includeMavenDependencies;



	/**
	 * The Maven project.
	 * 
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

    /**
     * Local Repository.
     *
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

	
	/**
	 * @parameter default-value="${basedir}
	 */
	private File baseDirectory;

	private boolean parsingFailed = false;


    public static final String DEFAULT_OBR_XML = "repository.xml";


    /**
	 * Execute method : this method launches the OBR generation.
	 * 
	 * @throws MojoExecutionException
	 *             : an exception occurs during the OBR generation..
	 * 
	 */
	public void execute() throws MojoExecutionException {

        if(inputAcr ==null || inputAcr.length<1) {
            inputAcr = new String[1];

            if(ACRInstallMojo.getTargetACR(outputAcr) != null) {
                getLog().info("No inputAcr repository URL specified, first fallback, trying to use the target output ACR");
                inputAcr[0] = ACRInstallMojo.getTargetACR(outputAcr).toString();

            } else {
                getLog().info("No inputAcr repository URL specified, using default local maven repo (obr)");
                inputAcr[0] = new String(localRepository.getUrl()+ DEFAULT_OBR_XML);
            }

        }
        List<URL> tab_acr=new ArrayList<URL>();
        for(int i=0;i< inputAcr.length;i++) {
            try {
                if(inputAcr[i] != null && !NONE.equals(inputAcr[i]) && ACRInstallMojo.getTargetACR(inputAcr[i]).toURL() != null) {
                    getLog().info("input ACR : " + inputAcr[i]+" successfully added");

                    tab_acr.add(ACRInstallMojo.getTargetACR(inputAcr[i]).toURL());
                } else {
                    getLog().info("input ACR : " + inputAcr[i]+" is not a valid URL, not added");
                }
            } catch(MalformedURLException exc) {
                getLog().info("input ACR : " + inputAcr[i]+" is not a valid URL, not added");
            }
        }


        try {

			super.execute();

			MavenProjectRepository projectRepository = new MavenProjectRepository(project, includeMavenDependencies, ApamMavenProperties.mavenVersion, this);

			getLog().info("ApAM metadata manipulator");

			
			// The jar to compile
			if (projectRepository.getComponents().isEmpty()) {
				throw new InvalidApamMetadataException("No Apam metadata");
			}

    		ApamCapabilityBroker broker = null;
    		
            try {
                ApamComponentRepository acr = new ApamComponentRepository(tab_acr.toArray(new URL[0]), this);
                broker = new ApamCapabilityBroker(projectRepository,acr);
            } catch (Exception exc) {
                exc.printStackTrace();
                throw new MojoExecutionException("Exception during initialize of OBR/ACR repositories "+exc.getMessage());
            }
            
			ApamRepoBuilder builder = new ApamRepoBuilder(broker,projectRepository.getClasspath(),projectRepository.getComponents(), project.getArtifact(), getLog());
			StringBuffer obrContent = builder.writeOBRFile();
			if (builder.hasFailedChecking()) {
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

			updateJarFile();

		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
			report(Severity.ERROR, e.getMessage());
			throw new MojoExecutionException(e.getMessage());
		}

		getLog().info(" obr.xml File generation - SUCCESS ");
	}

	@Override
	public void report(Severity severity, String message) {
		switch (severity) {
		case ERROR : 
			getLog().error("error parsing component declaration : " + message);
			parsingFailed = true;
			break;
		case WARNING : 
		case SUSPECT :
			getLog().warn("warning parsing component declaration : " + message);
			break;
		case INFO :
			getLog().info(message);
			break;
		}
	}

	public void updateJarFile() throws MojoExecutionException {
			
		File newOutput = new File(baseDirectory.getAbsolutePath()+ File.separator + "target" + File.separator + "_temp.jar");
		if (newOutput.exists()) {
			newOutput.delete();
		}

		JarFile bundle				= null; 
		JarFileResourceStore store 	= null;
		
		try {
		
			Artifact artifact 	= project.getArtifact();
			if (artifact.getFile() == null || !artifact.getFile().exists() || !artifact.getFile().isFile()) {
				throw new IOException("Error loading jar file for maven artifact "+artifact.getId());
			}

			bundle	= new JarFile(artifact.getFile());
			store 	= new JarFileResourceStore(bundle,newOutput);
			
			Manifest manifest = bundle.getManifest();
			String componentHeader = manifest.getMainAttributes().getValue("iPOJO-Components");
			if (componentHeader == null) {
				return;
			}

			Element metadata = ManifestMetadataParser.parseHeaderMetadata(componentHeader);
			store.setManifest(bundle.getManifest());

			
			ComponentDeclaration template = getVersionedComponentTemplate(artifact); 
			EnrichElementsHelper.addPropertiesToChildrenApAMComponents(metadata, template.getPropertyDefinitions(), template.getProperties());

			DefaultManifestBuilder builder = new DefaultManifestBuilder();
			builder.setMetadataRenderer(new MetadataRenderer());
			
			builder.addMetada(Arrays.asList(metadata.getElements()));
			
			store.setManifestBuilder(builder);

		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
			report(Severity.ERROR, e.getMessage());
			throw new MojoExecutionException(e.getMessage());
		}
		finally {
			try {
				if (store != null)
					store.close();
			}
			catch(Exception ignored) {
			}
			try {
				if (bundle != null)
					bundle.close();
			}
			catch(Exception ignored) {
			}
			
		}

		project.getArtifact().getFile().delete();
		newOutput.renameTo(project.getArtifact().getFile());
		
	}

	
	public static final String PROPERTY_VERSION_APAM 			= "apam.version";
	public static final String PROPERTY_VERSION_MAVEN_GROUP 	= "maven.groupId";
	public static final String PROPERTY_VERSION_MAVEN_ARTIFACT 	= "maven.artifactId";
	public static final String PROPERTY_VERSION_MAVEN_VERSION	= "maven.version";
	
	private static final ComponentDeclaration getVersionedComponentTemplate(Artifact artifact) {
		
		SpecificationDeclaration template = new SpecificationDeclaration("template");
		
		addProperty(template,PROPERTY_VERSION_APAM,"version",ApamMavenProperties.mavenVersion.replace('-', '.'));

		addProperty(template,PROPERTY_VERSION_MAVEN_GROUP,"string",artifact.getGroupId());
		addProperty(template,PROPERTY_VERSION_MAVEN_ARTIFACT,"string",artifact.getArtifactId());
		addProperty(template,PROPERTY_VERSION_MAVEN_VERSION,"string",artifact.getVersion());
		
		addProperty(template,CST.VERSION,"version",artifact.getVersion().replace('-', '.'));
		
		return template;
	}
	
	/**
	 * Add a property to an existing component
	 * 
	 * NOTE We may be modifying a component that has already version information attached (either because the
	 * component has already been built, and we are loading it as a dependency, or because the user has added
	 * the information manually) so we need to be careful not to override it
	 * 
	 */
	private static final void addProperty(ComponentDeclaration component, String property, String type, String value) {
		
		/*
		 */
		PropertyDefinition defintition = component.getPropertyDefinition(property);
		if (defintition == null) {
			defintition = new PropertyDefinition(component, property, type, null, null, null, null);
			component.getPropertyDefinitions().add(defintition);
		}

		String currentValue = component.getProperty(property);
		if (currentValue == null) {
			component.getProperties().put(property, value);
		}
	}
	
}
