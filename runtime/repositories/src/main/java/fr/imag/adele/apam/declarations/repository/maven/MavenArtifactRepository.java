package fr.imag.adele.apam.declarations.repository.maven;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.maven.artifact.Artifact;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.Reporter;
import fr.imag.adele.apam.declarations.Reporter.Severity;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.encoding.Decoder;
import fr.imag.adele.apam.declarations.encoding.ipojo.MetadataParser;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.Versioned;
import fr.imag.adele.apam.declarations.repository.ComponentIndex;
import fr.imag.adele.apam.declarations.repository.Repository;

/**
 * This class represents a component repository backed-up by the iPOJO metadata available in
 * an APAM component built by maven
 *
 * NOTE This repository is intended to be used at build time and we are supposing there are a
 * few components by bundle. Then, we keep permanently a list of components  in memory, and
 * it is never automatically reloaded, only manually using {@link #refresh()}
 *  
 * @author vega
 *
 */
public class MavenArtifactRepository implements Repository, Classpath.Entry {

	/**
	 * The reporter to signal errors and debug information
	 */
	private final Reporter reporter;
	
	/**
	 * The maven artifact that backs-up this repository
	 */
	private final Artifact artifact;
	
	/**
	 * The version of APAM that is being used
	 */
	private final String apamVersion;
	
	/**
	 * The list of components loaded from this artifact.
	 */
	private final List<ComponentDeclaration> components;
	
	/**
	 * The index of components by version
	 */
	private final ComponentIndex index;
	
	/**
	 * The list of classes in this bundle
	 */
	private final Set<String> classes;
	
	public MavenArtifactRepository(Artifact artifact, String apamVersion, Reporter reporter) throws IOException, ParseException {
		
		this.artifact		= artifact;
		this.apamVersion	= apamVersion;
		this.reporter 		= reporter;
		
		this.components		= new ArrayList<ComponentDeclaration>();
		this.index			= new ComponentIndex();
		this.classes		= new HashSet<String>();
		
		load();
	}
	
	/**
	 * The list of declared components of this artifact
	 */
	public List<ComponentDeclaration> getComponents() {
		return components;
	}
	
	/**
	 * Reloads the component information from the underlying artifact
	 */
	public void refresh() {
		try {
			load();
		} catch (Exception e) {
			error("Error reloading components from maven artifact "+artifact.getId(),e);
		}
	}
	
	@Override
	public <C extends ComponentDeclaration> C getComponent(ComponentReference<C> reference) {
		return index.getComponent(reference);
	}

	@Override
	public <C extends ComponentDeclaration> C getComponent(Versioned<C> reference) {
		return index.getComponent(reference);
	}

	@Override
	public boolean contains(String fullyQualifiedClassName) {
		return classes.contains(fullyQualifiedClassName);
	}

	/**
	 * The additional properties added to components to handle maven versionning
	 */
	public static final String PROPERTY_VERSION_APAM 			= "apam.version";
	public static final String PROPERTY_VERSION_MAVEN_GROUP 	= "maven.groupId";
	public static final String PROPERTY_VERSION_MAVEN_ARTIFACT 	= "maven.artifactId";
	public static final String PROPERTY_VERSION_MAVEN_VERSION	= "maven.version";
	
	/**
	 * Adds version information to a loaded component
	 */
	private ComponentDeclaration versioned(ComponentDeclaration component) {

		addProperty(component,PROPERTY_VERSION_APAM,"version",apamVersion.replace('-', '.'));

		addProperty(component,PROPERTY_VERSION_MAVEN_GROUP,"string",artifact.getGroupId());
		addProperty(component,PROPERTY_VERSION_MAVEN_ARTIFACT,"string",artifact.getArtifactId());
		addProperty(component,PROPERTY_VERSION_MAVEN_VERSION,"string",artifact.getVersion());
		
		addProperty(component,CST.VERSION,"version",artifact.getVersion().replace('-', '.'));
			
		return component;
	}
	
	
	/**
	 * Loads APAM components and class information from the target file associated to this maven artifact 
	 */
	private void load() throws IOException, ParseException {
		
		JarFile 	bundle 		= null;
		Manifest 	manifest	= null;
		
		try {
		
			if (artifact.getFile() == null || !artifact.getFile().exists() || !artifact.getFile().isFile()) {
				throw new IOException("Error loading jar file for maven artifact "+artifact.getId());
			}

			bundle 		= new JarFile(artifact.getFile());
			manifest	= bundle.getManifest();
			
			loadComponents(manifest);
			loadClasses(bundle);
		}
		finally {
			if (bundle != null)
				bundle.close();
		}
		
	}
	
	/**
	 * Loads APAM component metadata stored in the manifest of the bundle
	 */
	protected void loadComponents(Manifest manifest) throws ParseException {

		components.clear();
		index.clear();
		
		if (manifest == null)
			return;
		
		String componentHeader = manifest.getMainAttributes().getValue("iPOJO-Components");
		if (componentHeader == null) {
			return;
		}

		Element metadata = ManifestMetadataParser.parseHeaderMetadata(componentHeader);
		
		/*
		 * parse all the declared  components
		 */
		info("Parsing Apam metadata for " + artifact.getId()+ " from " + artifact.getFile());
		StringBuilder contents = new StringBuilder("    contains components: ");
		
		Decoder<Element> parser = new MetadataParser();
		
		List<SpecificationDeclaration> specifications 	= new ArrayList<SpecificationDeclaration>();
		List<ImplementationDeclaration> implementations = new ArrayList<ImplementationDeclaration>();
		List<CompositeDeclaration> composites 			= new ArrayList<CompositeDeclaration>();
		List<InstanceDeclaration> instances 			= new ArrayList<InstanceDeclaration>();
		
		
		for (Element element : metadata.getElements()) {
			ComponentDeclaration declaration = parser.decode(element,reporter);
			
			if (declaration == null)
				continue;
			
			contents.append(declaration.getName()).append(" ");
			
			if (declaration instanceof SpecificationDeclaration)
				specifications.add((SpecificationDeclaration)declaration);
			else if (declaration instanceof CompositeDeclaration)
				composites.add((CompositeDeclaration)declaration);
			else if (declaration instanceof ImplementationDeclaration)
				implementations.add((ImplementationDeclaration)declaration);
			else if (declaration instanceof InstanceDeclaration)
				instances.add((InstanceDeclaration)declaration);
			
		}

		/*
		 * add declared components in order of abstraction to ease cross-references when multiple interdependent
		 * components are declared in the same bundle
		 */
		components.addAll(specifications);
		components.addAll(implementations);
		components.addAll(composites);
		components.addAll(instances);
		
		/*
		 * index components
		 */
 		for (ComponentDeclaration component : components) {
			index.put(versioned(component));
		}
		
		info(contents.toString());
	}

	/**
	 * Load the list of classes available in this bundle
	 * 
	 * TODO We should consider classes visibility as specified by the Export-Packages in the bundle manifest
	 */
	protected void loadClasses(JarFile bundle) {
		
		classes.clear();
		
		Enumeration<JarEntry> entries = bundle.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			
			if (entry.isDirectory())
				continue;
			
			if (!entry.getName().endsWith(".class"))
				continue;
			
			String className = entry.getName().substring(0,entry.getName().lastIndexOf(".class")).replace('/','.').replace('\\','.').replace('$','.');
			classes.add(className);
		}
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
