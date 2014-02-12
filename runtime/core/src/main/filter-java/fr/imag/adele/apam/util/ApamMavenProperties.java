package fr.imag.adele.apam.util;

/**
 * Helper class to inject maven properties in java (using resource filtering)
 * @author thibaud
 *
 */
public class ApamMavenProperties {
	public static final String mavenGroupId="${project.groupId}";
	public static final String mavenArtifactId="${project.artifactId}";
	public static final String mavenVersion="${project.version}";
}
