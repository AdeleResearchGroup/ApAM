package fr.imag.adele.apam.apam2MavenPlugIn;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

//import fr.imag.adele.obrMan.OBRMan;

///**
// * Packages an OSGi jar "bundle" as an "iPOJO bundle".
// * 
// * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
// * @version $Rev$, $Date$
// * @goal ipojo-bundle
// * @phase package
// * @requiresDependencyResolution runtime
// * @description manipulate an OSGi bundle jar to build an iPOJO bundle
// */

/**
 * This plugin does the same as ipojo plugin and adds Apam information to the OBR file associated with an Apam bundle.
 * 
 * @version $Rev$, $Date$
 * @goal apam-ipojo-bundle
 * @phase package
 * @requiresDependencyResolution runtime
 * @description manipulate an OSGi bundle jar to build an Apam-iPOJO bundle
 */

public class Apam2MavenPlugin extends AbstractMojo {

    /**
     * The directory for the generated JAR.
     * 
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private String             m_buildDirectory;

    /**
     * The directory containing generated classes.
     * 
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File               m_outputDirectory;

    /**
     * Location of the metadata file or iPOJO metadata configuration.
     * 
     * @parameter alias="metadata"
     */
    private String             m_metadata;

    /**
     * If set, the manipulated jar will be attached to the project as a separate artifact.
     * 
     * @parameter alias="classifier" expression="${ipojo.classifier}"
     */
    private String             m_classifier;

    /**
     * The Maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject       m_project;

    /**
     * Used for attaching new artifacts.
     * 
     * @component
     * @required
     */
    private MavenProjectHelper m_helper;

    /**
     * Project types which this plugin supports.
     * 
     * @parameter
     */
    private final List         m_supportedProjectTypes = Arrays.asList(new String[] { "bundle", "jar", "war" });

    /**
     * Ignore annotations parameter.
     * 
     * @parameter alias="ignoreAnnotations" default-value="false"
     */
    private boolean            m_ignoreAnnotations;

    /**
     * Ignore embedded XSD parameter.
     * 
     * @parameter alias="IgnoreEmbeddedSchemas" default-value="false"
     */
    private boolean            m_ignoreEmbeddedXSD;

    protected MavenProject getProject() {
        return m_project;
    }

    private boolean isXML() {
        return (m_metadata != null) && (m_metadata.indexOf('<') > -1);
    }

    /**
     * Local Repository.
     * 
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    protected ArtifactRepository localRepository;

    /**
     * Execute method : this method launches the pojoization.
     * 
     * @throws MojoExecutionException : an exception occurs during the manipulation.
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException {

        System.err.println("OBR repo : " + localRepository.getBasedir());

        // ignore project types not supported, useful when the plugin is configured in the parent pom
        if (!m_supportedProjectTypes.contains(getProject().getArtifact().getType())) {
            getLog().debug(
                    "Ignoring project " + getProject().getArtifact() + " : type "
                            + getProject().getArtifact().getType()
                            + " is not supported by iPOJO plugin, supported types are " + m_supportedProjectTypes);
            return;
        }

        String obrFileStr = m_project.getBasedir().getAbsolutePath()
                + File.separator + "src"
                + File.separator + "main"
                + File.separator + "resources"
                + File.separator + "obr.xml";
        // System.out.println("obr.xml file : " + obrFileStr);
        // System.out.println("buildDirectory : " + m_buildDirectory);
        // System.out.println("iPOJOMetadata : " + m_metadata);

        initializeSaxDriver();
        getLog().info("Start Apam-iPOJO bundle manipulation");

        // Get metadata
        // Check if metadata are contained in the configuration
        File metadata = null; // Metadata File or directory containing the metadata files.
        InputStream is = null; // Use if contained in the configuration

        if (isXML()) {
            is = new ByteArrayInputStream(m_metadata.getBytes());
        } else {
            // If the metadata is not set,
            // first check if ./src/main/ipojo exists, if so look into it.
            if (m_metadata == null) {
                File m = new File(m_project.getBasedir(), "src/main/ipojo");
                if (m.isDirectory()) {
                    metadata = m;
                    getLog().info("Metadata directory : " + metadata.getAbsolutePath());
                } else {
                    // Else check target/classes/metadata.xml
                    File meta = new File(m_outputDirectory + File.separator + "metadata.xml");
                    if (!meta.exists()) {
                        // If it still does not exist, try ./metadata.xml
                        meta = new File(m_project.getBasedir() + File.separator + "metadata.xml");
                    }

                    if (meta.exists()) {
                        metadata = meta;
                        getLog().info("Apam-iPOJO Metadata file : " + metadata.getAbsolutePath());
                    }

                    // No metadata.
                }
            } else {
                // metadata path set.
                File m = new File(m_project.getBasedir(), m_metadata);
                if (!m.exists()) {
                    throw new MojoExecutionException("The Apam-iPOJO metadata file does not exist : "
                            + m.getAbsolutePath());
                }
                metadata = m;
                if (m.isDirectory()) {
                    getLog().info("Apam-iPOJO Metadata directory : " + metadata.getAbsolutePath());
                } else {
                    getLog().info("Apam-iPOJO Metadata file : " + metadata.getAbsolutePath());
                }
            }
            // System.out.println("metadata : " + metadata);
            if (metadata == null) {
                // Verify if annotations are ignored
                if (m_ignoreAnnotations) {
                    getLog().info("No Apam-iPOJO metadata file found - ignoring annotations");
                    return;
                } else {
                    getLog().info("No Apam-iPOJO metadata file found - trying to use only annotations");
                }
            }
        }

        // Get input bundle, we use the already create artifact.
        File in = m_project.getArtifact().getFile();
        getLog().info("Input Bundle File : " + in.getAbsolutePath());
        if (!in.exists()) {
            throw new MojoExecutionException("The specified bundle file does not exist : " + in.getAbsolutePath());
        }

        File out = new File(m_buildDirectory + File.separator + "_out.jar");
        Apam2RepoBuilder arb = new Apam2RepoBuilder(localRepository.getBasedir());
        Pojoization pojo = new Pojoization();
        if (m_ignoreAnnotations) {
            pojo.disableAnnotationProcessing();
        }
        if (!m_ignoreEmbeddedXSD) {
            pojo.setUseLocalXSD();
        }

        // Executes the pojoization.
        if (is == null) {
            if (metadata == null) { // No metadata.
                pojo.pojoization(in, out, (File) null); // Only annotations. Pure iPOJO.
            } else { // Apam components must have a metadata file
                arb.writeOBRFile(obrFileStr, metadata, null, in, m_outputDirectory);
                pojo.pojoization(in, out, metadata); // Metadata set
            }
        } else { // In-Pom metadata.
            arb.writeOBRFile(obrFileStr, null, is, in, m_outputDirectory);
            pojo.pojoization(in, out, is);
        }

        for (int i = 0; i < pojo.getWarnings().size(); i++) {
            getLog().warn((String) pojo.getWarnings().get(i));
        }
        if (pojo.getErrors().size() > 0) {
            throw new MojoExecutionException((String) pojo.getErrors().get(0));
        }

        if (m_classifier != null) {
            // The user want to attach the resulting jar
            // Do not delete in File
            m_helper.attachArtifact(m_project, "jar", m_classifier, out);
        } else {
            // Usual behavior
            if (in.delete()) {
                if (!out.renameTo(in)) {
                    getLog().warn("Cannot rename the manipulated jar file");
                }
            } else {
                getLog().warn("Cannot delete the input jar file");
            }
        }
        getLog().info("APAM-iPOJO Bundle manipulation - SUCCESS");
    }

    /**
     * If Maven runs with Java 1.4, we should use the Maven Xerces. To achieve that, we set the org.xml.sax.driver
     * property. Otherwise, the JVM sets the org.xml.sax.driver property.
     */
    private void initializeSaxDriver() {
        String version = System.getProperty("java.vm.version");
        if (version.startsWith("1.4")) {
            getLog().info("Set the Sax driver to org.apache.xerces.parsers.SAXParser");
            System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");
        }

    }

}