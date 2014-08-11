/**
 * Copyright 2011-2013 Universite Joseph Fourier, LIG, ADELE team
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
 *
 * AttributeCheckHelpers.java - 7 nov. 2013
 */
package fr.imag.adele.apam.apammavenplugin.helpers;

import java.util.Map;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.apammavenplugin.ApamCapability;
import fr.imag.adele.apam.apammavenplugin.CheckObr;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.InjectedPropertyPolicy;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.util.Attribute;

/**
 * @author thibaud
 * 
 */
public final class AttributeCheckHelpers {
	/**
	 * @param component
	 * @param ret
	 */
	public static void addComponentCaracteristics(
			ComponentDeclaration component, Map<String, Object> ret) {
		/*
		 * Add the component characteristics as final attributes, only if
		 * explicitly defined. Needed to compile members.
		 */
		if (component.isDefinedInstantiable()) {
			ret.put(CST.INSTANTIABLE,
					Boolean.toString(component.isInstantiable()));
		}
		if (component.isDefinedSingleton()) {
			ret.put(CST.SINGLETON, Boolean.toString(component.isSingleton()));
		}
		if (component.isDefinedShared()) {
			ret.put(CST.SHARED, Boolean.toString(component.isShared()));
		}
	}

	/**
	 * @param ret
	 * @param group
	 */
	public static void addDefaultValues(Map<String, Object> ret,
			ApamCapability group) {
		/*
		 * Add the default values specified in the group for properties not
		 * explicitly initialized
		 */
		if (group != null) {

			for (String prop : group.getValidAttrNames().keySet()) {
				if (Attribute.isInheritedAttribute(prop)
						&& ret.get(prop) == null
						&& group.getAttrDefault(prop) != null) {

					ret.put(prop, group.getAttrDefault(prop));
				}
			}
		}
	}

	/**
	 * @param ret
	 * @param entCap
	 * @return
	 */
	public static ApamCapability addAboveAttributes(Map<String, Object> ret,
			ApamCapability entCap) {
		/*
		 * add the attribute coming from "above" if not already instantiated and
		 * heritable
		 */
		ApamCapability group = entCap.getGroup();
		if (group != null && group.getProperties() != null) {
			if (group.getDcl() instanceof ImplementationDeclaration
					&& !group.getProperties().containsKey(CST.IMPLNAME)) {
				group.putAttr(CST.IMPLNAME, group.getName());
			}
			group.freeze();
			for (String prop : group.getProperties().keySet()) {
				if (ret.get(prop) == null
						&& Attribute.isInheritedAttribute(prop)) {
					ret.put(prop, group.getProperties().get(prop));
				}
			}
		}
		return group;
	}

	/**
	 * @param ent
	 * @param attr
	 * @param defAttr
	 * @param inheritedvalue
	 * @param parent
	 * @return
	 */
	public static String checkDefAttr(ApamCapability ent, String attr,
			String defAttr, String inheritedvalue, ApamCapability parent) {
		if (defAttr == null) {
			CheckObr.error("In " + ent.getName() + ", attribute \"" + attr
					+ "\" used but not defined.");
			return null;
		}

		if (inheritedvalue != null
				&& !inheritedvalue.equals(parent.getAttrDefault(attr))) {
			CheckObr.error("Cannot redefine attribute \"" + attr + "\"");
			return null;
		}

		return defAttr;
	}

	/**
	 * @param component
	 * @param group
	 */
	public static boolean checkPropertyDefinition(ComponentDeclaration component, PropertyDefinition definition, ApamCapability group) {
		
		String name = definition.getName() ;
		
		/*
		 * Final attributes cannot be defined or redefined
		 */
		if (Attribute.isFinalAttribute(name)) {
			// except if it is an external field attribute definition
			if (definition.getField() != null && definition.getInjected() == InjectedPropertyPolicy.EXTERNAL) 
				return true ;
			CheckObr.error("Cannot redefine final attribute \"" + name + "\"");
			return false;
		}

		if (Attribute.isReservedAttributePrefix(name)) {
			CheckObr.error("Attribute\"" + name + "\" is reserved");
			return false;
		}

		if (group != null) {
			String groupType = group.getAttrDefinition(name);
			if (groupType != null) {
				// Allowed only if defining a field, and it types are the same.
				// Default values are not allowed above in that case.
				PropertyDefinition propDef = component
						.getPropertyDefinition(name);
				if (propDef == null) {
					CheckObr.error("Invalid property definition " + name);
					return false;
				}
				if (propDef.getField() == null) {
					CheckObr.error("Property " + name
							+ " allready defined in the group.");
					return false;
				}
				if (!propDef.getType().equals(groupType)) {
					CheckObr.error("Cannot refine property definition " + name
							+ " Not the same types : " + propDef.getType()
							+ " not equal " + groupType);
					return false;
				}
				if (group.getAttrDefault(name) != null) {
					CheckObr.error("Cannot refine property definition with a default value properties "
							+ name + "=" + group.getAttrDefault(name));
					return false;
				}
			}
		}
		return true;
	}

}
