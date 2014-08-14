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

import org.apache.felix.obrplugin.Config;
import org.apache.felix.obrplugin.ObrUpdate;
import org.apache.felix.obrplugin.ObrUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Install Component Description inside a particular ACR (ApAM Component Repository)
 * (life-cycle goal)
 * This is quite the same as for OBRInstall with maven-bundle repository
 * (which is a final class with private methods sadly, so there's a lot of cut'n paste)
 * ,but does not install in local repository (already done by the real OBRInstall)
 *
 * @goal install
 * @phase package
 */
public final class ACRInstallMojo extends AbstractMojo {


    /**
     * ACR Repository (ApAM Component Repository)
     *
     * @parameter property="targetACR"
     */
    private String targetACR;

    /**
     * Project types which this plugin supports.
     *
     * @parameter
     */
    private List supportedProjectTypes = Arrays.asList(new String[]
            {"jar", "bundle"});

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
     * Attached source artifact
     */
    private Artifact m_sourceArtifact;

    /**
     * Attached doc artifact
     */
    private Artifact m_docArtifact;

    /**
     * Remote repository id, used to lookup authentication settings.
     */
    private String repositoryId;

    private static final String DOT_XML = ".xml";
    public static final String REPO_XML = "acr_repository.xml";

    /**
     * @param repository path to specific acr_repository.xml
     * @return URI pointing to correct acr_repository.xml or null
     */
    public static URI getTargetACR(String repository) {
        String targetPath = repository;

        Pattern ignoredNames = Pattern.compile("^(true|false|none|null)?$", Pattern.CASE_INSENSITIVE);

        // Combine location settings into a single repository location
        if (null == targetPath
                || ignoredNames.matcher(targetPath).matches()) {
            return null;
        } else if (!targetPath.toLowerCase().endsWith(DOT_XML)) {
            targetPath = targetPath + '/' + REPO_XML;
        }

        URI uri;
        try {
            uri = new URI(targetPath);
            uri.toURL(); // check protocol
        } catch (Exception e) {
            uri = null;
        }

        // fall-back to file-system approach
        if (null == uri || !uri.isAbsolute()) {
            try {
                uri = new File(targetPath).getCanonicalFile().toURI();
            }catch(IOException exc) {
                uri=null;
            }
        }
        return uri;
    }


    public void execute() throws MojoExecutionException {
        getLog().info("execute(), targetACR : " + targetACR);
        URI repoXML = getTargetACR(targetACR);
        repositoryId = localRepository.getId();

        if (repoXML != null) {
            String projectType = project.getPackaging();

            // ignore unsupported project types, useful when bundleplugin is configured in parent pom
            if (!supportedProjectTypes.contains(projectType)) {
                getLog().warn(
                        "Ignoring project type " + projectType + " - supportedProjectTypes = " + supportedProjectTypes);
                return;
            }

            String repoPath = targetACR;
            if (repoPath.toLowerCase().endsWith(DOT_XML)) {
                repoPath = repoPath.substring(0, repoPath.lastIndexOf('/'));
            }

            // check for any attached sources or docs
            Log log = getLog();
            ObrUpdate update;

            try {

                URI acrXmlFile = ObrUtils.findObrXml(project);

                Config userConfig = new Config();

                update = new ObrUpdate(repoXML, acrXmlFile, project, repoPath, userConfig, log);
                update.parseRepositoryXml();

                updateLocalBundleMetadata(project.getArtifact(), update);

                update.writeRepositoryXml();
            } catch (Exception e) {
                log.warn("Exception while updating ACR : " + e.getLocalizedMessage(), e);
            }


        } else {
            getLog().info("Local ACR update disabled or incorrect (enable with -DtargetACR)," +
                    " targetACR = " + targetACR);
            return;
        }

    }

    private void updateLocalBundleMetadata(Artifact artifact, ObrUpdate update) throws MojoExecutionException {
        if (!supportedProjectTypes.contains(artifact.getType())) {
            return;
        } else if (null == artifact.getFile() || artifact.getFile().isDirectory()) {
            getLog().error("No artifact found, try \"mvn install apam:install\"");
            return;
        }

        URI bundleJar = ObrUtils.getArtifactURI(localRepository, artifact);

        URI sourceJar = null;
        if (null != m_sourceArtifact) {
            sourceJar = ObrUtils.getArtifactURI(localRepository, m_sourceArtifact);
        }

        URI docJar = null;
        if (null != m_docArtifact) {
            docJar = ObrUtils.getArtifactURI(localRepository, m_docArtifact);
        }

        update.updateRepository(bundleJar, sourceJar, docJar);
    }


}
