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
import java.util.HashSet;
import java.util.List;
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

   


    /**
     * Execute method : this method launches the OBR generation.
     * 
     * @throws MojoExecutionException
     *             : an exception occurs during the OBR generation..
     * @see fr.imag.adele.obr.ipojo.plugin.OBRGeneratorMojo#execute()
     */
    public void execute() throws MojoExecutionException {
    	super.execute();
    	//OBR.xml generation
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
            getLog().info(" Parsing iPOJO metadata - SUCCESS ");
            Element root = ManifestMetadataParser
            .parseHeaderMetadata(ipojoMetadata);
            //            Element[] elements = root.getElements();

            for (Object artifact : getProject().getDependencyArtifacts()) {
                Artifact dependency = (Artifact) artifact;
                // 0.0.1.SNAPSHOT not 0.0.1-SNAPSHOT
                String version = dependency.getVersion().replace('-', '.');
                OBRGeneratorMojo.bundleDependencies.add(dependency.getArtifactId() + "/" + version);
            }
            // Debug
            System.out.print("Valid dependencies: ");
            for (String dep : OBRGeneratorMojo.bundleDependencies) {
                System.out.print(" " + dep);
            }
            System.out.println("");

            ApamRepoBuilder arb = new ApamRepoBuilder(localRepository.getBasedir());
            List<ComponentDeclaration> components = Util.getComponents(root);
            StringBuffer obrContent = arb.writeOBRFile(components);
            if (CheckObr.getFailedChecking()) {
                throw new MojoExecutionException("Inconsistent Metadata");
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
