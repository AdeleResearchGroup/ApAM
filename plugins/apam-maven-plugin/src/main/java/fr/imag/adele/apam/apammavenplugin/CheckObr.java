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

import fr.imag.adele.apam.AttrType;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.apammavenplugin.helpers.AttributeCheckHelpers;
import fr.imag.adele.apam.apammavenplugin.helpers.FilterCheckHelpers;
import fr.imag.adele.apam.apammavenplugin.helpers.ProvideHelpers;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration;
import fr.imag.adele.apam.declarations.AtomicImplementationDeclaration.CodeReflection;
import fr.imag.adele.apam.declarations.ComponentDeclaration;
import fr.imag.adele.apam.declarations.ComponentKind;
import fr.imag.adele.apam.declarations.ComponentReference;
import fr.imag.adele.apam.declarations.CompositeDeclaration;
import fr.imag.adele.apam.declarations.ConstrainedReference;
import fr.imag.adele.apam.declarations.GrantDeclaration;
import fr.imag.adele.apam.declarations.ImplementationDeclaration;
import fr.imag.adele.apam.declarations.ImplementationReference;
import fr.imag.adele.apam.declarations.InstanceDeclaration;
import fr.imag.adele.apam.declarations.InstanceReference;
import fr.imag.adele.apam.declarations.InterfaceReference;
import fr.imag.adele.apam.declarations.MessageReference;
import fr.imag.adele.apam.declarations.OwnedComponentDeclaration;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.declarations.RelationDeclaration;
import fr.imag.adele.apam.declarations.RelationPromotion;
import fr.imag.adele.apam.declarations.RequirerInstrumentation;
import fr.imag.adele.apam.declarations.ResolvableReference;
import fr.imag.adele.apam.declarations.ResourceReference;
import fr.imag.adele.apam.declarations.SpecificationDeclaration;
import fr.imag.adele.apam.declarations.SpecificationReference;
import fr.imag.adele.apam.declarations.UndefinedReference;
import fr.imag.adele.apam.declarations.VisibilityDeclaration;
import fr.imag.adele.apam.declarations.encoding.ipojo.ComponentParser;
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

	private final Set<String> ALL_FIELDS = new HashSet<String>();
	private final Set<String> ALL_GRANTS = new HashSet<String>();
	private final String IN = "In ";
	
	private final ApamCapabilityBroker broker;
	private final Log logger;
	
	private boolean failedChecking = false;

	/**
	 * Private constructor protects creating instances from this class (only
	 * static methods)
	 */
	public CheckObr(ApamCapabilityBroker broker, Log logger) {
		this.broker = broker;
		this.logger = logger;
	}

	public final boolean hasFailedChecking() {
		return failedChecking;
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
	 * Get the component associated with a Capability
	 * 
	 */
	private ComponentDeclaration component(ApamCapability capability) {
		return capability != null ? capability.getDeclaration() : null;
	}
	
	/**
	 * Checks if the constraints set on this relation are syntacticaly valid.
	 * Only for specification dependencies. Checks if the attributes mentioned
	 * in the constraints can be set on an implementation of that specification.
	 *
	 * @param dep
	 *            a relation
	 */
	private void checkConstraint(ComponentDeclaration component, RelationDeclaration dep) {

		if ((dep == null) || !(dep.getTarget() instanceof ComponentReference)) {
			return;
		}

		if (dep.isMultiple()
				&& (!dep.getImplementationPreferences().isEmpty() || !dep
						.getInstancePreferences().isEmpty())) {
			error("Preferences cannot be defined for a relation with multiple cardinality: "
					+ dep.getIdentifier());
		}

		// get the spec or impl definition
		ApamCapability cap = broker.get(dep.getTarget().as(ComponentReference.class));
		if (cap != null) {

			// computes the attributes that can be associated with this spec or
			// implementations members
			Map<String, String> validAttrs = cap.getValidAttrNames();

			checkFilters(component, dep.getImplementationConstraints(),
					dep.getImplementationPreferences(), validAttrs, dep
							.getTarget().getName());
			checkFilters(component, dep.getInstanceConstraints(),
					dep.getInstancePreferences(), validAttrs, dep.getTarget()
							.getName());
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

		ApamCapability entCap = broker.get(component.getReference());
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

		ApamCapability group = broker.get(component.getGroupReference());

		if (!AttributeCheckHelpers.checkPropertyDefinition(component, definition, group,this)) {
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

		ApamCapability source = broker.get(component.getName());
		if (!sub.sourceName.equals("this")) {
			// Look for the source component
			source = broker.get(sub.sourceName);
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
		
		SpecificationDeclaration specification = (SpecificationDeclaration) component(source);
		
		AtomicImplementationDeclaration bidon = new AtomicImplementationDeclaration("void-" + specification.getName(),
														specification.getReference().any(), null);
		return new ApamCapability(broker,bidon);
	}

	private ApamCapability buildDummyInst(ApamCapability source) {

		ImplementationDeclaration implementation = (ImplementationDeclaration) component(source);
		InstanceDeclaration bidon = new InstanceDeclaration(implementation.getReference().any(),
											source.getName() + "-01", null);
		return new ApamCapability(broker,bidon);
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

			ComponentReference<?> targetComponent = depDcl.getTarget().as(
					ComponentReference.class);
			if (targetComponent == null) { // it is an interface or message
				// target. Cannot check.
				warning(depDcl.getTarget().getName()
						+ " is an interface or message. Substitution \""
						+ defaultValue + "\" cannot be checked");
				return RUNTIME_COMPONET;
			}

			source = broker.get(targetComponent.getName());
			if (source == null) {
				error("Component " + targetComponent.getName()
						+ " not found in substitution : \"" + defaultValue
						+ "\"");
				return null;
			}
		}

		return source;

	}

	/**
	 * An implementation has the following provide; check if consistent with the
	 * list of provides found in "cap".
	 *
	 * @param interfaces
	 *            = "{I1, I2, I3}" or I1 or null
	 * @param messages
	 *            = "{M1, M2, M3}" or M1 or null
	 * @return
	 */
	public boolean checkImplProvide(ComponentDeclaration component,
			Set<InterfaceReference> interfaces,
			Set<MessageReference> messages,
			Set<UndefinedReference> interfacesUndefined,
			Set<UndefinedReference> messagesUndefined) {
		
		if (!(component instanceof AtomicImplementationDeclaration)) {
			return true;
		}
		AtomicImplementationDeclaration impl = (AtomicImplementationDeclaration) component;
		SpecificationReference.Versioned spec = impl.getSpecificationVersion();

		if (spec == null) {
			return true;
		}
		ApamCapability cap = broker.get(spec.getComponent().getName(), spec.getRange());
		if (cap == null) {
			return false;
		}

		Set<MessageReference> specMessages = cap.getProvideMessages();
		Set<InterfaceReference> specInterfaces = cap.getProvideInterfaces();

		ProvideHelpers.checkMessages(impl, messages, messagesUndefined,
				specMessages, this);

		ProvideHelpers.checkInterfaces(impl, interfaces, interfacesUndefined,
				specInterfaces, this);

		return true;
	}

	private Set<ResourceReference> getAllProvidedResources(ImplementationDeclaration implementation) {
		
		ApamCapability specification 	=  broker.get(implementation.getSpecificationVersion());
		Set<ResourceReference> provided = new HashSet<ResourceReference>();

		if (specification != null) {
			provided.addAll(specification.getProvideResources());
		}
		provided.addAll(implementation.getProvidedResources());
		
		return provided;
	}

	/**
	 * Get the provided resources of a given kind, for example Services or
	 * Messages.
	 *
	 * We use subclasses of ResourceReference as tags to identify kinds of
	 * resources. To add a new kind of resource a new subclass must be added.
	 *
	 * Notice that we return a set of resource references but typed to
	 * particular subtype of references, the unchecked downcast is then safe at
	 * runtime.
	 */
	@SuppressWarnings("unchecked")
	public <T extends ResourceReference> Set<T> getProvidedResources(
			Set<ResourceReference> resources, Class<T> kind) {
		Set<T> res = new HashSet<T>();
		for (ResourceReference resourceReference : resources) {
			if (kind.isInstance(resourceReference)) {
				res.add((T) resourceReference);
			}
		}
		return res;
	}

	public void checkCompoMain(CompositeDeclaration composite) {
		String name = composite.getName();

		Set<ResourceReference> allProvidedResources = getAllProvidedResources(composite);
		// Abstract composite have no main implem, but must not provide any
		// resource
		if (composite.getMainComponent() == null) {
			if (!getAllProvidedResources(composite).isEmpty()) {
				error("Composite "
						+ name
						+ " does not declare a main implementation, but provides resources "
						+ getAllProvidedResources(composite));
			}
			return;
		}

		String implName = composite.getMainComponent().getName();
		ApamCapability cap = broker.get(composite.getMainComponent());
		if (cap == null) {
			return;
		}
		if (composite.getSpecification() != null) {
			String spec = composite.getSpecification().getName();
			String capSpec = cap.getProperty(CST.PROVIDE_SPECIFICATION);
			if ((capSpec != null) && !spec.equals(capSpec)) {
				error(IN + name + " Invalid main implementation. "
						+ implName + " must implement specification " + spec);
			}
		}

		Set<MessageReference> mainMessages = cap.getProvideMessages();
		Set<MessageReference> compositeMessages = getProvidedResources(
				allProvidedResources, MessageReference.class);
		if (!mainMessages.containsAll(compositeMessages)) {
			error(IN + name + " Invalid main implementation. "
					+ implName + " produces messages " + mainMessages
					+ " instead of " + compositeMessages);
		}

		Set<InterfaceReference> mainInterfaces = cap.getProvideInterfaces();
		Set<InterfaceReference> compositeInterfaces = getProvidedResources(
				allProvidedResources, InterfaceReference.class);
		if (!mainInterfaces.containsAll(compositeInterfaces)) {
			error(IN + name + " Invalid main implementation. "
					+ implName + " implements " + mainInterfaces
					+ " instead of " + compositeInterfaces);
		}
	}

	/**
	 * Checks if a class or interface exists among the bundles read by Maven to
	 * compile. Note we cannot know
	 *
	 * @param interf
	 * @return
	 */
	public boolean checkInterfaceExist(String interf) {
		// Checking if the interface is existing
		// Can be "<Unavalable>" for generic collections, since it is not
		// possible to get the type at compile time
		if (interf == null || interf.startsWith("<Unavailable")) {
			return true;
		}
		if (OBRGeneratorMojo.classpathDescriptor.getElementsHavingClass(interf) == null) {
			error("Provided Interface " + interf
					+ " does not exist in your Maven dependencies");
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
		Set<RelationDeclaration> deps = component.getDependencies();
		if (deps == null || deps.isEmpty()) {
			return;
		}

		ALL_FIELDS.clear();
		Set<String> depIds = new HashSet<String>();

		for (RelationDeclaration dep : deps) {

			// Checking for predefined relations. Cannot be redefined
			if (CST.isFinalRelation(dep.getIdentifier())) {
				error("relation " + dep.getIdentifier()
						+ " is predefined.");
				continue;
			}

			// Checking for double relation Id
			if (depIds.contains(dep.getIdentifier())) {
				error("relation " + dep.getIdentifier()
						+ " allready defined.");
				continue;
			}
			depIds.add(dep.getIdentifier());

			// replace the definition by the effective relation (adding group
			// definition)
			dep = computeGroupRelation(component, dep);

			// validating relation constraints and preferences..
			checkConstraint(component, dep);

			// Checking fields and complex dependencies
			checkFieldTypeDep(dep);

			// eager and hide cannot be defined here
			// TODO relation, attention! this block was removed since now with
			// relation, the eager can be define in this stage
			// if (dep.isEager() != null || dep.isHide() != null) {
			// error("Cannot set flags \"eager\" or \"hide\" on a relation "
			// + dep.getIdentifier());
			// }

			// Checking if the exception is existing
			String except = dep.getMissingException();
			if (except != null
					&& OBRGeneratorMojo.classpathDescriptor
							.getElementsHavingClass(except) == null) {
				error("Exception " + except + " undefined in " + dep);
			}

			// Checking if the interface is existing
			if (dep.getTarget() instanceof InterfaceReference
					|| dep.getTarget() instanceof MessageReference) {
				checkInterfaceExist(dep.getTarget().getName());
			} else {

				// checking that the targetKind is not higher than the target
				if ((dep.getTarget() instanceof ImplementationReference && dep
						.getTargetKind() == ComponentKind.SPECIFICATION)
						|| (dep.getTarget() instanceof InstanceReference && dep
								.getTargetKind() != ComponentKind.INSTANCE)) {
					error("TargetKind " + dep.getTargetKind()
							+ " is higher than the target " + dep.getTarget());
				}
			}
		}
	}

	
	public RelationDeclaration getRelationDefinition(ApamCapability source, String relName) {
		// look for that relation declaration
		// ComponentDeclaration group =
		// ApamCapability.getDcl(depComponent.getGroupReference()) ;
		ApamCapability group = source;
		RelationDeclaration relDef = null;
		while (group != null && relDef == null) {
			relDef = group.getDeclaration().getLocalRelation(relName);
			group = group.getGroup();
		}
		return relDef;
	}
	
    public ComponentDeclaration getGroup(ComponentDeclaration component) {


        /*
         * handle versioned group references
         */
        ComponentReference<?>.Versioned group	= null;
        
        if(component instanceof InstanceDeclaration) {
        	group = ((InstanceDeclaration) component).getImplementationVersion();
        } 
        else if (component instanceof ImplementationDeclaration) {
        	group = ((ImplementationDeclaration) component).getSpecificationVersion();
        }
        else if (component instanceof SpecificationDeclaration) {
        	group = null;
        }
            
        return group != null ? broker.get(group).getDeclaration() : null;
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
		ComponentDeclaration group = getGroup(source);
		while (group != null) {

			RelationDeclaration groupDep = group.getLocalRelation(relName);

			/*
			 * skip group levels that do not refine the definition
			 */
			if (groupDep == null) {
				group = getGroup(group);
				continue;
			}

			boolean relationTargetDefined = !relation.getTarget().getName()
					.equals(ComponentParser.UNDEFINED);
			boolean groupTargetdefined = !groupDep.getTarget().getName()
					.equals(ComponentParser.UNDEFINED);

			/*
			 * If the target are defined at several levels they must be
			 * compatible
			 */
			if (relationTargetDefined && groupTargetdefined) {

				ResourceReference relationTargetResource = relation.getTarget().as(ResourceReference.class);
				ComponentReference<?> relationTargetComponent = relation.getTarget().as(ComponentReference.class);

				ResourceReference groupTargetResource = groupDep.getTarget().as(ResourceReference.class);
				ComponentReference<?> groupTargetComponent = groupDep.getTarget().as(ComponentReference.class);

				/*
				 * The relation declared in the group targets a resource, the
				 * refined relation must target the same
				 */
				if (relationTargetResource != null
						&& groupTargetResource != null) {
					if (!relationTargetResource.equals(groupTargetResource)) {
						error("Invalid target for " + relation
								+ " in component " + source.getName()
								+ " expected " + groupTargetResource
								+ " as specified in " + group.getName());
					}
				}

				/*
				 * The relation declared in the group targets a component, the
				 * refinement may target a provided resource, but we keep the
				 * more general definition
				 */
				if (relationTargetResource != null
						&& groupTargetComponent != null) {

					ComponentDeclaration groupTarget = broker.get(groupTargetComponent).getDeclaration();
					if (!groupTarget.getProvidedResources().contains(relationTargetResource)) {
						error("Invalid target for " + relation
								+ " in component " + source.getName()
								+ " expected one of the provided resources of "
								+ groupTargetComponent + " as specified in "
								+ group.getName());
					}
				}

				/*
				 * The relation declared in the group targets a component, the
				 * refinement may target any component that is in the group of
				 * the target, but we keep the more general definition
				 */
				if (relationTargetComponent != null
						&& groupTargetComponent != null) {

					ComponentDeclaration groupTarget = broker.get(groupTargetComponent).getDeclaration();
					ComponentDeclaration relationTarget = broker.get(relationTargetComponent).getDeclaration();

					while (relationTarget != null
							&& !relationTarget.getReference().equals(groupTarget.getReference()))
						relationTarget = getGroup(relationTarget);

					if (relationTarget == null) {
						error("Invalid target for " + relation
								+ " in component " + source.getName()
								+ " expected an member of "
								+ groupTargetComponent + " as specified in "
								+ group.getName());
					}
				}

				/*
				 * The relation declared in the group targets a resource, the
				 * refinement may target a component that provides this
				 * resource, but we keep the more general definition
				 */
				if (relationTargetComponent != null
						&& groupTargetResource != null) {
					ComponentDeclaration relationTarget = broker.get(relationTargetComponent).getDeclaration();
					if (!relationTarget.getProvidedResources().contains(
							groupTargetResource)) {
						error("Invalid target for " + relation
								+ " in component " + source.getName()
								+ " expected a component providing "
								+ groupTargetResource + " as specified in "
								+ group.getName());
					}
				}

			}

			relation = groupDep.refinedBy(relation);
			group = getGroup(group);
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
	private void checkFieldTypeDep(RelationDeclaration dep) {
		// All field must have same multiplicity, and must refer to interfaces
		// and messages provided by the specification.

		// In the case of implementation dependencies, we allow the field to be
		// of the main class of the implementation

		Set<ResourceReference> allowedTypes = new HashSet<ResourceReference>();

		// possible if only the dep ID is provided. Target will be computed
		// later
		if (dep.getTarget() == null) {
			return;
		}

		ComponentReference<?> targetComponent = dep.getTarget().as(
				ComponentReference.class);

		// If not explicit component target, it must be an interface/message
		// reference
		if (targetComponent == null) {
			allowedTypes.add(dep.getTarget().as(ResourceReference.class));
		} else {
			ApamCapability cap = broker.get(targetComponent);
            if(cap != null ) {
                allowedTypes.addAll(cap.getProvideResources());

                // check target's implementation class
                String implementationClass = cap.getImplementationClass();
                if (implementationClass != null)
                    allowedTypes.add(new ImplementatioClassReference(
                            implementationClass));

            } else {
				error("relation " + dep.getIdentifier()
						+ " : the target of the reference doesn' exists "
						+ targetComponent);
			}


		}

		for (RequirerInstrumentation innerDep : dep.getInstrumentations()) {

			if (!innerDep.isValidInstrumentation())
				error(dep.getComponent().getName()
						+ " : invalid type for field " + innerDep.getName());

			String type = innerDep.getRequiredResource().getJavaType();
			if (!(type.startsWith("fr.imag.adele.apam.")) // For links
					&& !(innerDep.getRequiredResource() instanceof UndefinedReference)
					&& !(allowedTypes.contains(innerDep.getRequiredResource()))) {
				error("Field "
						+ innerDep.getName()
						+ " is of type "
						+ type
						+ " which is not implemented by specification or implementation "
						+ dep.getTarget().getName());
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
	 * an implementation dependnecy. We could think of generalizing the concept
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
		if (ALL_FIELDS.contains(dep.getName())
				&& !dep.getName().equals(UNDEFINED)) {
			error(IN + component.getName() + " field/method "
					+ dep.getName() + " allready declared");
		} else {
			ALL_FIELDS.add(dep.getName());
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
		ApamCapability cap = broker.get(component.getReference());
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
			ImplementationReference<?> implRef = start.getImplementation();
			if (implRef == null) {
				error("Implementation name cannot be null");
				continue;
			}
			ApamCapability cap = broker.get(implRef);
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
		Set<ConstrainedReference> trig = start.getTriggers();
		for (ConstrainedReference ref : trig) {
			ResolvableReference target = ref.getTarget();
			ComponentReference<?> compoRef = target
					.as(ComponentReference.class);
			if (compoRef == null) {
				error("Start trigger not related to a valid component");
				continue;
			}
			ApamCapability cap = broker.get(compoRef);
			if (cap == null) {
				// error ("Unknown component " + target.getName()) ;
				continue;
			}

			Map<String, String> validAttrs = cap.getValidAttrNames();

			checkFilters(start, ref.getImplementationConstraints(),
					ref.getImplementationPreferences(), validAttrs, ref
							.getTarget().getName());
			checkFilters(start, ref.getInstanceConstraints(),
					ref.getInstancePreferences(), validAttrs, ref.getTarget()
							.getName());
		}
	}

	/**
	 * Check the state characteristic. <own specification="Door"
	 * property="location" value="{entrance, exit}">
	 *
	 * @param component
	 */
	private Set<String> checkState(CompositeDeclaration component) {
		PropertyDefinition.Reference ref = component.getStateProperty();
		if (ref == null) {
			return null;
		}

		ComponentReference<?> compo = ref.getDeclaringComponent();
		if (!(compo instanceof ImplementationReference)) {
			error("A state must be associated with an implementation.");
			return null;
		}
		ApamCapability implCap = broker.get(compo);
		if (implCap == null) {
			error("Implementation for state unavailable: " + compo.getName());
			return null;
		}
		// Attribute state must be defined on the implementation.
		String type = implCap.getLocalAttrDefinition(ref.getIdentifier());
		if (type == null) {
			error("The state attribute " + ref.getIdentifier()
					+ " on implementation " + compo.getName()
					+ " is undefined.");
			return null;
		}

		Set<String> values = Util.splitSet(type);
		if (values.isEmpty()) {
			error("State attribute " + ref.getIdentifier()
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
	private void checkOwn(CompositeDeclaration component) {
		Set<OwnedComponentDeclaration> owned = component.getOwnedComponents();

		if (owned.isEmpty()) {
			return;
		}

		// The composite must be a singleton
		if (!component.isSingleton()) {
			error("To define \"own\" clauses, composite "
					+ component.getName() + " must be a singleton.");
		}
		// check that a single own clause is defined for a component and its
		// members
		Set<String> compRef = new HashSet<String>();
		for (OwnedComponentDeclaration own : owned) {
			ApamCapability ownCap = broker.get(own.getComponent());
			if (ownCap == null) {
				error("Unknown component in own expression : "
						+ own.getComponent().getName());
				continue;
			}

			ComponentReference<?> foundReference = broker.get(own.getComponent()).getReference();
			if (!own.getComponent().getClass().isAssignableFrom(foundReference.getClass())) {
				error("Component in own expression is of the wrong type, expecting "
						+ own.getComponent() + " found " + foundReference);
				continue;

			}

			// computes the attributes that can be associated with this spec or
			// implementations members
			if (own.getProperty() != null) {
				String prop = own.getProperty().getIdentifier();
				String type = ownCap.getAttrDefinition(prop);
				if (type == null) {
					error("Undefined attribute "
							+ own.getProperty().getIdentifier()
							+ " for component " + own.getComponent().getName()
							+ " in own expression");
					continue;
				}
				Set<String> values = Util.splitSet(type);
				if (values.size() == 1) {
					error("Attribute "
							+ own.getProperty().getIdentifier()
							+ " for component "
							+ own.getComponent().getName()
							+ " is not an enumeration. Invalid in own expression");
					continue;
				}

				if (own.getValues().isEmpty()) {
					error("In own clause, values not specified for attribute "
							+ prop + " \n    for component "
							+ own.getComponent().getName() + ". Expected "
							+ Util.toStringResources(values));
				}

				if (!values.containsAll(own.getValues())) {
					error("In own clause, invalid values : "
							+ Util.toStringResources(own.getValues())
							+ " for attribute " + prop
							+ " \n    for component "
							+ own.getComponent().getName() + ". Expected "
							+ Util.toStringResources(values));
				}
			}

			/**
			 * Check that a single own clause applies for the same component,
			 * and members At execution must also be checked that if other grant
			 * clauses in other composites for that component or its members:
			 * -It must be the same property -It must be different values
			 */
			if (compRef.contains(own.getComponent().getName())) {
				error("Another Own clause exists for "
						+ own.getComponent().getName()
						+ " in this composite declaration");
				continue;
			}
			compRef.add(own.getComponent().getName());
			if (ownCap.getGroup() != null) {
				compRef.add(ownCap.getGroup().getName());
			}

			checkGrant(component, own);
		}
	}

	private void checkGrant(CompositeDeclaration component,
			OwnedComponentDeclaration own) {
		// Get state definition
		Set<String> stateDefinition = checkState(component);
		if (stateDefinition == null || stateDefinition.isEmpty()) { // No valid
			// state declaration.
			// No valid grants.

			if (!own.getGrants().isEmpty()) {
				error("In Grant expression, state is not defined in component "
						+ component.getName());
			}

			return;
		}

		// List<GrantDeclaration> grants = own.getGrants();
		for (GrantDeclaration grant : own.getGrants()) {
			ApamCapability grantComponent	= broker.get(grant.getRelation().getDeclaringComponent());
			ApamCapability ownedComp 		= broker.get(own.getComponent());

			// Check that the granted component exists
			if (grantComponent == null) {
				error("Unknown component "
						+ grant.getRelation().getDeclaringComponent().getName()
						+ " in grant expression : " + grant);
				continue;
			}

			// Check that the component is a singleton
			if (!CST.SINGLETON
					.equals(grantComponent.getProperty(CST.SINGLETON))) {
				warning("In Grant clause, Component "
						+ grantComponent.getName() + " is not a singleton");
			}

			// Check that grant state values are valid
			Set<String> grantStates = grant.getStates();
			if (!stateDefinition.containsAll(grant.getStates())) {
				error("In Grant expression, invalid values "
						+ Util.toStringResources(grant.getStates())
						+ " for state="
						+ Util.toStringResources(stateDefinition));
			}

			// Check that a single grant for a given state.
			for (String def : grantStates) {
				String completedef = component.getStateProperty()
						.getIdentifier() + def;
				if (ALL_GRANTS.contains(completedef)) {
					error("Component " + own.getComponent().getName()
							+ " already granted when state is " + def);
					continue;
				}
				ALL_GRANTS.add(completedef);
			}

			// Check that the relation exists and has as target the OWN
			// resource
			ComponentDeclaration granted = grantComponent.getDeclaration();
			String id = grant.getRelation().getIdentifier();
			ComponentReference<?> owned = own.getComponent();
			boolean found = false;
			// OWN is a specification or an implem
			// granted dep can be anything
			for (RelationDeclaration depend : granted.getDependencies()) {
				if (depend.getIdentifier().equals(id)) {
					found = true;

					// Check if the relation leads to the OWNed component
					if (depend.getTarget().getClass().equals(owned.getClass())
					/* same type spec or implem */
					&& (depend.getTarget().getName().equals(owned.getName()))) {
						break;
					}

					// If the relation is an implem check if its spec is the
					// owned one
					if (depend.getTarget() instanceof ImplementationReference) {
						ApamCapability depSpec = broker.get((ImplementationReference<?>) depend.getTarget());
						if (depSpec != null
								&& depSpec.getGroup().equals(owned.getName())) {
							break;
						}
					}

					// Check if the relation resource are provided by the
					// owned component
					if ((depend.getTarget() instanceof ResourceReference)
							&& (ownedComp.getDeclaration().getProvidedResources()
									.contains(depend.getTarget()))) {
						break;
					}

					// This id does not lead to the owned component
					error("The relation of the grant clause " + grant
							+ " does not refers to the owned component "
							+ owned);
				}
			}
			if (!found) {
				error("The relation id of the grant clause " + grant
						+ " is undefined for component "
						+ grant.getRelation().getDeclaringComponent().getName());
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
					&& OBRGeneratorMojo.classpathDescriptor
							.getElementsHavingClass(except) == null) {
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

		for (RelationPromotion promo : composite.getPromotions()) {
			ComponentDeclaration internalComp = broker.get(promo.getContentRelation().getDeclaringComponent()).getDeclaration();
			if (internalComp == null) {
				error("Invalid promotion: unknown component "
						+ promo.getContentRelation().getDeclaringComponent()
								.getName());
			}
			RelationDeclaration internalDep = null;
			if (internalComp.getDependencies() != null)
				for (RelationDeclaration intDep : internalComp
						.getDependencies()) {
					if (intDep.getIdentifier().equals(
							promo.getContentRelation().getIdentifier())) {
						internalDep = intDep;
						break;
					}
				}
			// Check if the dependencies are compatible
			if (internalDep == null) {
				error("Component " + internalComp.getName()
						+ " does not define a relation with id="
						+ promo.getContentRelation().getIdentifier());
				continue;
			}

			RelationDeclaration compositeDep = null;
			if (composite.getDependencies() != null)
				for (RelationDeclaration dep : composite.getDependencies()) {
					if (dep.getIdentifier().equals(
							promo.getCompositeRelation().getIdentifier())) {
						compositeDep = dep;
						break;
					}
				}
			if (compositeDep == null) {
				error("Undefined composite relation: "
						+ promo.getCompositeRelation().getIdentifier());
				continue;
			}

			// Both the composite and the component have a relation with the
			// right id.
			// Check if the targets are compatible
			if (!checkRelationMatch(internalDep, compositeDep)) {
				error("relation " + internalDep
						+ " does not match the composite relation "
						+ compositeDep);
			}
		}
	}

	// Copy paste of the Util class ! too bad, this one uses ApamCapability
	private boolean checkRelationMatch(RelationDeclaration clientDep,
			RelationDeclaration compoDep) {
		boolean multiple = clientDep.isMultiple();
		// Look for same relation: the same specification, the same
		// implementation or same resource name
		// Constraints are not taken into account

		if (compoDep.getTarget().getClass()
				.equals(clientDep.getTarget().getClass())) { // same nature
			if (compoDep.getTarget().equals(clientDep.getTarget())) {
				if (!multiple || compoDep.isMultiple()) {
					return true;
				}
			}
		}

		// Look for a compatible relation.
		// Stop at the first relation matching only based on same name (same
		// resource or same component)
		// No provision for : cardinality, constraints or characteristics
		// (missing, eager)
		// for (relationDeclaration compoDep : compoDeps) {
		// Look if the client requires one of the resources provided by the
		// specification
		if (compoDep.getTarget() instanceof SpecificationReference) {
			SpecificationDeclaration spec = (SpecificationDeclaration) broker.get(((SpecificationReference) compoDep.getTarget())).getDeclaration();

			if ((spec != null)
					&& spec.getProvidedResources().contains(
							clientDep.getTarget())
					&& (!multiple || compoDep.isMultiple())) {
				return true;
			}
		} else {
			// If the composite has a relation toward an implementation
			// and the client requires a resource provided by that
			// implementation
			if (compoDep.getTarget() instanceof ImplementationReference) {
				ImplementationDeclaration impl = (ImplementationDeclaration) broker.get(((ImplementationReference<?>) compoDep.getTarget())).getDeclaration();
				if (impl != null) {
					// The client requires the specification implemented by that
					// implementation
					if (clientDep.getTarget() instanceof SpecificationReference) {
						String clientReqSpec = ((SpecificationReference) clientDep
								.getTarget()).getName();
						SpecificationReference spec = impl.getSpecification();
						if ( spec!= null && spec.getName().equals(clientReqSpec)
								&& (!multiple || compoDep.isMultiple())) {
							return true;
						}
					} else {
						// The client requires a resource provided by that
						// implementation
						if (impl.getProvidedResources().contains(
								clientDep.getTarget())
								&& (!multiple || compoDep.isMultiple())) {
							return true;
						}
					}
				}
			}
		}
		return false;

	}
}
