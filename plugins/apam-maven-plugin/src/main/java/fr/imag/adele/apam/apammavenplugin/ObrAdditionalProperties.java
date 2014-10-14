package fr.imag.adele.apam.apammavenplugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.manipulator.metadata.FileMetadataProvider;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.plugin.MavenReporter;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import fr.imag.adele.apam.declarations.encoding.ipojo.ComponentParser;

public class ObrAdditionalProperties {

	public static final List<String> additionalStringProperties = new ArrayList<String>(
			Arrays.asList("maven.groupId", "maven.artifactId", "maven.version"));;
	public static final List<String> additionalVersionProperties = new ArrayList<String>(
			Arrays.asList("apam.version", "version"));;

	public static Map<String, Element> parseFile(File obrFile, Log logger)
			throws FileNotFoundException, IOException {

		if (obrFile == null || !obrFile.exists() || !obrFile.canRead())
			throw new FileNotFoundException("Problem opening file : " + obrFile
					+ " not exists or not readable");

		Log myLog;

		if (logger == null)
			myLog = new SystemStreamLog();
		else
			myLog = logger;

		FileMetadataProvider obrMetadataProvider = new FileMetadataProvider(
				obrFile, new MavenReporter(myLog));

		if (obrMetadataProvider != null
				&& obrMetadataProvider.getMetadatas() != null)
			return parseElements(obrMetadataProvider.getMetadatas());
		return null;
	}

	private static Map<String, Element> parseElements(
			List<Element> listElements) {
		Map<String, Element> additionalProperties = new HashMap<String, Element>();

		for (Element elt : listElements) {
			if (elt.getName().equals("capability")
					&& elt.getAttribute("name") != null
					&& elt.getAttribute("name").equals("apam-component")) {
				String componentName = null;
//				List<Element> props = new ArrayList<Element>();
				Element root = new Element("obr", ComponentParser.APAM);

				for (Element subelt : elt.getElements("p")) {
					String name = subelt.getAttribute("n");
					String type = subelt.getAttribute("t");
					String value = subelt.getAttribute("v");
					System.err.println(" name : "+name
							+ ", type "+type
							+ " , value : "+value);
					if (name.equals("name")) {
						componentName=value;
						
					}
					if (name != null && type != null && value != null) {
						if (type.equals("string")
								&& additionalStringProperties.contains(name)) {
							addProperty(name, type, value, root);
						} else if (type.equals("version")
								&& additionalVersionProperties.contains(name)) {
							addProperty(name, type, value, root);
						}
					}
				}
				additionalProperties.put(componentName, root);//props.toArray(new Element[0]));
				System.err.println("Parsed OBR properties for : "+componentName
						+", props "+root.toXMLString());
			}
		}
		System.err.println("Parsed OBR properties : "+additionalProperties.keySet());
		return additionalProperties;
	}

	private static void addProperty(String propertyName, String propertyType,
			String propertyValue, Element root) {
		System.err.println("Adding a property, " + propertyName + " = "
				+ propertyValue);
		
		// TODO : On implementation level, if property injected using field or method
		// TODO : add injected=internal
		// maybe in the runtime parser (we know the fields in this part)
		
		Element def = new Element(ComponentParser.DEFINITION,
				ComponentParser.APAM);
		def.addAttribute(new Attribute(ComponentParser.ATT_NAME, propertyName));
		def.addAttribute(new Attribute(ComponentParser.ATT_TYPE, propertyType));
		root.addElement(def);
	
		
		Element prop = new Element(ComponentParser.PROPERTY,
				ComponentParser.APAM);
		prop.addAttribute(new Attribute(ComponentParser.ATT_NAME, propertyName));
		prop.addAttribute(new Attribute(ComponentParser.ATT_VALUE, propertyValue));
		root.addElement(prop);
	
	}
}
