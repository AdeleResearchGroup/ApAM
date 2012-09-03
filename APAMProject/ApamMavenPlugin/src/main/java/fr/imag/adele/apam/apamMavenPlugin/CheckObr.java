package fr.imag.adele.apam.apamMavenPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.DataModelHelper;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.DataModelHelperImpl;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.ComponentReference;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.DependencyInjection;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.MessageReference;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.impl.ComponentImpl;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.OBR;
import fr.imag.adele.apam.util.Util;

public class CheckObr {

	private static DataModelHelper dataModelHelper; 
	private static Repository repo;
	private static Resource[]     resources;

	private static final Map<String, Capability> readCapabilities        = new HashMap<String, Capability>();
	private static final Set<String>             allFields         = new HashSet<String>();

	private static boolean                       failedChecking = false;

	/**
	 * An string value that will be used to represent mandatory attributes not specified. From CoreParser.
	 */
	public final static String                   UNDEFINED      = new String("<undefined value>");

	public static boolean getFailedChecking() {
		return CheckObr.failedChecking;
	}

	public static void error(String msg) {
		CheckObr.failedChecking = true;
		System.err.println(msg);
	}

	public static void warning(String msg) {
		System.out.println(msg);
	}

	public static void setFailedParsing(boolean failed) {
		CheckObr.failedChecking = failed;
	}

	public static void init(String defaultOBRRepo) {
		System.out.println("start CheckOBR. Default repo= " + defaultOBRRepo);
		try {
			File theRepo = new File(defaultOBRRepo);
			CheckObr.dataModelHelper =  new DataModelHelperImpl();

			CheckObr.repo = CheckObr.dataModelHelper.repository(theRepo.toURI().toURL());
			// CheckObr.repo.refresh();
			// System.out.println("read repo " + defaultOBRRepo);
			CheckObr.resources = CheckObr.repo.getResources();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private static <R extends ResourceReference> Set<R> asSet(String set, Class<R> kind) {
		Set<ResourceReference> references = new HashSet<ResourceReference>();
		for (String  id : Util.split(set)) {
			if (InterfaceReference.class.isAssignableFrom(kind))
				references.add(new InterfaceReference(id));
			if (MessageReference.class.isAssignableFrom(kind))
				references.add(new MessageReference(id));
		}
		return (Set<R>) references;
	}

	/**
	 * 
	 * @param dep
	 */
	public static void checkConstraints(DependencyDeclaration dep) {
		if ((dep == null) || !(dep.getTarget() instanceof SpecificationReference))
			return;

		SpecificationReference reference = dep.getTarget().as(SpecificationReference.class);
		String spec = reference.getName();

		Capability cap = CheckObr.getCapability(reference);
		if (cap == null)
			return;
		Set<String> validImplAttrs = CheckObr.getValidImplAttributes(cap);
		//        Set<String> validInstAttrs = CheckObr.getValidInstAttributes(cap);

		CheckObr.checkFilterSet(dep.getImplementationConstraints(), validImplAttrs, spec);
		CheckObr.checkFilterList(dep.getImplementationPreferences(), validImplAttrs, spec);
		CheckObr.checkInstFilterSet(dep.getInstanceConstraints(), validImplAttrs, spec);
		CheckObr.checkInstFilterList(dep.getInstancePreferences(), validImplAttrs, spec);
	}

	/**
	 * In theory we cannot check a constraint on instance attributes since we do not know the implementation that
	 * will be selected, however, if the constraints contains "impl-name = xyz" we could do it.
	 * 
	 * At least we can check the spec and implem attributes
	 * 
	 */
	public static void checkInstFilterList(List<String> filters, Set<String> validAttr, String spec) {
		if ((validAttr == null) || (filters == null))
			return;

		// try to see if implementation name "impl-name" is in the constraints
		String impl = null;
		for (String f : filters) {
			try {
				ApamFilter parsedFilter = ApamFilter.newInstance(f);
				if (parsedFilter == null)
					CheckObr.error("String " + f + " returns null filter.");
				else
					impl = parsedFilter.lookForAttr(CST.A_IMPLNAME);
			} catch (Exception e) {
				CheckObr.error("Invalid filter " + f);
			}
			if (impl != null)
				break ;
		}
		// if implementation is found
		Capability cap = null;
		if (impl != null) {
			cap = CheckObr.getCapability(new ImplementationReference<ImplementationDeclaration>(impl));
			if (cap != null) {
				// Map<String, Object> props = cap.getProperties();
				for (Property attr : cap.getProperties()) {
					if (!attr.getName().startsWith(OBR.A_DEFINITION_PREFIX))
						validAttr.add(attr.getName());
				}
			}
		}
		if (cap != null)
			CheckObr.checkFilterList(filters, validAttr, impl);
		else
			CheckObr.checkFilterList(filters, validAttr, spec);
	}

	public static void checkInstFilterSet(Set<String> filters, Set<String> validAttr, String spec) {
		List<String> filterSet = new ArrayList<String>(filters);
		CheckObr.checkInstFilterList(filterSet, validAttr, spec);
	}

	public static void checkFilterList(List<String> filters, Set<String> validAttr, String spec) {
		for (String f : filters) {
			ApamFilter parsedFilter = ApamFilter.newInstance(f);
			// System.err.println("validating filter " + f);
			parsedFilter.validateAttr(validAttr, f, spec);
		}
	}

	public static void checkFilterSet(Set<String> filters, Set<String> validAttr, String spec) {
		for (String f : filters) {
			ApamFilter parsedFilter = ApamFilter.newInstance(f);
			// System.err.println("validating filter " + f);
			parsedFilter.validateAttr(validAttr, f, spec);
		}
	}

	/**
	 * returns all the attributes that can be associated with an implementation:
	 * attribute instantiated on the spec, plus those defined.
	 * 
	 * @param cap : the capability of the associated specification.
	 * @return
	 */
	private static Set<String> getValidImplAttributes(Capability cap) {
		if (cap == null)
			return null;
		Set<String> validAttrs = new HashSet<String>();

		for (Property attrObject : cap.getProperties()) {

			if (attrObject.getName().startsWith(OBR.A_DEFINITION_PREFIX))
				validAttrs.add(attrObject.getName().substring(11));
			else
				validAttrs.add(attrObject.getName());
		}
		return validAttrs;
	}

	//        private static Set<String> getValidInstAttributes(Capability cap) {
	//            if (cap == null)
	//                return null;
	//            Set<String> validAttrs = new HashSet<String>();
	//            // for (String predef : CheckObr.predefAttributes) {
	//            // validAttrs.add(predef);
	//            // }
	//            String attr;
	//    
	//            return validAttrs;
	//            xxx
	//        }

	/**
	 * Check if attribute "attr" is defined in the list of attributes and definitions found in props
	 * Props contains attribute (Cannot be redefined), and attribute definitions.
	 * Check if the value is consistent with the type.
	 * All predefined attributes are Ok (scope ...)
	 * Cannot be a reserved attribute
	 */
	private static boolean capContainsDefAttr(Property[] props, String attr, Object value, String component, String spec) {
		if (!Util.validAttr(component, attr))
			return false ;

		String defattr = OBR.A_DEFINITION_PREFIX + attr;
		for (Property prop : props) {
			if (prop.getName().equals(defattr)) {
				return Util.checkAttrType(attr, value, prop.getValue());
			}
		}
		if (component != null)
			System.out
			.println("in component " + component + ", attribute " + attr + " used but not defined in " + spec);
		else         
			System.out.println("in instance, attribute " + attr + " used but not defined in " + spec);
		return false;
	}

	/**
	 * Check the consistency of an instance :
	 * -Existence of its implementation,
	 * -Validity of its attributes,
	 * -Validity of its constraints.
	 * 
	 * @param instance
	 */
	public static void checkInstance(InstanceDeclaration instance) {
		ImplementationReference impl = instance.getImplementation();
		String name = instance.getName();
		if (impl == null) {
			CheckObr.error("ERROR: implementation name missing for instance " + name);
			return;
		}

		// Capability capImpl = CheckObr.getImplCapability(instance.getImplementation());
		CheckObr.checkInstAttributes(impl.getName(), name, instance);

		Set<DependencyDeclaration> deps = instance.getDependencies();
		if (deps == null)
			return;
		for (DependencyDeclaration dep : deps) {
			CheckObr.checkConstraints(dep);
		}
	}

	public static Map<String, Object> getSpecOBRAttr(ImplementationDeclaration component) {
		SpecificationReference spec = component.getSpecification();
		Map<String, Object> properties = component.getProperties();
		if (spec == null)
			return properties;
		Capability cap = CheckObr.getCapability(component.getSpecification());
		if (cap == null) {
			return properties;
		}
		return cap.getPropertiesAsMap();
	}
	/**
	 * 
	 * @param implName
	 * @param spec
	 * @param properties
	 */
	public static void checkImplAttributes(ImplementationDeclaration component) {
		String implName = component.getName();
		Map<String, Object> properties = component.getProperties();
		String defAttr = null ;

		Property[] specProps = null ;
		SpecificationReference spec = component.getSpecification();
		if (spec != null) {
			Capability specCap = CheckObr.getCapability(spec);
			if (specCap != null)
				specProps = specCap.getProperties();
		}

		for (String attr : properties.keySet()) {
			attr = attr.toLowerCase() ;
			if (Util.isPredefinedAttribute(attr))continue ;
			if (!Util.validAttr(implName, attr)) continue ;
			//No spec, cannot check validity
			if (specProps == null) continue ; 
			if (getPropertyValue(specProps, attr) == null) {
				System.out.println("cannot redefine attribute \"" + attr + "\"");
				continue ;
			}
			defAttr = getAttrDefinition(specProps, attr) ;
			if (defAttr == null) {
				System.out.println("in " + component + ", attribute \"" + attr + "\" used but not defined in " + spec);
				continue ;
			}

			Util.checkAttrType(attr, properties.get(attr), defAttr);
		}
	}

	//    if (component != null)
	//        System.out
	//        .println("in component " + component + ", attribute " + attr + " used but not defined in " + spec);
	//    else         
	//        System.out.println("in instance, attribute " + attr + " used but not defined in " + spec);
	//    return false;
	//			}
	//
	//        
	//        if (spec == null)
	//            return Util.validAttr(component, attr);
	//        Capability cap = CheckObr.getCapability(component.getSpecification());
	//        if (cap == null) {
	//            return;
	//        }
	//        
	//        // each attribute in properties must be declared in spec.
	//        Property[] props = cap.getProperties();
	//        for (String attr : properties.keySet()) {
	//            CheckObr.capContainsDefAttr(props, attr, properties.get(attr), implName, spec.getName());
	//        }
	//    }

	/**
	 * Return the value of property "attr" from an array of properties.
	 * @param specProps
	 * @param attr
	 * @return
	 */
	private static String getPropertyValue (Property[] props, String attr) {
		for (Property prop : props) {
			if (prop.getName().equals(attr)) {
				return prop.getValue() ;
			}
		}
		return null ;
	}

	/**
	 * returns the value of attribute "definition-"attr from an array of properties
	 * @param props
	 * @param attr
	 * @return
	 */
	private static String getAttrDefinition (Property[] props, String attr) {
		return getPropertyValue(props, OBR.A_DEFINITION_PREFIX + attr) ;
	}
	/**
	 * Provided component is an instance "name", and impl its implem in cap, check if the instance attribute are valid.
	 * 
	 * @param impl
	 * @param name
	 * @param cap
	 * @param instance
	 */
	public static void checkInstAttributes(String impl, String name, InstanceDeclaration instance) {
		Capability capImpl = CheckObr.getCapability(instance.getImplementation());
		if (capImpl == null) {
			return;
		}

		Map<String, Object> properties = instance.getProperties();

		Property[] props = capImpl.getProperties();

		// Add spec attributes
		// each attribute in properties must be declared in cap.
		for (String attr : properties.keySet()) {
			CheckObr.capContainsDefAttr(props, attr, properties.get(attr), name, impl);
		}
	}

	/**
	 * An implementation has the following provide; check if consistent with the list of provides found in "cap".
	 * 
	 * @param cap. Can be null.
	 * @param interfaces = "{I1, I2, I3}" or I1 or null
	 * @param messages= "{M1, M2, M3}" or M1 or null
	 * @return
	 */
	public static boolean checkImplProvide(String implName, String spec, Set<InterfaceReference> interfaces,
			Set<MessageReference> messages) {
		if (spec == null)
			return true;
		Capability cap = CheckObr.getCapability(new SpecificationReference(spec));
		if (cap == null) {
			return true;
		}
		// CheckObr.printCap(cap);

		Set<MessageReference> specMessages = CheckObr.asSet(CheckObr.getAttributeInCap(cap, OBR.A_PROVIDE_MESSAGES),
				MessageReference.class);
		Set<InterfaceReference> specInterfaces = CheckObr.asSet(CheckObr.getAttributeInCap(cap,
				OBR.A_PROVIDE_INTERFACES), InterfaceReference.class);

		if (!(messages.containsAll(specMessages)))
			CheckObr.error("Implementation " + implName + " must produce messages "
					+ CheckObr.getAttributeInCap(cap, OBR.A_PROVIDE_MESSAGES));

		if (!(interfaces.containsAll(specInterfaces)))
			CheckObr.error("Implementation " + implName + " must implement interfaces "
					+ CheckObr.getAttributeInCap(cap, OBR.A_PROVIDE_INTERFACES));

		return true;
	}


	public static void checkCompoMain(CompositeDeclaration composite) {
		String name = composite.getName();
		// System.err.println("in checkCompoMain ");
		String implName = composite.getMainComponent().getName();
		Capability cap = CheckObr.getCapability(composite.getMainComponent());
		if (cap == null) {
			return;
		}
		if (composite.getSpecification() != null) {
			String spec = composite.getSpecification().getName();
			String capSpec = CheckObr.getAttributeInCap(cap, OBR.A_PROVIDE_SPECIFICATION);
			if ((capSpec != null) && !spec.equals(capSpec)) {
				CheckObr.error("In " + name + " Invalid main implementation. " + implName
						+ " must implement specification " + spec);
			}
		}

		Set<MessageReference> mainMessages = CheckObr.asSet(CheckObr.getAttributeInCap(cap, OBR.A_PROVIDE_MESSAGES), MessageReference.class);
		Set<MessageReference> compositeMessages = composite.getProvidedResources(MessageReference.class);
		if (!mainMessages.containsAll(compositeMessages))
			CheckObr.error("In " + name + " Invalid main implementation. " + implName
					+ " produces messages " + mainMessages
					+ " \n but must produce messages " + compositeMessages);

		Set<InterfaceReference> mainInterfaces = CheckObr.asSet(CheckObr.getAttributeInCap(cap,
				OBR.A_PROVIDE_INTERFACES), InterfaceReference.class);
		Set<InterfaceReference> compositeInterfaces = composite.getProvidedResources(InterfaceReference.class);
		if (!mainInterfaces.containsAll(compositeInterfaces))
			CheckObr.error("In " + name + " Invalid main implementation. " + implName
					+ " implements " + mainInterfaces
					+ " \n but must implement interfaces " + compositeInterfaces);
	}

	/**
	 * For all kinds of components checks the dependencies : fields (for implems), and constraints.
	 * 
	 * @param component
	 */
	public static void checkRequire(ComponentDeclaration component) {
		Set<DependencyDeclaration> deps = component.getDependencies();
		if (deps == null)
			return;
		CheckObr.allFields.clear();
		Set<String> depIds = new HashSet<String>();
		for (DependencyDeclaration dep : deps) {
			if (depIds.contains(dep.getIdentifier())) {
				CheckObr.error("ERROR: Dependency " + dep.getIdentifier() + " allready defined.");
			} else
				depIds.add(dep.getIdentifier());
			// validating dependency constraints and preferences..
			CheckObr.checkConstraints(dep);
			// Checking fields and complex dependencies
			CheckObr.checkFieldTypeDep(dep, component);
		}
	}


	public static void printRes(Resource aResource) {
		System.out.println("\n\nRessource SymbolicName : " + aResource.getSymbolicName() + " id: " + aResource.getId());
		for (Capability aCap : aResource.getCapabilities()) {
			CheckObr.printCap(aCap);
		}
	}

	public static void printCap(Capability aCap) {
		System.out.println("   Capability name: " + aCap.getName());

		for (Property prop : aCap.getProperties()) {

			System.out.println("type de value: " + prop.getValue().getClass());
			System.out.println("     " +  prop.getName() + " val= " +  prop.getValue());
		}
	}

	private static String getAttributeInCap(Capability cap, String name) {
		if (cap == null)
			return null;
		Map<String, Object> props = cap.getPropertiesAsMap();
		String prop =  (String) props.get(name);
		if (prop == null)
			return null;
		return prop;
	}

	private static Capability getCapability(ComponentReference<?> reference) {
		String name = reference.getName();
		if (CheckObr.readCapabilities.containsKey(name))
			return CheckObr.readCapabilities.get(name);
		for (Resource res : CheckObr.resources) {
			if (reference instanceof SpecificationReference) {
				if (!OBRGeneratorMojo.bundleDependencies.contains(res.getId()))
					continue ;
			}
			//            if (OBRGeneratorMojo.bundleDependencies.contains(res.getId())) {
			for (Capability cap : res.getCapabilities()) {
				if (cap.getName().equals(OBR.CAPABILITY_COMPONENT)
						&& (CheckObr.getAttributeInCap(cap, "name").equals(name))) {
					System.out.println("     Component " + name + " found in bundle " + res.getId());
					CheckObr.readCapabilities.put(name, cap);
					return cap;
				}
				//                }
			}
		}
		System.err.println("     Component " + name + " not found in repository " + CheckObr.repo.getURI());
		return null;
	}

	//    private static Capability getImplCapability(ComponentReference<?> reference) {
	//        String name = reference.getName();
	//        if (CheckObr.readCapabilities.containsKey(name))
	//            return CheckObr.readCapabilities.get(name);
	//        for (Resource res : CheckObr.resources) {
	//            //            if (ApamMavenPlugin.bundleDependencies.contains(res.getId())) {
	//            for (Capability cap : res.getCapabilities()) {
	//                if (cap.getName().equals(OBR.CAPABILITY_COMPONENT)
	//                        && (CheckObr.getAttributeInCap(cap, "impl-name") != null)
	//                        && (CheckObr.getAttributeInCap(cap, "impl-name").equals(name))) {
	//                    System.out.println("     Implementation " + name + " found in bundle " + res.getId());
	//                    CheckObr.readCapabilities.put(name, cap);
	//                    return cap;
	//                }
	//            }
	//        }
	//        System.err.println("     Implementation " + name + " not found in repository " + CheckObr.repo.getURI());
	//        return null;
	//    }
	//
	//    private static Capability getCompoCapability(ImplementationReference<? extends CompositeDeclaration> reference) {
	//        String name = reference.getName();
	//        for (Resource res : CheckObr.resources) {
	//            for (Capability cap : res.getCapabilities()) {
	//                if (cap.getName().equals(OBR.CAPABILITY_COMPONENT)
	//                        && (CheckObr.getAttributeInCap(cap, CST.A_COMPOSITE) != null)
	//                        && (CheckObr.getAttributeInCap(cap, CST.A_COMPOSITE).equals("true"))
	//                        && (CheckObr.getAttributeInCap(cap, name) != null))
	//                    return cap;
	//            }
	//        }
	//        return null;
	//    }

	/**
	 * Provided a dependency "dep" (simple or complex) checks if the field type and attribute multiple are compatible.
	 * For complex dependency, for each field, checks if the target specification implements the field resource.
	 * 
	 * @param dep : a dependency
	 * @param component : the component currently analyzed
	 */
	private static void checkFieldTypeDep(DependencyDeclaration dep, ComponentDeclaration component) {
		if (!(component instanceof AtomicImplementationDeclaration)) return ;

		// All field must have same multiplicity, and must refer to interfaces and messages provided by the specification.


		Set<ResourceReference> specResources = new HashSet<ResourceReference>();

		if (dep.getTarget() instanceof SpecificationReference) {
			SpecificationReference spec = (SpecificationReference) dep.getTarget();
			specResources.addAll( CheckObr.asSet(CheckObr.getAttributeInCap(CheckObr.getCapability(spec), OBR.A_PROVIDE_INTERFACES), InterfaceReference.class));
			specResources.addAll( CheckObr.asSet(CheckObr.getAttributeInCap(CheckObr.getCapability(spec), OBR.A_PROVIDE_MESSAGES), MessageReference.class));
		} else if (dep.getTarget() instanceof ImplementationReference) {
			ImplementationReference implem = (ImplementationReference) dep.getTarget();
			specResources.addAll(CheckObr.asSet(CheckObr.getAttributeInCap(CheckObr.getCapability(implem),
					OBR.A_PROVIDE_INTERFACES), InterfaceReference.class));
			specResources.addAll(CheckObr.asSet(CheckObr.getAttributeInCap(CheckObr.getCapability(implem),
					OBR.A_PROVIDE_MESSAGES), MessageReference.class));
		} else {
			specResources.add(dep.getTarget().as(ResourceReference.class));
		}

		Boolean mult = dep.isMultiple();
		for (DependencyInjection innerDep : dep.getInjections()) {
			// check if attribute "multiple" matches the fields type (Set, List Array)
			// if multiple is not explicitly defined, assume the first field multiplicity

			// TODO MIGRATION DECLARATAION change ineference
			if (mult == null) {
				//dep.setMultiple(CheckObr.isFieldMultiple(innerDep, component));
				mult = dep.isMultiple();
			}
			if (mult != CheckObr.isFieldMultiple(innerDep, component)) {
				if (!mult)
					CheckObr.error("ERROR: in " + component.getName() + dep + "\n      Field "
							+ innerDep.getName()
							+ " is a collection field, while other fields in same dependency are simple.");
				else
					CheckObr.error("ERROR: in " + component.getName() + dep + "\n      Field "
							+ innerDep.getName()
							+ " is a simple field, while other fields in same dependency are collection.");
			}
			String type = innerDep.getResource().getJavaType();

			if ((innerDep.getResource() != ResourceReference.UNDEFINED) && !(specResources.contains(innerDep.getResource()))) {
				System.err.println("ERROR: in " + component.getName() + dep + "\n      Field "
						+ innerDep.getName()
						+ " is of type " + type
						+ " which is not implemented by specification or implementation " + dep.getIdentifier());
			}
		}
	}

	/**
	 * Provided an atomic dependency, returns if it is multiple or not.
	 * Checks if the same field is declared twice.
	 * 
	 * @param dep
	 * @param component
	 * @return
	 */
	public static boolean isFieldMultiple(DependencyInjection dep, ComponentDeclaration component) {
		if (CheckObr.allFields.contains(dep.getName()) && !dep.getName().equals(CheckObr.UNDEFINED)) {
			CheckObr.error("ERROR: in " + component.getName() + " field/method " + dep.getName()
					+ " allready declared");
		}
		else {
			CheckObr.allFields.add(dep.getName());
		}

		return dep.isCollection();
	}
}
