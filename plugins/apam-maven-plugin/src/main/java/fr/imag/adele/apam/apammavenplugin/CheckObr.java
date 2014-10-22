/**
 * Copyright 2011-2012 Universite Joseph Fourier, LIG, ADELE team
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
 */
package fr.imag.adele.apam.apammavenplugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.osgi.framework.Version;

import fr.imag.adele.apam.AttrType;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.apammavenplugin.helpers.AttributeCheckHelpers;
import fr.imag.adele.apam.apammavenplugin.helpers.FilterCheckHelpers;
import fr.imag.adele.apam.apammavenplugin.helpers.ProvideHelpers;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration.CodeReflection;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.ConstrainedReference;
import fr.imag.adele.apam.declarations.GrantDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.OwnedComponentDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.RelationPromotion;
import fr.imag.adele.apam.declarations.RequirerInstrumentation;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.VisibilityDeclaration;
import fr.imag.adele.apam.declarations.encoding.ipojo.ComponentParser;
import fr.imag.adele.apam.declarations.references.components.ComponentReference;
import fr.imag.adele.apam.declarations.references.components.Versioned;
import fr.imag.adele.apam.declarations.references.resources.InterfaceReference;
import fr.imag.adele.apam.declarations.references.resources.MessageReference;
import fr.imag.adele.apam.declarations.references.resources.PackageReference;
import fr.imag.adele.apam.declarations.references.resources.ResourceReference;
import fr.imag.adele.apam.declarations.references.resources.UnknownReference;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Attribute;
import fr.imag.adele.apam.util.Substitute;
import fr.imag.adele.apam.util.Substitute.SplitSub;
import fr.imag.adele.apam.util.Util;

public final class CheckObr {

	/**
	 * An string value that will be used to represent mandatory attributes not
	 * specified. From Decoder.
	 */
	public final static String UNDEFINED = "<undefined value>";

	private final ApamCapabilityBroker broker;
	private final ClasspathDescriptor classpath;
	
	private final Set<String> allFields = new HashSet<String>();
	private final Set<String> allGrants = new HashSet<String>();
	
	private final Log logger;
	
	private boolean failedChecking = false;

	/**
	 * Private constructor protects creating instances from this class (only
	 * static methods)
	 */
	public CheckObr(ClasspathDescriptor classpath, ApamCapabilityBroker broker, Log logger) {
		this.broker 	= broker;
		this.classpath	= classpath;
		this.logger 	= logger;
	}

	/**
	 * Get the capability associated to the declaration in the broker
	 */
	private ApamCapability capability(ComponentDeclaration declaration) {
		return broker.get(declaration);
	}

	/**
	 * Get the declaration of a capability
	 */
	private static ComponentDeclaration declaration(ApamCapability capability) {
		return capability != null ? capability.getDeclaration() : null;
	}
	
	public final boolean hasFailedChecking() {
		return failedChecking;
	}

	public final void error(ComponentDeclaration component, String msg) {
		error("In "+ component.getName() + " "+msg);
	}

	public final void error(String msg) {
		this.failedChecking = true;
		if (logger != null) logger.error(msg);
	}

	public final void warning(String msg) {
		if (logger != null) logger.warn(msg);
	}

	public final void info(String msg) {
		if (logger != null) logger.info(msg);
	}


	/**
	 * Get the component representing the target of a relation.
	 * 
	 * Returns null either if the target is not a component or it is not
	 * available in the repository
	 * 
	 */
	private ApamCapability target(RelationDeclaration relation) {
		return broker.getTargetComponent(relation);
	}

	/**
	 * Whether the target of a relation is a component
	 */
	private static boolean targetIsComponent(RelationDeclaration relation) {
		return relation.getTarget().as(ComponentReference.class) != null;
	}
	
	/**
	 * Checks if the constraints set on this relation are syntacticaly valid.
	 * Only for specification dependencies. Checks if the attributes mentioned
	 * in the constraints can be set on an implementation of that specification.
	 *
	 * @param dep
	 *            a relation
	 */
	private void checkConstraint(ComponentDeclaration component, RelationDeclaration relation) {

		if ((relation == null) || !targetIsComponent(relation)) {
			return;
		}

		if (relation.isMultiple() && 
			(!relation.getImplementationPreferences().isEmpty() || !relation.getInstancePreferences().isEmpty())) {
			error("Preferences cannot be defined for a relation with multiple cardinality: "
					+ relation.getIdentifier());
		}

		// get the spec or impl definition
		ApamCapability target = target(relation);
		if (target != null) {

			// computes the attributes that can be associated with this spec or
			// implementations members
			Map<String, String> validAttrs = target.getValidAttrNames();

			checkFilters(component, relation.getImplementationConstraints(),
					relation.getImplementationPreferences(), validAttrs,
					relation.getTarget().getName());
			
			checkFilters(component, relation.getInstanceConstraints(),
					relation.getInstancePreferences(), validAttrs,
					relation.getTarget().getName());
		}

	}

	/**
	 * Checks the attributes *defined* in the component; if valid, they are
	 * returned. Then the attributes pertaining to the entity above are added.
	 * Then the final attributes
	 *
	 * @param component
	 *            the component to check
	 */
	public  Map<String, Object> getValidProperties(ComponentDeclaration component) {
		// the attributes to return
		Map<String, Object> ret = new HashMap<String, Object>();
		// Properties of this component
		Map<String, String> properties = component.getProperties();

		ApamCapability entCap = capability(component);
		if (entCap == null) {
			return ret; // should never happen.
		}

		getValidAttributes(component, ret, properties, entCap);

		ApamCapability group = AttributeCheckHelpers.addAboveAttributes(ret,entCap,this);

		AttributeCheckHelpers.addDefaultValues(ret, group);

		AttributeCheckHelpers.addComponentCaracteristics(component, ret);

		return ret;
	}

	/**
	 * @param component
	 * @param ret
	 * @param properties
	 * @param entCap
	 */
	private void getValidAttributes(ComponentDeclaration component,
			Map<String, Object> ret, Map<String, String> properties,
			ApamCapability entCap) {
		/*
		 * return the valid attributes
		 */
		for (String attr : properties.keySet()) {
			String defAttr = getDefAttr(entCap, attr);
			if (defAttr == null) {
				continue;
			}
			Object val = Attribute.checkAttrType(attr, properties.get(attr),
					defAttr);
			if (val == null) {
				error("invalid attribute value "+attr);
				continue;
			}
			// checkSubstitute (ComponentDeclaration component, String attr,
			// String type, String defaultValue)
			if (checkSubstitute(component, attr, defAttr, properties.get(attr))) {
				ret.put(attr, val);
			}
		}
	}

	/**
	 * Checks if the attribute / values pair is valid for the component ent. If
	 * a final attribute, it is ignored but returns null. (cannot be set).
	 *
	 * For "integer" returns an Integer object, otherwise it is the string
	 * "value"
	 *

	 * @return
	 */
	private String getDefAttr(ApamCapability ent, String attr) {
		if (Attribute.isFinalAttribute(attr)
				|| !Attribute.validAttr(ent.getName(), attr)) {
			return null;
		}

		String defAttr = null;
		String inheritedvalue = null;

		ApamCapability parent = ent;

		while (parent != null && inheritedvalue == null) {
			if (defAttr == null) {
				defAttr = parent.getAttrDefinition(attr);
			}
			if (!parent.equals(ent)) {
				inheritedvalue = parent.getProperty(attr);
			}
			parent = parent.getGroup();
		}

		return AttributeCheckHelpers.checkDefAttr(ent, attr, defAttr,
				inheritedvalue, parent, this);
	}

	public boolean checkProperty(ComponentDeclaration component, PropertyDefinition definition) {

		String type = definition.getType();
		String name = definition.getName();
		String defaultValue = definition.getDefaultValue();

		ApamCapability group = capability(component).getGroup();

		if (!AttributeCheckHelpers.checkPropertyDefinition(component, definition, group, this)) {
			return false;
		}

		// We have a default value, check it as if a property.
		if (type != null && defaultValue != null && !defaultValue.isEmpty()) {
			if (Attribute.checkAttrType(name, defaultValue, type) != null) {
				return checkSubstitute(component, name, type, defaultValue);
			} else {
				error("invalid default value "+name);
				return false;
			}
		}

		// no default value. Only check if the type is valid
		if (!Attribute.validAttrType(type)) {
			error("invalid attrobute type "+name);
			return false;
		}

		return true;
	}

	private boolean checkFunctionSubst(ComponentDeclaration component,
			String type, String defaultValue) {

		if (!(component instanceof AtomicImplementationDeclaration)) {
			return false;
		}
		CodeReflection reflection = ((AtomicImplementationDeclaration) component)
				.getReflection();

		String function = defaultValue.substring(1);
		try {
			// 1° Verify if method exists
			reflection.getMethodParameterNumber(function, true);

			// 2° Verify method return Type
			// TODO Verify this !
			String returnType = reflection.getMethodReturnType(function, null,
					true);
			if (returnType == null || !returnType.equals(type)) {
				return false;
			}

			return true;
		} catch (NoSuchMethodException exc) {
			return false;
		}

	}

	/**
	 * Checks the syntax of the filter. Warning : does not check the values :
	 * their type and the substitutions.
	 *
	 * @param filter
	 * @return
	 */
	private boolean checkSyntaxFilter(String filter) {
		try {
			ApamFilter parsedFilter = ApamFilter.newInstance(filter);
			return parsedFilter != null;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isSubstitute(ComponentDeclaration component,
			String attr) {
		PropertyDefinition def = component.getPropertyDefinition(attr);
		return (def != null && def.getDefaultValue() != null && (def
				.getDefaultValue().indexOf('$') == 0 || def.getDefaultValue()
				.indexOf('@') == 0));
	}

	public boolean checkFilter(ApamFilter filt, ComponentDeclaration component, Map<String, String> validAttr, String f, String spec) {
		switch (filt.op) {
		case ApamFilter.AND:
		case ApamFilter.OR: {
			return FilterCheckHelpers.checkFilterOR(filt, component, validAttr,
					f, spec, this);
		}

		case ApamFilter.NOT: {
			ApamFilter filter = (ApamFilter) filt.value;
			return checkFilter(filter, component, validAttr, f, spec);
		}

		case ApamFilter.SUBSTRING:
		case ApamFilter.EQUAL:
		case ApamFilter.GREATER:
		case ApamFilter.LESS:
		case ApamFilter.APPROX:
		case ApamFilter.SUBSET:
		case ApamFilter.SUPERSET:
		case ApamFilter.PRESENT:
			return FilterCheckHelpers.checkFilterPRESENT(filt, component,
					validAttr, f, spec, this);
		}
		return true;
	}

	private final ApamCapability RUNTIME_COMPONET = new ApamCapability();
	
	public boolean checkSubstitute(ComponentDeclaration component,
			String attr, String type, String defaultValue) {
		// If it is a function substitution
		if (defaultValue.charAt(0) == '@') {
			return checkFunctionSubst(component, type, defaultValue);
		}

		// If it is not a substitution, do nothing
		if (defaultValue.charAt(0) != '$') {
			return true;
		}

		/*
		 * String substitution
		 */
		SplitSub sub = Substitute.split(defaultValue);
		if (sub == null) {
			error("Invalid substitute value " + defaultValue
					+ " for attribute " + attr);
			return false;
		}

		AttrType st = new AttrType(type);

		ApamCapability source = capability(component);
		if (!sub.sourceName.equals("this")) {
			// Look for the source component
			source = broker.getByName(sub.sourceName);
			if (source == null) {
				error("Component " + sub.sourceName
						+ " not found in substitution : " + defaultValue
						+ " of attribute " + attr);
				return false;
			}
		}

		/*
		 * if we have a navigation, get the navigation target
		 */
		source = getTargetNavigation(source, sub.depIds, defaultValue);
		if (source == null) {
			return false;
		}
		if (source == RUNTIME_COMPONET) {
			return true;
		}

		/*
		 * check if the attribute is defined and if types are compatibles.
		 */
		String pd = source.getAttrDefinition(sub.attr);
		if (pd == null) {
			error("Substitute attribute " + attr + "=" + defaultValue
					+ ".  Undefined attribute " + sub.attr + " for component "
					+ source.getName());
			return false;
		}
		if (Substitute.checkSubType(st, new AttrType(pd), attr, sub)) {
			return true;
		}

		error("substitution navigation error "+st);
		return false;

	}

	/**
	 * expecting that the relation will be inherited, create a dummy capability
	 * and dummy component that only refers to its group in order to get
	 * relation definitions to continue navigating
	 *
	 * @param source
	 * @return
	 */
	private ApamCapability buildDummyImplem(ApamCapability source) {
		
		SpecificationDeclaration specification = (SpecificationDeclaration) declaration(source);
		
		AtomicImplementationDeclaration bidon = new AtomicImplementationDeclaration("void-" + specification.getName(),
														Versioned.any(specification.getReference()), null);
		return new ApamCapability(broker,bidon,Version.emptyVersion);
	}

	private ApamCapability buildDummyInst(ApamCapability source) {

		ImplementationDeclaration implementation = (ImplementationDeclaration) declaration(source);
		Versioned<? extends ImplementationDeclaration> versionImplem = Versioned.any(implementation.getReference());
		InstanceDeclaration bidon = new InstanceDeclaration(versionImplem,source.getName() + "-01", null);
		return new ApamCapability(broker,bidon,Version.emptyVersion);
	}

	/**
	 * Compute the set of objects destination of a navigation from source
	 * through final relation depname. If the navigation cannot be checked,
	 * return the capability "ApamCapability.trueCap", if it is invalid, return
	 * "null", otherwise returns the target capabilities.
	 *
	 * @param source
	 * @param depName
	 * @return
	 */
	private ApamCapability getCapFinalRelation(ApamCapability source, String depName) {
		if (!CST.isFinalRelation(depName))
			return null;

		if (depName.equals(CST.REL_SPEC)) {
			switch (source.getKind()) {
				case SPECIFICATION:
					return source;
				case IMPLEMENTATION:
					return source.getGroup();
				case INSTANCE:
					return source.getGroup().getGroup();
				default:
					return null;
			}
		}
		
		if (depName.equals(CST.REL_IMPL)) {
			switch (source.getKind()) {
				case IMPLEMENTATION:
					return source;
				case INSTANCE:
					return source.getGroup();
				default:
					return null;
			}
		}
			
		if (depName.equals(CST.REL_INST)) {
			switch (source.getKind()) {
				case INSTANCE:
					return source;
				default:
					return null;
			}
		}

		if (depName.equals(CST.REL_IMPLS)) {
			switch (source.getKind()) {
				case SPECIFICATION:
					return buildDummyImplem(source);
				case IMPLEMENTATION:
					return source;
				case INSTANCE:
					return source.getGroup();
				default:
					return null;
			}
		}

		if (depName.equals(CST.REL_INSTS)) {
			switch (source.getKind()) {
				case IMPLEMENTATION:
					return buildDummyInst(source);
				case INSTANCE:
					return source;
				default:
					return null;
			}
		}

		if (depName.equals(CST.REL_COMPOSITE)) {
			// cannot compute staticaly
			return RUNTIME_COMPONET;
		}
		
		if (depName.equals(CST.REL_COMPOTYPE)) {
			// cannot compute staticaly
			return RUNTIME_COMPONET;
		}
		
		if (depName.equals(CST.REL_CONTAINS)) {
			// cannot compute staticaly
			return RUNTIME_COMPONET;
		}
		
		return null;
	}

	/**
	 * Return the definition of the last component for a navigation.
	 *
	 * @param source
	 * @param navigation
	 * @return null if false, ApamCapability.trueCap if not possible to check,
	 *         the last ApamCompoent in the navigation if successfull
	 */
	private ApamCapability getTargetNavigation(ApamCapability source,
			List<String> navigation, String defaultValue) {
		if (navigation == null || navigation.isEmpty()) {
			return source;
		}

		for (String rel : navigation) {
			if (CST.isFinalRelation(rel)) {
				source = getCapFinalRelation(source, rel);
				if (source == null) {
					return RUNTIME_COMPONET;
				}
				continue;
			}

			RelationDeclaration depDcl = getRelationDefinition(source, rel);
			if (depDcl == null) {
				error("Relation " + rel + " undefined for "
						+ source.getKind() + " "
						+ source.getName() + " in substitution : \""
						+ defaultValue + "\"");
				return null;
			}

			if (!targetIsComponent(depDcl)) { // it is an interface or message
				// target. Cannot check.
				warning(depDcl.getTarget().getName()
						+ " is an interface or message. Substitution \""
						+ defaultValue + "\" cannot be checked");
				return RUNTIME_COMPONET;
			}

			source = target(depDcl);
			if (source == null) {
				error("Component " + depDcl.getTarget().getName()
						+ " not found in substitution : \"" + defaultValue
						+ "\"");
				return null;
			}
		}

		return source;

	}

	/**
	 * Whether implementation actually provides the resources of the specification
	 */
	public boolean checkImplProvide(ImplementationDeclaration implementation) {
		
		if (! capability(implementation).isGroupReferenceValid()) {
			return false;
		}
		
		ApamCapability specification = capability(implementation).getGroup();
		
		if (specification == null) {
			return true;
		}

		ProvideHelpers.checkResources(implementation,declaration(specification),MessageReference.class,this);
		ProvideHelpers.checkResources(implementation,declaration(specification),InterfaceReference.class,this);

		return true;
	}

	private Set<ResourceReference> getAllProvidedResources(ImplementationDeclaration implementation, Class<? extends ResourceReference> kind) {
		
		SpecificationDeclaration specification 	=  (SpecificationDeclaration) declaration(capability(implementation).getGroup());
		Set<ResourceReference> provided 		= new HashSet<ResourceReference>();

		if (specification != null) {
			provided.addAll(specification.getProvidedResources(kind));
		}
		
		provided.addAll(implementation.getProvidedResources(kind));
		
		return provided;
	}


	public void checkCompoMain(CompositeDeclaration composite) {

		Set<ResourceReference> allProvidedResources = getAllProvidedResources(composite,ResourceReference.class);
		// Abstract composite have no main implem, but must not provide any resource
		if (composite.getMainComponent() == null) {
			if (!allProvidedResources.isEmpty()) {
				error("Composite "
						+ composite.getName()
						+ " does not declare a main implementation, but provides resources "
						+ Util.list(allProvidedResources,true));
			}
			return;
		}

		String mainName 			= composite.getMainComponent().getName();
		ComponentDeclaration main 	= declaration(broker.getByName(mainName));
		
		if (main == null) {
			return;
		}
		
		if (composite.getGroup() != null && main.getGroup() != null && !composite.getGroupVersioned().equals(main.getGroupVersioned()) ) {
			error(composite,"Invalid main implementation. "+ mainName + " must implement specification " + composite.getGroup().getName());
		}

		Set<MessageReference> mainMessages 		= main.getProvidedResources(MessageReference.class);
		Set<MessageReference> compositeMessages = composite.getProvidedResources(MessageReference.class);
		if (!mainMessages.containsAll(compositeMessages)) {
			error(composite, "Invalid main implementation. "+ 
					mainName + " produces messages " + mainMessages + " instead of " + compositeMessages);
		}

		Set<InterfaceReference> mainInterfaces		= main.getProvidedResources(InterfaceReference.class);
		Set<InterfaceReference> compositeInterfaces	= composite.getProvidedResources(InterfaceReference.class);
		if (!mainInterfaces.containsAll(compositeInterfaces)) {
			error(composite, "Invalid main implementation. "+
					mainName + " implements " + mainInterfaces + " instead of " + compositeInterfaces);
		}
	}

	/**
	 * Checks if a class or interface exists among the bundles read by Maven to
	 * compile. Note we cannot know
	 *
	 * @param interf
	 * @return
	 */
	public boolean checkResourceExists(ResourceReference resource) {
		
		/*
		 *  Can be unknown if the resource is defined by the type of a generic collection, since it is not
		 *  possible to get the type at compile time
		 */
		
		if (resource == null || resource instanceof UnknownReference) {
			return true;
		}
		
		/*
		 * TODO check that the package is exported by some bundle in the classpath
		 */
		if (resource.as(PackageReference.class) != null) {
			return true;
		}
		
		if (classpath.getElementsHavingClass(resource.getJavaType()) == null) {
			error("Java class " + resource.getJavaType() + " does not exist in your build dependencies");
			return false;
		}
		return true;
	}

	/**
	 * For all kinds of components checks the dependencies : fields (for
	 * implems), and constraints.
	 *
	 * @param component
	 */
	public void checkRelations(ComponentDeclaration component) {
		Set<RelationDeclaration> relations = component.getRelations();
		if (relations == null || relations.isEmpty()) {
			return;
		}

		allFields.clear();
		Set<String> relationIds = new HashSet<String>();

		for (RelationDeclaration relation : relations) {

			// Checking for predefined relations. Cannot be redefined
			if (CST.isFinalRelation(relation.getIdentifier())) {
				error("relation " + relation.getIdentifier() + " is predefined.");
				continue;
			}

			// Checking for double relation Id
			if (relationIds.contains(relation.getIdentifier())) {
				error("relation " + relation.getIdentifier() + " allready defined.");
				continue;
			}
			relationIds.add(relation.getIdentifier());

			// replace the definition by the effective relation (adding group
			// definition)
			relation = computeGroupRelation(component, relation);

			// validating relation constraints and preferences..
			checkConstraint(component, relation);

			// Checking fields and complex dependencies
			checkFieldTypeDep(relation);

			// eager and hide cannot be defined here
			// TODO relation, attention! this block was removed since now with
			// relation, the eager can be define in this stage
			// if (dep.isEager() != null || dep.isHide() != null) {
			// error("Cannot set flags \"eager\" or \"hide\" on a relation "
			// + dep.getIdentifier());
			// }

			// Checking if the exception is existing
			String except = relation.getMissingException();
			if (except != null
					&& classpath.getElementsHavingClass(except) == null) {
				error("Exception " + except + " undefined in " + relation);
			}

			// Checking if the interface is existing
			if (! targetIsComponent(relation)) {
				checkResourceExists((ResourceReference)relation.getTarget());
			} else {

				// checking that the targetKind is not higher than the target
				ComponentKind targetKind = relation.getTargetKind() != null ? relation.getTargetKind() : ComponentKind.INSTANCE; 
				if (targetKind.isMoreAbstractThan(relation.getTarget().as(ComponentReference.class).getKind())) {
					error("TargetKind " + relation.getTargetKind()
							+ " is higher than the target " + relation.getTarget());
				}
			}
		}
	}

	
	private static RelationDeclaration getRelationDefinition(ApamCapability source, String relName) {
		// look for that relation declaration
		ApamCapability declaring = source;
		RelationDeclaration declaration = null;
		while (declaring != null && declaration == null) {
			declaration = declaring.getDeclaration().getRelation(relName);
			declaring 	= declaring.getGroup();
		}
		return declaration;
	}
	

	/**
	 * Provided a relation declaration, compute the effective relation, adding
	 * group constraint and flags. Compute which is the good target, and check
	 * the targets are compatible. If needed changes the target to set the more
	 * general one.
	 *
	 * @param depComponent
	 * @param relation
	 * @return
	 */
	public RelationDeclaration computeGroupRelation(ComponentDeclaration source, RelationDeclaration relation) {
		String relName = relation.getIdentifier();

		/*
		 * Iterate over all ancestor groups, and complete missing information in
		 * order to get the full relation declaration
		 */
		ApamCapability group = capability(source).getGroup();
		while (group != null) {

			RelationDeclaration inheritedRelation = declaration(group).getRelation(relName);

			/*
			 * skip group levels that do not refine the definition
			 */
			if (inheritedRelation == null) {
				group = group.getGroup();
				continue;
			}

			boolean relationTargetDefined	= !relation.getTarget().getName().equals(ComponentParser.UNDEFINED);
			boolean groupTargetdefined 		= !inheritedRelation.getTarget().getName().equals(ComponentParser.UNDEFINED);

			/*
			 * If the target are defined at several levels they must be
			 * compatible
			 */
			if (relationTargetDefined && groupTargetdefined) {

				/*
				 * The relation declared in the group targets a resource, the
				 * refined relation must target the same
				 */
				if (!targetIsComponent(relation) && !targetIsComponent(inheritedRelation)) {
					if (!relation.getTarget().equals(inheritedRelation.getTarget())) {
						error("Invalid target for " + relation
								+ " in component " + source.getName()
								+ " expected " + inheritedRelation.getTarget().getName()
								+ " as specified in " + group.getName());
					}
				}

				/*
				 * The relation declared in the group targets a component, the
				 * refinement may target a provided resource, but we keep the
				 * more general definition
				 */
				if (!targetIsComponent(relation) && targetIsComponent(inheritedRelation)) {
					ApamCapability inheritedTarget = target(inheritedRelation);
					if ( inheritedTarget != null && !inheritedTarget.provides(relation.getTarget().as(ResourceReference.class),true)) {
						error("Invalid target for " + relation
								+ " in component " + source.getName()
								+ " expected one of the provided resources of "
								+ inheritedRelation.getTarget().getName() + " as specified in "
								+ group.getName());
					}
				}

				/*
				 * The relation declared in the group targets a component, the
				 * refinement may target any component that is in the group of
				 * the target, but we keep the more general definition
				 */
				if (targetIsComponent(relation) && targetIsComponent(inheritedRelation)) {

					ApamCapability inheritedTarget	= target(inheritedRelation);
					ApamCapability relationTarget 	= target(relation);

					if (relationTarget != null && inheritedTarget != null && !inheritedTarget.isAncestorOf(relationTarget, true)) {
						error("Invalid target for " + relation
								+ " in component " + source.getName()
								+ " expected an member of "
								+ inheritedRelation.getTarget().getName() + " as specified in "
								+ group.getName());
					}
				}

				/*
				 * The relation declared in the group targets a resource, the
				 * refinement may target a component that provides this
				 * resource, but we keep the more general definition
				 */
				if (targetIsComponent(relation) && !targetIsComponent(inheritedRelation)) {
					ApamCapability relationTarget = target(relation);
					if ( relationTarget != null && !relationTarget.provides(inheritedRelation.getTarget().as(ResourceReference.class),true)) {
						error("Invalid target for " + relation
								+ " in component " + source.getName()
								+ " expected a component providing "
								+ inheritedRelation.getTarget().getName() + " as specified in "
								+ group.getName());
					}
				}

			}

			relation	= inheritedRelation.refinedBy(relation);
			group 		= group.getGroup();
		}

		return relation;
	}

	/**
	 * Provided a relation "dep" (simple or complex) checks if the field type
	 * and attribute multiple are compatible. For complex relation, for each
	 * field, checks if the target specification implements the field resource.
	 *
	 * @param dep
	 *            : a relation
	 */
	private void checkFieldTypeDep(RelationDeclaration relation) {
		// All field must have same multiplicity, and must refer to interfaces
		// and messages provided by the specification.

		// In the case of implementation dependencies, we allow the field to be
		// of the main class of the implementation

		Set<ResourceReference> allowedTypes = new HashSet<ResourceReference>();

		// possible if only the dep ID is provided. Target will be computed
		// later
		if (relation.getTarget() == null) {
			return;
		}

		// If not explicit component target, it must be an interface/message
		// reference
		if (!targetIsComponent(relation)) {
			allowedTypes.add(relation.getTarget().as(ResourceReference.class));
		} else {
			ComponentDeclaration target = declaration(target(relation));
            if(target != null ) {
                allowedTypes.addAll(target.getProvidedResources());

                // check target's implementation class
                if (target instanceof AtomicImplementationDeclaration) {
                	allowedTypes.add(new ImplementatioClassReference(((AtomicImplementationDeclaration)target).getClassName()));
                }

            } else {
				error("relation " + relation.getIdentifier() + " : the target of the reference doesn' exists " + relation.getTarget().getName());
			}


		}

		for (RequirerInstrumentation instrumentation : relation.getInstrumentations()) {

			if (!instrumentation.isValidInstrumentation())
				error(relation.getComponent().getName()	+ " : invalid type for field " + instrumentation.getName());

			ComponentKind targetKind = relation.getTargetKind() != null ? relation.getTargetKind() : ComponentKind.INSTANCE; 
			if (!(instrumentation.getRequiredResource() instanceof UnknownReference) &&
				!(targetKind.isAssignableTo(instrumentation.getRequiredResource().getJavaType())) && 
				!(allowedTypes.contains(instrumentation.getRequiredResource()))) {
				error("Field "
						+ instrumentation.getName()
						+ " is of type "
						+ instrumentation.getRequiredResource().getJavaType()
						+ " which is not implemented by specification or implementation "
						+ relation.getTarget().getName());
			}
		}

		/*
		 * TODO We also need to validate callback parameters
		 */
	}

	/**
	 * This class represents the main class of an implementation.
	 *
	 * TODO Right now this class is only used to verify the type of a field in
	 * an implementation relation. We could think of generalizing the concept
	 * of "interface relation" to "class relation" and allow APAM to resolve a
	 * field of a given class to an instance of some implementation that
	 * implements this class. In that case we can move this class to the core of
	 * APAM.
	 */
	private class ImplementatioClassReference extends ResourceReference {

		protected ImplementatioClassReference(String type) {
			super(type);
		}

		@Override
		public String toString() {
			return "class " + getIdentifier();
		}

	}

	/**
	 * Provided an atomic relation, returns if it is multiple or not. Checks if
	 * the same field is declared twice.
	 *
	 * @param dep
	 * @param component
	 * @return
	 */
	public boolean isFieldMultiple(RequirerInstrumentation dep,
			ComponentDeclaration component) {
		if (allFields.contains(dep.getName())
				&& !dep.getName().equals(UNDEFINED)) {
			error(component,"field/method "	+ dep.getName() + " allready declared");
		} else {
			allFields.add(dep.getName());
		}

		return dep.acceptMultipleProviders();
	}

	/**
	 * Checks if the component characteristics : shared, exclusive,
	 * instantiable, singleton, when explicitly defined, are not in
	 * contradiction with the group definition.
	 *
	 * @param component
	 */
	public void checkComponentHeader(ComponentDeclaration component) {
		ApamCapability cap = capability(component);
		if (cap == null)
			return;
		ApamCapability group = cap.getGroup();

		while (group != null) {
			if (cap.shared() != null && group.shared() != null
					&& (cap.shared() != group.shared())) {
				error("The \"shared\" property is incompatible with the value declared in "
						+ group.getName());
			}
			if (cap.instantiable() != null && group.instantiable() != null
					&& (cap.instantiable() != group.instantiable())) {
				error("The \"Instantiable\" property is incompatible with the value declared in "
						+ group.getName());
			}
			if (cap.singleton() != null && group.singleton() != null
					&& (cap.singleton() != group.singleton())) {
				error("The \"Singleton\" property is incompatible with the value declared in "
						+ group.getName());
			}
			group = group.getGroup();
		}
	}

	/**
	 * check all the characteristics that can be found in the <contentMngt> of a
	 * composite
	 *
	 * @param component
	 */
	public void checkCompositeContent(CompositeDeclaration component) {

		checkStart(component);
		checkState(component);
		checkOwn(component); // and grant
		checkVisibility(component);
		checkContextualDependencies(component);
		checkPromote(component);
	}

	/**
	 * Check the start characteristic. It is very similar to an instance
	 * declaration, plus a trigger.
	 *
	 * @param component
	 */
	private void checkStart(CompositeDeclaration component) {
		for (InstanceDeclaration start : component.getInstanceDeclarations()) {
			ComponentReference<?> implRef = start.getImplementation();
			if (implRef == null) {
				error("Implementation name cannot be null");
				continue;
			}
			ApamCapability cap = broker.getByReference(implRef);
			if (cap == null) {
				continue;
			}
			for (String attr : start.getProperties().keySet()) {
				String defAttr = getDefAttr(cap, attr);
				if (defAttr == null
						|| Attribute.checkAttrType(attr, start.getProperties()
								.get(attr), defAttr) == null) {
					error("invalid attribute " + attr + " = "
							+ start.getProperties().get(attr));
				}
			}

			checkRelations(start);

			checkTrigger(start);
		}
	}

	private boolean checkFilters(ComponentDeclaration component,	Set<String> filters, List<String> listFilters, Map<String, String> validAttr, String comp) {
		if (filters != null) {
			for (String f : filters) {
				ApamFilter parsedFilter = ApamFilter.newInstance(f);
				if (parsedFilter == null || !checkFilter(parsedFilter, component, validAttr, f, comp)) {
					return false;
				}
			}
		}
		if (listFilters != null) {
			for (String f : listFilters) {
				ApamFilter parsedFilter = ApamFilter.newInstance(f);
				if (parsedFilter == null || !checkFilter(parsedFilter, component, validAttr, f, comp)) {
					return false;
				}
			}
		}
		return true;
	}

	private void checkTrigger(InstanceDeclaration start) {
		for (ConstrainedReference trigger : start.getTriggers()) {

			if (trigger.getTarget().as(ComponentReference.class) == null) {
				error("Start trigger not related to a valid component");
				continue;
			}
			
			ApamCapability target = broker.getTargetComponent(trigger);
			if (target == null) {
				// error ("Unknown component " + target.getName()) ;
				continue;
			}

			Map<String, String> validAttrs = target.getValidAttrNames();

			checkFilters(start, trigger.getImplementationConstraints(), trigger.getImplementationPreferences(), 
					validAttrs, trigger.getTarget().getName());
			
			checkFilters(start, trigger.getInstanceConstraints(),trigger.getInstancePreferences(),
					validAttrs, trigger.getTarget().getName());
		}
	}

	/**
	 * Check the state characteristic. <own specification="Door"
	 * property="location" value="{entrance, exit}">
	 *
	 * @param component
	 */
	private Set<String> checkState(CompositeDeclaration component) {
		PropertyDefinition.Reference stateProperty = component.getStateProperty();
		
		if (stateProperty == null) {
			return null;
		}

		if (! stateProperty.getDeclaringComponent().getKind().equals(ComponentKind.IMPLEMENTATION)) {
			error("A state must be associated with an implementation.");
			return null;
		}
		
		ApamCapability implementation = broker.getDeclaringComponent(stateProperty);
		if (implementation == null) {
			error("Implementation for state unavailable: " + stateProperty.getDeclaringComponent().getName());
			return null;
		}
		// Attribute state must be defined on the implementation.
		String type = implementation.getLocalAttrDefinition(stateProperty.getIdentifier());
		if (type == null) {
			error("The state attribute " + stateProperty.getIdentifier()
					+ " on implementation " + stateProperty.getDeclaringComponent().getName()
					+ " is undefined.");
			return null;
		}

		Set<String> values = Util.splitSet(type);
		if (values.isEmpty()) {
			error("State attribute " + stateProperty.getIdentifier()
					+ " is not an enumeration. Invalid state attribute");
			return null;
		}
		return values;
	}

	private boolean visibilityExpression(String expr) {
		if (expr == null) {
			return true;
		}

		if (expr.equals(CST.V_FALSE) || expr.equals(CST.V_TRUE)) {
			return true;
		}

		return checkSyntaxFilter(expr);
	}

	private void checkVisibility(CompositeDeclaration component) {
		VisibilityDeclaration visiDcl = component.getVisibility();
		if (!visibilityExpression(visiDcl.getApplicationInstances())) {
			error("bad expression in ExportApp visibility: "
					+ visiDcl.getApplicationInstances());
		}
		if (!visibilityExpression(visiDcl.getExportImplementations())) {
			error("bad expression in Export implementation visibility: "
					+ visiDcl.getExportImplementations());
		}
		if (!visibilityExpression(visiDcl.getExportInstances())) {
			error("bad expression in Export instance visibility: "
					+ visiDcl.getExportInstances());
		}
		if (!visibilityExpression(visiDcl.getImportImplementations())) {
			error("bad expression in Imports implementation visibility: "
					+ visiDcl.getImportImplementations());
		}
		if (!visibilityExpression(visiDcl.getImportInstances())) {
			error("bad expression in Imports instance visibility: "
					+ visiDcl.getImportInstances());
		}
	}

	/**
	 *
	 * @param component
	 */
	private void checkOwn(CompositeDeclaration composite) {

		// The composite must be a singleton to define owns
		if (!composite.getOwnedComponents().isEmpty() && !composite.isSingleton()) {
			error(composite,"To define \"own\" clauses, composite must be a singleton.");
		}


		// check that a single own clause is defined for a component and its
		// members
		Set<String> compRef = new HashSet<String>();
		for (OwnedComponentDeclaration ownDeclaration : composite.getOwnedComponents()) {
			
			ApamCapability owned = broker.getByReference(ownDeclaration.getComponent());
			if (owned == null) {
				error("Unknown component in own expression : " + ownDeclaration.getComponent().getName());
				continue;
			}

			if (!ownDeclaration.getComponent().getKind().equals(owned.getReference().getKind())) {
				error("Component in own expression is of the wrong type, expecting "
						+ ownDeclaration.getComponent() + " found " + owned.getReference());
				continue;

			}

			// computes the attributes that can be associated with this spec or
			// implementations members
			if (ownDeclaration.getProperty() != null) {
				String prop = ownDeclaration.getProperty().getIdentifier();
				String type = owned.getAttrDefinition(prop);
				if (type == null) {
					error("Undefined attribute "
							+ ownDeclaration.getProperty().getIdentifier()
							+ " for component " + ownDeclaration.getComponent().getName()
							+ " in own expression");
					continue;
				}
				Set<String> values = Util.splitSet(type);
				if (values.size() == 1) {
					error("Attribute "
							+ ownDeclaration.getProperty().getIdentifier()
							+ " for component "
							+ ownDeclaration.getComponent().getName()
							+ " is not an enumeration. Invalid in own expression");
					continue;
				}

				if (ownDeclaration.getValues().isEmpty()) {
					error("In own clause, values not specified for attribute "
							+ prop + " \n    for component "
							+ ownDeclaration.getComponent().getName() + ". Expected "
							+ Util.toStringResources(values));
				}

				if (!values.containsAll(ownDeclaration.getValues())) {
					error("In own clause, invalid values : "
							+ Util.toStringResources(ownDeclaration.getValues())
							+ " for attribute " + prop
							+ " \n    for component "
							+ ownDeclaration.getComponent().getName() + ". Expected "
							+ Util.toStringResources(values));
				}
			}

			/**
			 * Check that a single own clause applies for the same component,
			 * and members At execution must also be checked that if other grant
			 * clauses in other composites for that component or its members:
			 * -It must be the same property -It must be different values
			 */
			if (compRef.contains(ownDeclaration.getComponent().getName())) {
				error("Another Own clause exists for "
						+ ownDeclaration.getComponent().getName()
						+ " in this composite declaration");
				continue;
			}
			compRef.add(ownDeclaration.getComponent().getName());
			if (owned.getGroup() != null) {
				compRef.add(owned.getGroup().getName());
			}

			checkGrant(composite, owned, ownDeclaration);
		}
	}

	private void checkGrant(CompositeDeclaration composite, ApamCapability owned, OwnedComponentDeclaration ownDeclaration) {

		// Get state definition
		Set<String> stateDefinition = checkState(composite);
		if (stateDefinition == null || stateDefinition.isEmpty()) { // No valid
			// state declaration.
			// No valid grants.

			if (!ownDeclaration.getGrants().isEmpty()) {
				error(composite,"Error in grant expression, state is not defined in component");
			}

			return;
		}

		
		
		// List<GrantDeclaration> grants = own.getGrants();
		for (GrantDeclaration grantDeclaration : ownDeclaration.getGrants()) {
			ApamCapability granted	= broker.getDeclaringComponent(grantDeclaration.getRelation());

			// Check that the granted component exists
			if (granted == null) {
				error("Unknown component "
						+ grantDeclaration.getRelation().getDeclaringComponent().getName()
						+ " in grant expression : " + grantDeclaration);
				continue;
			}

			// Check that the component is a singleton
			if (granted.singleton() != null && granted.singleton()) {
				warning("In Grant clause, Component "
						+ granted.getName() + " is not a singleton");
			}

			// Check that grant state values are valid
			Set<String> grantStates = grantDeclaration.getStates();
			if (!stateDefinition.containsAll(grantStates)) {
				error("In Grant expression, invalid values "
						+ Util.toStringResources(grantStates)
						+ " for state="
						+ Util.toStringResources(stateDefinition));
			}

			// Check that a single grant for a given state.
			for (String def : grantStates) {
				String completedef = composite.getStateProperty().getIdentifier() + def;
				if (allGrants.contains(completedef)) {
					error("Component " + owned.getName()
							+ " already granted when state is " + def);
					continue;
				}
				allGrants.add(completedef);
			}

			// Check that the relation exists and has as target the OWN resource
			// OWN is a specification or an implem but the granted relation can be anything
			
			RelationDeclaration  grantedRelation = declaration(granted).getRelation(grantDeclaration.getRelation().getIdentifier());

			if (grantedRelation == null) {
				error("The relation id of the grant clause " + grantDeclaration	+ " is undefined for component " + granted.getName());
			}
			
			boolean ownedSatisfiesGrantedRelation = true;
			if (grantedRelation != null && targetIsComponent(grantedRelation)) {
				ApamCapability grantedTarget = target(grantedRelation);
				ownedSatisfiesGrantedRelation = owned.isAncestorOf(grantedTarget, true);
			}

			if (grantedRelation != null && !targetIsComponent(grantedRelation)) {
				ownedSatisfiesGrantedRelation = owned.provides(grantedRelation.getTarget().as(ResourceReference.class),true);
			}				

			if (! ownedSatisfiesGrantedRelation) {
				// This id does not lead to the owned component
				error("The relation of the grant clause " + grantDeclaration +
					  " does not refers to the owned component " + owned.getName());
			}
			
		}
	}

	/**
	 * Cannot check almost nothing ! Because of wild cards, components are not
	 * known, and their attribute and dependencies cannot be checked. Only the
	 * syntax of filters can be checked. the exceptions
	 *
	 * @param component
	 */
	private void checkContextualDependencies(
			CompositeDeclaration component) {
		for (RelationDeclaration pol : component.getContextualDependencies()) {

			for (String constraint : pol.getImplementationConstraints()) {
				checkSyntaxFilter(constraint);
			}
			for (String constraint : pol.getImplementationPreferences()) {
				checkSyntaxFilter(constraint);
			}
			for (String constraint : pol.getInstanceConstraints()) {
				checkSyntaxFilter(constraint);
			}
			for (String constraint : pol.getInstancePreferences()) {
				checkSyntaxFilter(constraint);
			}

			// Checking if the exception is existing
			String except = pol.getMissingException();
			if (except != null
					&& classpath.getElementsHavingClass(except) == null) {
				error("Exception " + except + " undefined in " + pol);
			}
		}
	}

	/**
	 * Cannot check if the component relation is valid. Only checks that the
	 * composite relation is declared, and that the component is known.
	 *
	 * @param composite
	 */
	private void checkPromote(CompositeDeclaration composite) {
		if (composite.getPromotions() == null)
			return;

		for (RelationPromotion promotion : composite.getPromotions()) {
			ApamCapability promotionSource = broker.getDeclaringComponent(promotion.getContentRelation());
			if (promotionSource == null) {
				error("Invalid promotion: unknown component "
						+ promotion.getContentRelation().getDeclaringComponent().getName());
			}
			
			RelationDeclaration promotedRelation = declaration(promotionSource).getRelation(promotion.getContentRelation());
			// Check if the dependencies are compatible
			if (promotedRelation == null) {
				error("Component " + promotionSource.getName()
						+ " does not define a relation with id="+ promotion.getContentRelation().getIdentifier());
				continue;
			}

			RelationDeclaration compositeRelation = composite.getRelation(promotion.getCompositeRelation());
			if (compositeRelation == null) {
				error("Undefined composite relation: "+ promotion.getCompositeRelation().getIdentifier());
				continue;
			}

			// Both the composite and the component have a relation with the
			// right id.
			// Check if the targets are compatible
			if (!checkRelationMatch(promotedRelation, compositeRelation)) {
				error("relation " + promotedRelation
						+ " does not match the composite relation "	+ compositeRelation);
			}
		}
	}

	// Copy paste of the Util class ! too bad, this one uses ApamCapability
	private boolean checkRelationMatch(RelationDeclaration promotedRelation, RelationDeclaration compositeRelation) {

		boolean match = false;
		
		if (!targetIsComponent(promotedRelation) && !targetIsComponent(compositeRelation)) {
			match = compositeRelation.getTarget().equals(promotedRelation.getTarget());
		}

		if (!targetIsComponent(promotedRelation) && targetIsComponent(compositeRelation)) {
			ApamCapability compositeTarget = target(compositeRelation);
			match = compositeTarget.provides(promotedRelation.getTarget().as(ResourceReference.class),true);
		}

		if (targetIsComponent(promotedRelation) && targetIsComponent(compositeRelation)) {
			ApamCapability promotedTarget 	= target(promotedRelation);
			ApamCapability compositeTarget	= target(compositeRelation);
			
			match = promotedTarget.isAncestorOf(compositeTarget,true);
		}
		
		return  match && ( !promotedRelation.isMultiple() || compositeRelation.isMultiple());
	}
}
