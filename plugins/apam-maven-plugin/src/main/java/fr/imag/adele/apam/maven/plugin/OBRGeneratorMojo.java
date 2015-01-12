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
package fr.imag.adele.apam.maven.plugin;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.ipojo.manipulator.render.MetadataRenderer;
import org.apache.felix.ipojo.manipulator.store.JarFileResourceStore;
import org.apache.felix.ipojo.manipulator.store.builder.DefaultManifestBuilder;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.plugin.ManipulatorMojo;
import org.apache.felix.utils.log.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceListener;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.repository.RepositoryChain;
import fr.imag.adele.apam.declarations.repository.acr.ApamComponentRepository;
import fr.imag.adele.apam.declarations.repository.maven.MavenProjectRepository;
import fr.imag.adele.apam.declarations.tools.Reporter;
import fr.imag.adele.apam.maven.plugin.helpers.EnrichElementsHelper;
import fr.imag.adele.apam.maven.plugin.validation.ValidationContext;
import fr.imag.adele.apam.maven.plugin.validation.Validator;
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
public class OBRGeneratorMojo extends ManipulatorMojo  {

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

    private static final String NONE = "NONE";
    
    /**
     * Local Repository.
     *
     * @parameter default-value="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    private static final String DEFAULT_OBR_XML = "repository.xml";


    private List<URL> getInputRepositoryLocations() {
    	
        List<URL> acrLocations	= new ArrayList<URL>();

        if (inputAcr == null || inputAcr.length == 0) {
            
        	URL location			= null;
        	
            if (outputAcr != null) {
                getLog().info("No inputAcr repository URL specified, first fallback, trying to use the target output ACR");
                location = getURL(outputAcr);

            } 
            
            if (location != null)
            	return Collections.singletonList(location);
            
            getLog().info("No inputAcr repository URL specified, using default local maven repository (obr) at "+localRepository.getUrl());
        	location = getURL(localRepository.getUrl()+DEFAULT_OBR_XML);

            if (location != null)
            	return Collections.singletonList(location);
        	
            return null;
        }
        else {
        	for (String input : inputAcr) {
            	URL location = (input != null || !NONE.equals(input)) ? getURL(input) : null;
               	if (location != null)
               		acrLocations.add(location);
			}
        }
        
        return acrLocations;
    }


    private URL getURL(String location) {
    	
    	if (location == null)
    		return null;
    	
        try {

        	URI locationURI = ACRInstallMojo.getTargetACR(location);
        	if (locationURI == null) {
    		    getLog().info("Invalid repository URL specified : " + location +", will be ignored");
    		    return null;
        	}
        	
        	return locationURI.toURL();
		} 
        catch(MalformedURLException exc) {
		    getLog().info("Invalid repository URL specified : " + location +", will be ignored");
		    return null;
		}
    }
    
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
	 * @parameter default-value="${basedir}
	 */
	private File baseDirectory;




    /**
     * This utility class capture messages and errors during processing and show them in the
     * build log
     * 
     * TODO Built a better structured error report
     * 
     * @author vega
     *
     */
    private static class ErrorReport implements Reporter {
    	
    	private final Log log;
    	private boolean hasErrors = false;
    	
    	public ErrorReport(Log log) {
    		this.log 		= log;
    		this.hasErrors	= false;
    	}
    	
    	public boolean hasErrors() {
    		return hasErrors;
    	}
    	
    	@Override
    	public void report(Severity severity, String message) {
    		switch (severity) {
    		case ERROR : 
    			log.error(message);
    			hasErrors = true;
    			break;
    		case WARNING : 
    		case SUSPECT :
    			log.warn(message);
    			break;
    		case INFO :
    			log.info(message);
    			break;
    		}
    	}
    	
    }
    
    /**
	 * Execute method : this method launches the OBR generation.
	 * 
	 * @throws MojoExecutionException
	 *             : an exception occurs during the OBR generation..
	 * 
	 */
	public void execute() throws MojoExecutionException {

        try {

        	/*
        	 * Perform iPOJO manipulation 
        	 */
			super.execute();

			/*
			 * Loads and parses components from the project and its dependencies
			 */
			
			ErrorReport parsingResult = new ErrorReport(getLog());
			MavenProjectRepository projectRepository = new MavenProjectRepository(project, includeMavenDependencies, ApamMavenProperties.mavenVersion, parsingResult);

			getLog().info("ApAM metadata manipulator");
			
			/*
			 * The components to verify
			 */
			
			List<ComponentDeclaration> components = projectRepository.getBuildRepository().getComponents();
			if (components.isEmpty()) {
				throw new InvalidApamMetadataException("No Apam metadata");
			}

			ErrorReport acrParsingResult = new ErrorReport(getLog());
    		ApamComponentRepository acr = null;
            try {
                acr = new ApamComponentRepository(mockManager(getInputRepositoryLocations()),getInputRepositoryLocations(),acrParsingResult);
            } catch (Exception exc) {
                exc.printStackTrace();
                throw new MojoExecutionException("Exception during initialize of OBR/ACR repositories "+exc.getMessage());
            }
            
            /*
             * Validate components, we validate first the most abstract components so that if there are cross-references
             * among components in the same build we detect errors soon and avoid cascaded errors 
             */

            ValidationContext context	= new ValidationContext(new RepositoryChain(projectRepository,acr));
    		Validator validator			= new Validator(projectRepository.getClasspath(),context);
    		
            ErrorReport validatorResult = new ErrorReport(getLog());
            
			for (ComponentDeclaration component : components) {
				if (component instanceof SpecificationDeclaration) {
					validator.validate(component, validatorResult);
				}
			}

			for (ComponentDeclaration component : components) {
				if (component instanceof AtomicImplementationDeclaration) {
					validator.validate(component, validatorResult);
				}
			}

			for (ComponentDeclaration component : components) {
				if (component instanceof CompositeDeclaration) {
					validator.validate(component, validatorResult);
				}
			}

			for (ComponentDeclaration component : components) {
				if (component instanceof InstanceDeclaration) {
					validator.validate(component, validatorResult);
				}
			}

			/*
			 * Abort if there are errors
			 */
			if (parsingResult.hasErrors()) {
				throw new MojoFailureException("Invalid xml Apam Metadata syntax.");
			}
			
			if (validatorResult.hasErrors()) {
				throw new MojoFailureException("Invalid Apam component declaration");
			}
			
			/*
			 * Generate the OBR metadata corresponding to this project
			 */

			ApamComponentRepositoryBuilder builder	= new ApamComponentRepositoryBuilder(acr);

            ErrorReport generatorResult = new ErrorReport(getLog());
            String obrProjectContent	= builder.build(context,projectRepository,generatorResult);

			if (generatorResult.hasErrors()) {
				throw new MojoFailureException("Error generating ACR metadata");
			}

			/*
			 * Modify the OBR file that will be merged by the felix maven plugin at install
			 * time to modify the output repository
			 * 
			 * TODO We need to be sure whether the file used at install is in src or target
			 * directory
			 */
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
			obr.write(obrProjectContent.getBytes());
			obr.flush();

			// Map<String, Element> map =
			// ObrAdditionalProperties.parseFile(obrFile,getLog());
			// System.err.println("Obr file : " + obrFile.getAbsolutePath());
			obr.close();

			updateJarFile();

		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
			throw new MojoExecutionException(e.getMessage());
		}

		getLog().info(" obr.xml File generation - SUCCESS ");
	}

	   /**
     * Mock some of the OSGi context to allow using the repository at build time
     */

    private static RepositoryAdmin mockManager(List<URL> repositories) throws Exception {
    	
    	if (repositories == null || repositories.isEmpty())
    		return null;
    	
        BundleContext bundleContext = mock(BundleContext.class);
        Bundle systemBundle = mock(Bundle.class);

        // TODO: Change this one
        when(bundleContext.getProperty(RepositoryAdminImpl.REPOSITORY_URL_PROP)).thenReturn(repositories.get(0).toExternalForm());

        when(bundleContext.getProperty(anyString())).thenReturn(null);
        when(bundleContext.getBundle(0)).thenReturn(systemBundle);
        when(systemBundle.getHeaders()).thenReturn(new Hashtable<String,String>());
        when(systemBundle.getRegisteredServices()).thenReturn(null);
        when(new Long(systemBundle.getBundleId())).thenReturn(new Long(0));
        when(systemBundle.getBundleContext()).thenReturn(bundleContext);
        bundleContext.addBundleListener((BundleListener) anyObject());
        bundleContext.addServiceListener((ServiceListener) anyObject());
        when(bundleContext.getBundles()).thenReturn(new Bundle[]{systemBundle});

        RepositoryAdminImpl repoAdmin = new RepositoryAdminImpl(bundleContext, new Logger(bundleContext));

        // force initialization && remove all initial repositories
        org.apache.felix.bundlerepository.Repository[] repos = repoAdmin.listRepositories();
        for (int i = 0; repos != null && i < repos.length; i++) {
            repoAdmin.removeRepository(repos[i].getURI());
        }

        return repoAdmin;
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
			defintition = new PropertyDefinition(component.getReference(), property, type, null);
			component.getPropertyDefinitions().add(defintition);
		}

		component.getProperties().put(property, value);
	}
	
}
