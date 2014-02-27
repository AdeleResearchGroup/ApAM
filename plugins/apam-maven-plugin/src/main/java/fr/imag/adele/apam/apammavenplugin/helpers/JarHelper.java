package fr.imag.adele.apam.apammavenplugin.helpers;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.apammavenplugin.InvalidApamMetadataException;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.util.CoreMetadataParser;
import fr.imag.adele.apam.util.CoreParser;
import fr.imag.adele.apam.util.CoreParser.ErrorHandler;
import fr.imag.adele.apam.util.CoreParser.ErrorHandler.Severity;

public class JarHelper {

	Logger logger = LoggerFactory.getLogger(JarHelper.class);

	ErrorHandler m_ErrorHandler;
	File m_File;
	
	JarFile m_JarFile;
	
	public JarFile getJarFile() {
		try {
			return new JarFile(m_File);
		} catch (IOException e) {
			logger.error(e.getMessage());
			m_ErrorHandler.error(Severity.ERROR, e.getMessage());
			return null;
		}
	}

	Manifest m_Manifest;

	public Manifest getManifest() {
		return m_Manifest;
	}

	public JarHelper(File file, ErrorHandler handler)
			throws InvalidApamMetadataException {

		if (handler == null) {
			throw new InvalidApamMetadataException("No ErrorHandlerProvided");
		} else {
			m_ErrorHandler = handler;
		}

		try {
			if (file == null || !file.exists()) {
				throw new InvalidApamMetadataException(
						"Jar File does not exists");
			}
			m_File=file;

			m_JarFile = new JarFile(m_File);
			m_Manifest = m_JarFile.getManifest();

			if (m_Manifest == null) {
				m_JarFile.close();
				throw new InvalidApamMetadataException("Manifest is Empty");
			}

			m_JarFile.close();

		} catch (IOException e) {
			logger.error(e.getMessage());
			m_ErrorHandler.error(Severity.ERROR, e.getMessage());
		}

	}
	

	public List<ComponentDeclaration> getApAMComponents()
			throws InvalidApamMetadataException {
		if(getRootiPojoElement() == null) {
			return Collections.emptyList();
		}

		CoreParser parser = new CoreMetadataParser(getRootiPojoElement(), null);
		List<ComponentDeclaration> ret = parser.getDeclarations(m_ErrorHandler);

		String contains = "    contains components: ";
		for (ComponentDeclaration comp : ret) {
			contains += comp.getName() + " ";
		}
		logger.info(contains);

		return ret;
	}

	public Element getRootiPojoElement() {
		Attributes iPOJOmetadata = m_Manifest.getMainAttributes();
		String ipojoMetadata = iPOJOmetadata.getValue("iPOJO-Components");

		iPOJOmetadata = null;
		if (ipojoMetadata == null) {
			String message = " No Apam metadata for " + m_File;
			logger.error(message);
			return null;
		}

		logger.info("Parsing Apam metadata for " + m_File + " - SUCCESS ");
		try {
			Element root = ManifestMetadataParser
					.parseHeaderMetadata(ipojoMetadata);
			return root;
		} catch (ParseException e) {
			String message = "Parsing manifest metadata for " + m_File
					+ " - FAILED ";
			logger.error(message);
			m_ErrorHandler.error(Severity.ERROR, message);

		}
		return null;
	}

}
