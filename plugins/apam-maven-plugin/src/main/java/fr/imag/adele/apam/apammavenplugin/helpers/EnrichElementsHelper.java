package fr.imag.adele.apam.apammavenplugin.helpers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

import fr.imag.adele.apam.apammavenplugin.InvalidApamMetadataException;
import fr.imag.adele.apam.declarations.InjectedPropertyPolicy;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.util.CoreMetadataParser;

/**
 * Helper class to add properties to an ApAM Component using its iPOJO metadata
 * description
 * 
 * @author thibaud
 * 
 */
public class EnrichElementsHelper {

	/**
	 * Helper method to add a set of property definitions and property settings,
	 * to all children Element (that should be ApAM Component) from an metadata
	 * root Element.
	 * 
	 * @param rootElement
	 *            Should be the "iPOJO-Components" property of the manifest file
	 * @param addedDefinitions
	 *            The property definitions (if the component is an
	 *            Implementation, it checks if injection policies are valid)
	 * @param addedProperties
	 *            The added properties, property name MUST be relative to an
	 *            added definition, property value will erase an already
	 *            existing value.
	 */
	public static void addPropertiesToChildrenApAMComponents(
			Element rootElement, Set<PropertyDefinition> addedDefinitions,
			Map<String, String> addedProperties)
			throws InvalidApamMetadataException {

		for (Element element : rootElement.getElements()) {

			/*
			 * Ignore not APAM elements
			 */
			if (element.getNameSpace() == null
					|| !CoreMetadataParser.APAM.equals(element.getNameSpace())) {
				continue;
			}

			addPropertiesToSingleApAMComponent(element, addedDefinitions,
					addedProperties);
		}

	}

	/**
	 * Helper method to add a set of property definitions and property settings,
	 * to an Element (that should be ApAM Component) from an metadata root
	 * Element.
	 * 
	 * @param element
	 *            Should be an ApAM Component
	 * @param addedDefinitions
	 *            The property definitions (if the component is an
	 *            Implementation, it checks if injection policies are valid).
	 *            These property definitions MUST not provide any attribute
	 *            related to java injection (no field, no callback)
	 * @param addedProperties
	 *            The added properties, property name MUST be relative to an
	 *            added definition, property value will erase an already
	 *            existing value.
	 */
	public static void addPropertiesToSingleApAMComponent(Element element,
			Set<PropertyDefinition> addedDefinitions,
			Map<String, String> addedProperties)
			throws InvalidApamMetadataException {
		Map<String, PropertyDefinition> mapAddedDefinitions = new HashMap<String, PropertyDefinition>();


		// First we create a map of all the property definition we will add
		// (to be able to modify an existing one before adding it)
		if (addedDefinitions != null) {
			for (PropertyDefinition definition : addedDefinitions) {
				mapAddedDefinitions.put(definition.getName(), definition);
			}
		}

		// Then we iterates on all already defined properties on the component
		// (checking property names)
		// If we found we remove the corresponding element and update the
		// definition to add

		Set<String> componentPropertyDefinitions = checkExistingDefinitions(
				element, mapAddedDefinitions);
		checkExistingProperty(element, addedProperties,
				componentPropertyDefinitions);

		// Finally we can add definitions
		for (PropertyDefinition def : mapAddedDefinitions.values()) {
			Element toAdd = new Element(CoreMetadataParser.DEFINITION,
					CoreMetadataParser.APAM);

			toAdd.addAttribute(new Attribute(CoreMetadataParser.ATT_NAME, def
					.getName()));
			
			if(def.getType()!= null) {
			toAdd.addAttribute(new Attribute(CoreMetadataParser.ATT_TYPE, def
					.getType()));
			}
			
			if(def.getDefaultValue()!= null) {
			toAdd.addAttribute(new Attribute(CoreMetadataParser.ATT_DEFAULT,
					def.getDefaultValue()));
			}
			
			if(def.getField()!=null) {
			toAdd.addAttribute(new Attribute(CoreMetadataParser.ATT_FIELD, def
					.getField()));
			}
			
			if(def.getCallback()!=null) {
				toAdd.addAttribute(new Attribute(CoreMetadataParser.ATT_METHOD, def
					.getCallback()));
			}
			
			if (def.getInjected() != null) {
				toAdd.addAttribute(new Attribute(
						CoreMetadataParser.ATT_INJECTED, def.getInjected()
								.toString()));
			}

			element.addElement(toAdd);
		}

		// And add the properties
		for (String name : addedProperties.keySet()) {
			Element prop = new Element(CoreMetadataParser.PROPERTY,
					CoreMetadataParser.APAM);
			prop.addAttribute(new Attribute(CoreMetadataParser.ATT_NAME, name));
			prop.addAttribute(new Attribute(CoreMetadataParser.ATT_VALUE,
					addedProperties.get(name)));

			element.addElement(prop);
		}

		// addProperty("apam.version", "Version",
		// ApamMavenProperties.mavenVersion.replace('-', '.'), element);

	}

	/**
	 * Checks if property definition already exists as elements, if it the case,
	 * it removes its from the elements, and update the defintion
	 * 
	 * @param element
	 * @param mapAddedDefinitions
	 * @return
	 * @throws InvalidApamMetadataException
	 */
	private static Set<String> checkExistingDefinitions(Element element,
			Map<String, PropertyDefinition> mapAddedDefinitions)
			throws InvalidApamMetadataException {
		
		Set<String> definitions = new HashSet<String>();

		Element[] tabElt = element.getElements(CoreMetadataParser.DEFINITION,
				CoreMetadataParser.APAM);

		if (tabElt == null) {
			return definitions;
		}
		for (Element subElement : tabElt) {
			String name = subElement.getAttribute(CoreMetadataParser.ATT_NAME,
					CoreMetadataParser.APAM);
			definitions.add(name);
			if (name != null & mapAddedDefinitions.containsKey(name)) {

				InjectedPropertyPolicy injectPolicy = InjectedPropertyPolicy
						.valueOf(subElement
								.getAttribute(CoreMetadataParser.ATT_INJECTED));
				if (injectPolicy != null
						&& !injectPolicy
								.equals(InjectedPropertyPolicy.EXTERNAL))
					throw new InvalidApamMetadataException(
							"Property definition error (wrong injection policy) for property "
									+ name);

				String type = subElement
						.getAttribute(CoreMetadataParser.ATT_TYPE);
				if (type != null
						&& !type.equals(mapAddedDefinitions.get(name).getType()))
					throw new InvalidApamMetadataException(
							"Property definition error (type mismatch) for property "
									+ name);

				String defaultVal = subElement
						.getAttribute(CoreMetadataParser.ATT_DEFAULT);
				if (defaultVal != null
						&& !defaultVal.equals(mapAddedDefinitions.get(name)
								.getDefaultValue()))
					throw new InvalidApamMetadataException(
							"Property definition error (default value mismatch) for property "
									+ name);

				String field = subElement
						.getAttribute(CoreMetadataParser.ATT_FIELD);
				String callback = subElement
						.getAttribute(CoreMetadataParser.ATT_METHOD);

				element.removeElement(subElement);
				PropertyDefinition updatedDef = new PropertyDefinition(
						new SpecificationDeclaration("Dummy"), name, type,
						defaultVal, field, callback, injectPolicy);

				mapAddedDefinitions.put(name, updatedDef);
			}
		}
		return definitions;
	}

	/**
	 * Checks if property already exists, and if the definition exists at this
	 * level (inheritance not allowed when defining properties at build)
	 * 
	 * @param element
	 * @param addedDefinitions
	 * @throws InvalidApamMetadataException
	 */
	private static void checkExistingProperty(Element element,
			Map<String, String> addedProperties,
			Set<String> componentPropertyDefinitions)
			throws InvalidApamMetadataException {


		// TODO correct this
		// if
		// (!componentPropertyDefinitions.containsAll(addedProperties.keySet()))
		// {
		// throw new InvalidApamMetadataException(
		// "Some properties are not defined at this level");
		// }

		Element[] tabElt = element.getElements(CoreMetadataParser.PROPERTY,
				CoreMetadataParser.APAM);

		if (tabElt != null) {
			for (Element subElement : tabElt) {
				String name = subElement.getAttribute(
						CoreMetadataParser.ATT_NAME, CoreMetadataParser.APAM);
				if (name != null & addedProperties.containsKey(name)) {
					throw new InvalidApamMetadataException(
							"Property already defined : " + name);
				}
			}
		}

	}

}