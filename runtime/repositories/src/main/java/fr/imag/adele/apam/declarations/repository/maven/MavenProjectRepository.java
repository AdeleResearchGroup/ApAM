package fr.imag.adele.apam.declarations.repository.maven;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.ipojo.parser.ParseException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import fr.imag.adele.apam.declarations.repository.Repository;
import fr.imag.adele.apam.declarations.repository.RepositoryChain;
import fr.imag.adele.apam.declarations.tools.Reporter;
import fr.imag.adele.apam.declarations.tools.Reporter.Severity;

/**
 * This class represents a component repository backed-up by the iPOJO metadata available in
 * an APAM component built in a maven project, including optionally its dependencies
 *
 * NOTE This repository is intended to be used at build time, it delegates to a list of
 * repositories of class {@link MavenArtifactRepository}. This means that the components
 * are kept permanently in memory. We do not expect a big number of components in the
 * dependencies of the project.
 * 
 * @author vega
 *
 */
public class MavenProjectRepository extends RepositoryChain implements Repository {

	/**
	 * The reporter to signal errors and debug information
	 */
	private final Reporter reporter;

	/**
	 * The repository corresponding to the components built in the project
	 */
	private final MavenArtifactRepository buildRepository;

	/**
	 * The class path of the project
	 */
	private final Classpath classpath;
	
	public MavenProjectRepository(MavenProject project, boolean includeDependencies, String apamVersion, Reporter reporter) throws IOException, ParseException {
		
		this.reporter 			= reporter;
		this.classpath			= new Classpath();

		/*
		 * load the main artifact
		 */
		this.buildRepository 	= new MavenArtifactRepository(project.getArtifact(),apamVersion,reporter);
		
		classpath.add(buildRepository);
		addRepository(buildRepository);

		/*
		 * Get all COMPILE scope dependencies transitively
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
			if (dependency instanceof Artifact) {
				Artifact artifact = (Artifact) dependency;
				if ("system".equalsIgnoreCase(artifact.getScope())) {
					requiredArtifacts.add((Artifact) artifact);
				}
			}				
		}

		/*
		 * load the required artifacts
		 */
		
        for (Artifact requiredArtifact : requiredArtifacts) {

        	MavenArtifactRepository requiredRepository = new MavenArtifactRepository(requiredArtifact,apamVersion,reporter);
        	
        	classpath.add(requiredRepository);
        	if (includeDependencies && !requiredRepository.getComponents().isEmpty()) {
        		addRepository(requiredRepository);
        	}
        	
        }
	}

	/**
	 * The list of components that are being build on this project
	 */
	public MavenArtifactRepository getBuildRepository() {
		return buildRepository;
	}

	/**
	 * The class path of the project
	 */
	public Classpath getClasspath() {
		return classpath;
	}
	
	/**
	 * Utility functions to report errors and debug infor
	 * 
	 */
	public void error(String message, Throwable cause) {
		error(message);
		for (StackTraceElement frame : cause.getStackTrace()) {
			error(frame.toString());
		}
	}

	public final void error(String message) {
		reporter.report(Severity.ERROR, message);
	}
	
	public final void warning(String message) {
		reporter.report(Severity.WARNING, message);
	}

	public final void info(String message) {
		reporter.report(Severity.INFO, message);
	}

	
}
