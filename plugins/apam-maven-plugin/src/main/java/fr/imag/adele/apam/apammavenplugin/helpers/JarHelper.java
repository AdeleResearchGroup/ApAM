package fr.imag.adele.apam.apammavenplugin.helpers;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.maven.plugin.logging.Log;

import fr.imag.adele.apam.apammavenplugin.InvalidApamMetadataException;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.util.CoreMetadataParser;
import fr.imag.adele.apam.util.CoreParser;
import fr.imag.adele.apam.util.CoreParser.ErrorHandler;
import fr.imag.adele.apam.util.CoreParser.ErrorHandler.Severity;

public class JarHelper {

	private final Log 			logger;
	private final ErrorHandler 	m_ErrorHandler;

	private final File 			m_File;

	private final Manifest 		m_Manifest;

	public JarHelper(File file, ErrorHandler handler, Log logger) throws InvalidApamMetadataException {

		/*
		 * Set logger and error handler
		 */
		this.logger = logger;

		if (handler == null) {
			throw new InvalidApamMetadataException("No ErrorHandlerProvided");
		} else {
			m_ErrorHandler = handler;
		}

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

		CoreParser parser = new CoreMetadataParser(metadata, null);
		List<ComponentDeclaration> ret = parser.getDeclarations(m_ErrorHandler);

		String contains = "    contains components: ";
		for (ComponentDeclaration comp : ret) {
			contains += comp.getName() + " ";
		}
		
		info(contains);

		return ret;
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
			error(e);
			return null;
		}
	}

	public Manifest getManifest() {
		return m_Manifest;
	}

	private void error(Throwable cause) {
		error(cause.getMessage(),cause);
	}

	private void error(String message, Throwable cause) {
		m_ErrorHandler.error(Severity.ERROR, message);
		if (cause != null)
			logger.error(cause);
	}

	private void info(String message) {
		logger.info(message);
	}
}
