package fr.imag.adele.apam.apamMavenPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Resource;

import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.OBR;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.apam.core.*;

public class CheckObr {

    private static RepositoryImpl repo;
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
            CheckObr.repo = new RepositoryImpl(theRepo.toURI().toURL());
            CheckObr.repo.refresh();
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
     * only string, int and bool attributes are accepted.
     * 
     * @param value
     * @param type
     */
    public static void checkAttrType(String attr, Object val, String type) {
        if ((type == null) || (val == null))
            return;

        if (!(val instanceof String)) {
            CheckObr.error("Invalid attribute value " + val + " for attribute " + attr
                    + ". String value expected.");
        }
        String value = (String) val;
        if (type.equals("bool") && !value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            CheckObr.error("Invalid attribute value " + value + " for attribute " + attr + ". Boolean value expected.");
            return;
        }
        if (type.equals("int")) {
            try {
                int valint = Integer.parseInt(value);
                return;
            } catch (Exception e) {
                CheckObr.error("Invalid attribute value " + value + " for attribute " + attr
                        + ". Integer value expected.");
            }
        }
        if (!(value instanceof String)) {
            CheckObr.error("Invalid attribute value " + value + " for attribute " + attr
                    + ". String value expected.");
        }
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

        Capability cap = CheckObr.getSpecCapability(reference);
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
                    System.err.println("String " + f + " returns null filter.");
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
            cap = CheckObr.getImplCapability(new ImplementationReference<ImplementationDeclaration>(impl));
            if (cap != null) {
                // Map<String, Object> props = cap.getProperties();
                for (Object attr : cap.getProperties().keySet()) {
                    if (!((String) attr).startsWith(OBR.A_DEFINITION_PREFIX))
                        validAttr.add((String) attr);
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
        String attr;
        for (Object attrObject : cap.getProperties().keySet()) {
            attr = (String) attrObject;
            if (attr.startsWith(OBR.A_DEFINITION_PREFIX))
                validAttrs.add(attr.substring(11));
            else
                validAttrs.add(attr);
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
    private static boolean capContainsDefAttr(Map<String, Object> props, String attr, Object value) {
        if (Util.isPredefinedAttribute(attr))
            return true;

        if (Util.isReservedAttribute(attr)) {
            CheckObr.error("ERROR: " + attr + " is a reserved attribute");
            return false;
        }

        for (Object prop : props.keySet()) {
            if (((String) prop).equalsIgnoreCase(attr)) {
                CheckObr.error("ERROR: cannot redefine attribute " + attr);
                return false;
            }
        }
        String defattr = OBR.A_DEFINITION_PREFIX + attr;
        for (Object prop : props.keySet()) {
            if (((String) prop).equals(defattr)) {
                // for definitions, value is the type: "string", "int", "bool"
                Object val = props.get(prop);
                if (val instanceof Collection) {
                    for (Object aVal : (Collection) val) {
                        CheckObr.checkAttrType(attr, value, (String) aVal);
                    }
                    return true;
                }
                if (val instanceof String) {
                    CheckObr.checkAttrType(attr, value, (String) val);
                    return true;
                }
            }
        }
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
        Capability cap = CheckObr.getSpecCapability(component.getSpecification());
        if (cap == null) {
            return properties;
        }
        return cap.getProperties();
    }
    /**
     * 
     * @param implName
     * @param spec
     * @param properties
     */
    public static void checkImplAttributes(ImplementationDeclaration component) {
        String implName = component.getName();
        SpecificationReference spec = component.getSpecification();
        Map<String, Object> properties = component.getProperties();
        if (spec == null)
            return;
        Capability cap = CheckObr.getSpecCapability(component.getSpecification());
        if (cap == null) {
            return;
        }
        // each attribute in properties must be declared in spec.
        Map<String, Object> props = cap.getProperties();
        for (String attr : properties.keySet()) {
            if (!CheckObr.capContainsDefAttr(props, attr, properties.get(attr))) {
                System.err.println("In implementation " + implName + ", attribute " + attr
                        + " used but not defined in "
                        + spec.getName());
            }
        }
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
        Capability capImpl = CheckObr.getImplCapability(instance.getImplementation());
        if (capImpl == null) {
            return;
        }

        Map<String, Object> properties = instance.getProperties();

        Map<String, Object> props = capImpl.getProperties();

        // Add spec attributes
        // each attribute in properties must be declared in cap.
        for (String attr : properties.keySet()) {
            if (!CheckObr.capContainsDefAttr(props, attr, properties.get(attr))) {
                if (name == null) {
                    System.err.println("In instance, attribute " + attr
                            + " used but not defined in " + impl);
                } else {
                    System.err.println("In instance " + name + ", attribute " + attr
                            + " used but not defined in " + impl);
                }
            }
        }
    }

    /**
     * An implementation has the following provide; check if consistent with its specification.
     * First tries to find the spec in obr; if found check that all the spec provide are included
     * 
     * @param spec. Can be null.
     * @param interfaces = "{I1, I2, I3}" or I1 or null
     * @param messages= "{M1, M2, M3}" or M1 or null
     * @return
     */
    public static boolean checkImplProvide(String implName, String spec, Set<InterfaceReference> interfaces, Set<MessageReference> messages) {
        if (spec == null)
            return true;
        Capability cap = CheckObr.getSpecCapability(new SpecificationReference(spec));
        if (cap == null) {
            return true;
        }
        // CheckObr.printCap(cap);

        Set<MessageReference> specMessages = CheckObr.asSet(CheckObr.getAttributeInCap(cap, OBR.A_PROVIDE_MESSAGES),MessageReference.class); 
        Set<InterfaceReference> specInterfaces = CheckObr.asSet(CheckObr.getAttributeInCap(cap, OBR.A_PROVIDE_INTERFACES),InterfaceReference.class);

        if ( !(messages.containsAll(specMessages)))
            System.err.println("Implementation " + implName + " must produce messages "
                    + CheckObr.getAttributeInCap(cap, OBR.A_PROVIDE_MESSAGES));

        if (! (interfaces.containsAll(specInterfaces)))
            System.err.println("Implementation " + implName + " must implement interfaces "
                    + CheckObr.getAttributeInCap(cap, OBR.A_PROVIDE_INTERFACES));

        return true;
    }


    public static void checkCompoMain(CompositeDeclaration composite) {
        String name = composite.getName();
        // System.err.println("in checkCompoMain ");
        String implName = composite.getMainComponent().getName();
        Capability cap = CheckObr.getImplCapability(composite.getMainComponent());
        if (cap == null) {
            cap = CheckObr.getSpecCapability(composite.getMainComponent());
            if (cap == null)
                return;
        }
        String spec = composite.getSpecification().getName();
        if (spec == null)
            return;
        String capSpec = CheckObr.getAttributeInCap(cap, OBR.A_PROVIDE_SPECIFICATION);
        if ((capSpec != null) && !spec.equals(capSpec)) {
            CheckObr.error("In " + name + " Invalid main implementation. " + implName
                    + " must implement specification " + spec);
        }

        Set<MessageReference> mainMessages = CheckObr.asSet(CheckObr.getAttributeInCap(cap, OBR.A_PROVIDE_MESSAGES), MessageReference.class);
        Set<MessageReference> compositeMessages = composite.getProvidedResources(MessageReference.class);
        if (!mainMessages.containsAll(compositeMessages))
            System.err.println("In " + name + " Invalid main implementation. " + implName
                    + " produces messages " + mainMessages
                    + " \n but must produce messages " + compositeMessages);

        Set<InterfaceReference> mainInterfaces = CheckObr.asSet(CheckObr.getAttributeInCap(cap,
                OBR.A_PROVIDE_INTERFACES), InterfaceReference.class);
        Set<InterfaceReference> compositeInterfaces = composite.getProvidedResources(InterfaceReference.class);
        if (!mainInterfaces.containsAll(compositeInterfaces))
            System.err.println("In " + name + " Invalid main implementation. " + implName
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
        String value;
        for (Object prop : aCap.getProperties().keySet()) {
            Object val = aCap.getProperties().get(prop);
            System.out.println("type de value: " + val.getClass());
            System.out.println("     " + (String) prop + " val= " + aCap.getProperties().get(prop));
        }
    }

    private static String getAttributeInCap(Capability cap, String name) {
        if (cap == null)
            return null;
        Map<String, Object> props = cap.getProperties();
        List<String> prop = (List<String>) props.get(name);
        if (prop == null)
            return null;
        return (String) prop.toArray()[0];
    }

    private static Capability getSpecCapability(ComponentReference<?> reference) {
        String name = reference.getName();
        if (CheckObr.readCapabilities.containsKey(name))
            return CheckObr.readCapabilities.get(name);
        for (Resource res : CheckObr.resources) {
            if (OBRGeneratorMojo.bundleDependencies.contains(res.getId())) {
                for (Capability cap : res.getCapabilities()) {
                    if (cap.getName().equals(OBR.CAPABILITY_SPECIFICATION)
                            && (CheckObr.getAttributeInCap(cap, "spec-name") != null)
                            && (CheckObr.getAttributeInCap(cap, "spec-name").equals(name))) {
                        System.out.println("     Specification " + name + " found in bundle " + res.getId());
                        CheckObr.readCapabilities.put(name, cap);
                        return cap;
                    }
                }
            }
        }
        System.out
        .println("     Warning: Specification " + name + " not found in repository " + CheckObr.repo.getURL());
        return null;
    }

    private static Capability getImplCapability(ComponentReference<?> reference) {
        String name = reference.getName();
        if (CheckObr.readCapabilities.containsKey(name))
            return CheckObr.readCapabilities.get(name);
        for (Resource res : CheckObr.resources) {
            //            if (ApamMavenPlugin.bundleDependencies.contains(res.getId())) {
            for (Capability cap : res.getCapabilities()) {
                if (cap.getName().equals(OBR.CAPABILITY_IMPLEMENTATION)
                        && (CheckObr.getAttributeInCap(cap, "impl-name") != null)
                        && (CheckObr.getAttributeInCap(cap, "impl-name").equals(name))) {
                    System.out.println("     Implementation " + name + " found in bundle " + res.getId());
                    CheckObr.readCapabilities.put(name, cap);
                    return cap;
                }
                //                }
            }
        }
        System.err.println("     Implementation " + name + " not found in repository " + CheckObr.repo.getURL());
        return null;
    }

    private static Capability getCompoCapability(ImplementationReference<? extends CompositeDeclaration> reference) {
        String name = reference.getName();
        for (Resource res : CheckObr.resources) {
            for (Capability cap : res.getCapabilities()) {
                if (cap.getName().equals(OBR.CAPABILITY_IMPLEMENTATION)
                        && (CheckObr.getAttributeInCap(cap, CST.A_COMPOSITE) != null)
                        && (CheckObr.getAttributeInCap(cap, CST.A_COMPOSITE).equals("true"))
                        && (CheckObr.getAttributeInCap(cap, name) != null))
                    return cap;
            }
        }
        return null;
    }

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

        SpecificationReference spec = dep.getTarget().as(SpecificationReference.class);
        Set<ResourceReference> specResources = new HashSet<ResourceReference>();

        if (spec != null) {
            specResources.addAll( CheckObr.asSet(CheckObr.getAttributeInCap(CheckObr.getSpecCapability(spec), OBR.A_PROVIDE_INTERFACES), InterfaceReference.class));
            specResources.addAll( CheckObr.asSet(CheckObr.getAttributeInCap(CheckObr.getSpecCapability(spec), OBR.A_PROVIDE_MESSAGES), MessageReference.class));
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
                CheckObr.error("ERROR: in " + component.getName() + dep + "\n      Field "
                        + innerDep.getName()
                        + " is of type " + type
                        + " which is not implemented by specification " + dep.getIdentifier());
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
