package fr.imag.adele.apam.maven.plugin.helpers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.InjectedPropertyPolicy;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.encoding.ipojo.ComponentParser;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.maven.plugin.InvalidApamMetadataException;

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
			Element rootElement, List<PropertyDefinition> addedDefinitions,
			Map<String, String> addedProperties)
			throws InvalidApamMetadataException {

		for (Element element : rootElement.getElements()) {

			/*
			 * Ignore not APAM elements
			 */
			if (element.getNameSpace() == null
					|| !ComponentParser.APAM.equals(element.getNameSpace())) {
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
			List<PropertyDefinition> addedDefinitions,
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
			Element toAdd = new Element(ComponentParser.DEFINITION,
					ComponentParser.APAM);

			toAdd.addAttribute(new Attribute(ComponentParser.ATT_NAME, def
					.getName()));
			
			if(def.getType()!= null) {
			toAdd.addAttribute(new Attribute(ComponentParser.ATT_TYPE, def
					.getType()));
			}
			
			if(def.hasDefaultValue()) {
			toAdd.addAttribute(new Attribute(ComponentParser.ATT_DEFAULT,
					def.getDefaultValue()));
			}
			
			if(def.getField()!=null) {
			toAdd.addAttribute(new Attribute(ComponentParser.ATT_FIELD, def
					.getField()));
			}
			
			if(def.getCallback()!=null) {
				toAdd.addAttribute(new Attribute(ComponentParser.ATT_METHOD, def
					.getCallback()));
			}
			
			if (def.getInjected() != null) {
				toAdd.addAttribute(new Attribute(
						ComponentParser.ATT_INJECTED, def.getInjected()
								.toString()));
			}

			element.addElement(toAdd);
		}

		// And add the properties
		for (String name : addedProperties.keySet()) {
			Element prop = new Element(ComponentParser.PROPERTY,
					ComponentParser.APAM);
			prop.addAttribute(new Attribute(ComponentParser.ATT_NAME, name));
			prop.addAttribute(new Attribute(ComponentParser.ATT_VALUE,
					addedProperties.get(name)));

			element.addElement(prop);
		}

		// addProperty("apam.version", "VersionedReference",
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

		Element[] tabElt = element.getElements(ComponentParser.DEFINITION,
				ComponentParser.APAM);

		if (tabElt == null) {
			return definitions;
		}
		for (Element subElement : tabElt) {
			String name = subElement.getAttribute(ComponentParser.ATT_NAME,
					ComponentParser.APAM);
			definitions.add(name);
			if (name != null & mapAddedDefinitions.containsKey(name)) {

				InjectedPropertyPolicy injectPolicy = InjectedPropertyPolicy
						.valueOf(subElement
								.getAttribute(ComponentParser.ATT_INJECTED));
				if (injectPolicy != null
						&& !injectPolicy
								.equals(InjectedPropertyPolicy.EXTERNAL))
					throw new InvalidApamMetadataException(
							"Property definition error (wrong injection policy) for property "
									+ name);

				String type = subElement
						.getAttribute(ComponentParser.ATT_TYPE);
				if (type != null
						&& !type.equals(mapAddedDefinitions.get(name).getType()))
					throw new InvalidApamMetadataException(
							"Property definition error (type mismatch) for property "
									+ name);

				String defaultVal = subElement.getAttribute(ComponentParser.ATT_DEFAULT);
				if (defaultVal != null	&& mapAddedDefinitions.get(name).hasDefaultValue() && !defaultVal.equals(mapAddedDefinitions.get(name).getDefaultValue()))
					throw new InvalidApamMetadataException(
							"Property definition error (default value mismatch) for property "
									+ name);

				String field 	= subElement.getAttribute(ComponentParser.ATT_FIELD);
				String callback = subElement.getAttribute(ComponentParser.ATT_METHOD);

				element.removeElement(subElement);
				PropertyDefinition updatedDef = new PropertyDefinition(new ComponentReference<ComponentDeclaration>("Dummy"), name, type, defaultVal, field, callback, injectPolicy);

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

		Element[] tabElt = element.getElements(ComponentParser.PROPERTY,
				ComponentParser.APAM);

		if (tabElt != null) {
			for (Element subElement : tabElt) {
				String name = subElement.getAttribute(
						ComponentParser.ATT_NAME, ComponentParser.APAM);
				if (name != null & addedProperties.containsKey(name)) {
					throw new InvalidApamMetadataException(
							"Property already defined : " + name);
				}
			}
		}

	}

}
