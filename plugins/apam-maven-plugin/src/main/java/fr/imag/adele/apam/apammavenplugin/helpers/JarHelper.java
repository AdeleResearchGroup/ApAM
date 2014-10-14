package fr.imag.adele.apam.apammavenplugin.helpers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;

import fr.imag.adele.apam.apammavenplugin.InvalidApamMetadataException;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.encoding.Decoder;
import fr.imag.adele.apam.declarations.encoding.Reporter;
import fr.imag.adele.apam.declarations.encoding.Reporter.Severity;
import fr.imag.adele.apam.declarations.encoding.ipojo.MetadataParser;

public class JarHelper {

	private final Reporter 		reporter;

	private final File 			m_File;

	private final Manifest 		m_Manifest;

	public JarHelper(File file, Reporter reporter) throws InvalidApamMetadataException {

		/*
		 * Set logger and error handler
		 */
		this.reporter = reporter;

		/*
		 * Refence original file
		 */
		this.m_File 	= file;

		/*
		 * Verify file is a valid bundle (jar with manifest)
		 */

		Manifest loadedManifest  = null;
		try {

			if (file == null || !file.exists()) {
				throw new InvalidApamMetadataException("Jar File does not exists");
			}

			JarFile jarFile = getJarFile();
			loadedManifest 	= jarFile != null ? jarFile.getManifest() : null;
			jarFile.close();

		} catch (IOException e) {
			throw new InvalidApamMetadataException("Jar File invalid");
		}
		
		this.m_Manifest	= loadedManifest;

		if (m_Manifest == null) {
			throw new InvalidApamMetadataException("Manifest is Empty");
		}

	}
	
	public List<ComponentDeclaration> getApAMComponents() throws InvalidApamMetadataException {
		
		Element metadata = getiPojoMetadata();
		
		if( metadata == null) {
			return Collections.emptyList();
		}

		Decoder<Element> parser = new MetadataParser();
		
		/*
		 * parse all the declared  components
		 */
		List<SpecificationDeclaration> specifications 	= new ArrayList<SpecificationDeclaration>();
		List<ImplementationDeclaration> implementations = new ArrayList<ImplementationDeclaration>();
		List<CompositeDeclaration> composites 			= new ArrayList<CompositeDeclaration>();
		List<InstanceDeclaration> instances 			= new ArrayList<InstanceDeclaration>();
		
		
		for (Element element : metadata.getElements()) {
			ComponentDeclaration declaration = parser.decode(element,reporter);
			
			if (declaration == null)
				continue;
			
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
		 * add declared components in order of abstraction to ease cross-references
		 */
		List<ComponentDeclaration> components = new ArrayList<ComponentDeclaration>();
		components.addAll(specifications);
		components.addAll(implementations);
		components.addAll(composites);
		components.addAll(instances);
		
		String contains = "    contains components: ";
		for (ComponentDeclaration comp : components) {
			contains += comp.getName() + " ";
		}
		
		info(contains);

		return components;
	}

	public Element getiPojoMetadata() {
		Attributes iPOJOmetadata = m_Manifest.getMainAttributes();
		String ipojoMetadata = iPOJOmetadata.getValue("iPOJO-Components");

		iPOJOmetadata = null;
		if (ipojoMetadata == null) {
			return null;
		}

		info("Parsing Apam metadata for " + m_File + " - SUCCESS ");
		try {
			Element root = ManifestMetadataParser.parseHeaderMetadata(ipojoMetadata);
			return root;
		} catch (ParseException e) {
			error("Parsing manifest metadata for " + m_File + " - FAILED ",e);
		}
		
		return null;
	}

	public JarFile getJarFile() {
		try {
			return new JarFile(m_File);
		} catch (IOException e) {
			error("Error openinf metadata file for " + m_File,e);
			return null;
		}
	}

	public Manifest getManifest() {
		return m_Manifest;
	}

	private void error(String message, Throwable cause) {
		reporter.report(Severity.ERROR,message);
		for (StackTraceElement frame : cause.getStackTrace()) {
			reporter.report(Severity.ERROR, frame.toString());
		}
	}


	private void info(String message) {
		reporter.report(Severity.INFO, message);
	}
}
