package fr.imag.adele.apam.apamMavenPlugin;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Resource;

import fr.imag.adele.apam.util.ApamComponentXML;
import fr.imag.adele.apam.util.Util;
import fr.imag.adele.apam.util.ApamComponentXML.ApamComponentInfo;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Dependency.AtomicDependency;
import fr.imag.adele.apam.util.Dependency.CompositeDependency;
import fr.imag.adele.apam.util.Dependency.ImplementationDependency;
import fr.imag.adele.apam.util.Dependency.SpecificationDependency;

//import fr.imag.adele.obrMan.OBRManager;
//import fr.imag.adele.obrMan.OBRManager.Selected;

public class CheckObr {

    private static RepositoryImpl repo;
    private static Resource[]     resources;
    private static String[]       predefAttributes = { "scope", "shared", "visible", "name",
        "apam-composite", "apam-main-implementation", "require-interface",
        "require-specification", "require-message" };

    private static final String[]                fieldTypeMultiple = { "java.util.Set", "java.util.List",
        "java.util.Collection", "java.util.Vector" };

    private static final Map<String, Capability> readSpecs        = new HashMap<String, Capability>();
    private static final Set<String>             allFields         = new HashSet<String>();

    public static void init(String defaultOBRRepo) {
        File theRepo = new File(defaultOBRRepo);
        try {
            CheckObr.repo = new RepositoryImpl(theRepo.toURI().toURL());
            CheckObr.repo.refresh();
            System.out.println("read repo " + defaultOBRRepo);
            CheckObr.resources = CheckObr.repo.getResources();

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void checkList(String impl, String spec, String msg) {
        if ((spec == null) || (impl == null))
            return;
        List<String> implList = Util.splitList(impl);
        // each element of sp must be found in implList
        for (String sp : Util.split(spec)) {
            if (!implList.contains(sp)) {
                System.err.println(msg + sp + ". Declared: " + impl);
            }
        }
    }

    public static void checkFilterList(List<String> filters, Set<String> validAttr) {
        if ((validAttr == null) || (filters == null))
            return;
        for (String f : filters) {
            try {
                ApamFilter parsedFilter = ApamFilter.newInstance(f);
                // System.err.println("validating filter " + f);
                parsedFilter.validateAttr(validAttr, CheckObr.predefAttributes, f);
            } catch (InvalidSyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isPredefAttribute(String attr) {
        for (String predef : CheckObr.predefAttributes) {
            if (predef.equalsIgnoreCase(attr))
                return true;
        }
        return false;
    }

    private static Set<String> getValidAttributes(Capability cap) {
        if (cap == null)
            return null;
        Set<String> validAttrs = new HashSet<String>();
        //        for (String predef : CheckObr.predefAttributes) {
        //            validAttrs.add(predef);
        //        }
        String attr;
        for (Object attrObject : cap.getProperties().keySet()) {
            attr = (String) attrObject;
            if (attr.startsWith("definition-"))
                validAttrs.add(attr.substring(11));
            else
                validAttrs.add(attr);
        }
        return validAttrs;
    }

    private static boolean capContainsDefAttr(Capability cap, String attr) {
        if (CheckObr.isPredefAttribute(attr))
            return true;

        Map<String, Object> props = cap.getProperties();
        for (Object prop : cap.getProperties().keySet()) {
            if (((String) prop).equals(attr)) {
                System.err.println("Warning: redefining specification attribute " + attr);
                return true;
            }
        }
        attr = "definition-" + attr;
        for (Object prop : cap.getProperties().keySet()) {
            if (((String) prop).equals(attr)) {
                return true;
            }
        }
        return false;
    }

    public static void checkAttributes(String implName, String spec, Map<String, String> properties) {
        if (spec == null)
            return;
        Capability cap = CheckObr.getSpecCapability(spec);
        if (cap == null) {
            //            System.err.println("Warning : Specification " + spec + "  not found in repository");
            return;
        }
        // each attribute in properties must be declared in spec.
        for (String attr : properties.keySet()) {
            if (!CheckObr.capContainsDefAttr(cap, attr)) {
                System.err.println("In implementation " + implName + ", attribute " + attr
                        + " used but not defined in "
                        + spec);
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
    public static boolean checkProvide(String implName, String spec, String interfaces, String messages) {
        if (spec == null)
            return true;
        Capability cap = CheckObr.getSpecCapability(spec);
        if (cap == null) {
            System.out.println("Warning : Specification " + spec + "  not found in repository");
            return true;
        }
        // CheckObr.printCap(cap);
        CheckObr.checkList(messages, CheckObr.getAttributeInCap(cap, "provide-messages"),
                "Implementation " + implName + " must produce message ");
        CheckObr.checkList(interfaces, CheckObr.getAttributeInCap(cap, "provide-interfaces"),
                "Implementation " + implName + " must implement interface ");

        return true;
    }

    public static void checkCompoRequire(String implName, String spec, Set<CompositeDependency> deps) {
        // TODO
    }

    public static void checkImplRequire(ApamComponentInfo component) {
        String spec = component.getSpecification();
        Set<ImplementationDependency> deps = component.getImplemDependencies();
        Set<String> validAttrs = CheckObr.getValidAttributes(CheckObr.getSpecCapability(spec));
        CheckObr.allFields.clear();
        for (ImplementationDependency dep : deps) {
            // System.err.println("validating dependency constraints and preferences....");
            CheckObr.checkFilterList(dep.implementationConstraints, validAttrs);
            CheckObr.checkFilterList(dep.instanceConstraints, validAttrs);
            CheckObr.checkFilterList(dep.implementationPreferences, validAttrs);
            CheckObr.checkFilterList(dep.instancePreferences, validAttrs);

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
        Map<String, Object> props = cap.getProperties();
        List<String> prop = (List<String>) props.get(name);
        if (prop == null)
            return null;
        return (String) prop.toArray()[0];
    }

    private static Capability getSpecCapability(String name) {
        if (CheckObr.readSpecs.containsKey(name))
            return CheckObr.readSpecs.get(name);
        for (Resource res : CheckObr.resources) {
            if (ApamMavenPlugin.bundleDependencies.contains(res.getId())) {
                for (Capability cap : res.getCapabilities()) {
                    if (cap.getName().equals("apam-specification")
                            && (CheckObr.getAttributeInCap(cap, "name").equals(name))) {
                        System.out.println("Specification " + name + " found in bundle " + res.getId());
                        CheckObr.readSpecs.put(name, cap);
                        return cap;
                    }
                }
            }
        }
        System.err.println("Specification " + name + " not found. ");
        return null;
    }

    private static Capability getImplCapability(String name) {
        for (Resource res : CheckObr.resources) {
            if (ApamMavenPlugin.bundleDependencies.contains(res.getId())) {
                for (Capability cap : res.getCapabilities()) {
                    if (cap.getName().equals("apam-implementation")
                            && (CheckObr.getAttributeInCap(cap, name) != null))
                        return cap;
                }
            }
        }
        return null;
    }

    private static Capability getCompoCapability(String name) {
        for (Resource res : CheckObr.resources) {
            for (Capability cap : res.getCapabilities()) {
                if (cap.getName().equals("apam-implementation")
                        && (CheckObr.getAttributeInCap(cap, "apam-composite") != null)
                        && (CheckObr.getAttributeInCap(cap, "apam-composite").equals("true"))
                        && (CheckObr.getAttributeInCap(cap, name) != null))
                    return cap;
            }
        }
        return null;
    }

    private static void checkFieldTypeDep(ImplementationDependency dep, ApamComponentInfo component) {
        if (dep.specification == null) { // atomic dependency
            AtomicDependency aDep = (AtomicDependency) dep.dependencies.toArray()[0];
            boolean mult = CheckObr.getFieldType(aDep, component);
            if (!mult && (mult != dep.isMultiple)) {
                System.err.println("ERROR: in " + component.getName() + " field " + aDep.fieldName
                        + " is a simple field, while declared multiple.");
            }
            dep.isMultiple = mult;
            return;
        }
        // complex dependency
        // All field must have same multiplicity, and must refer to interfaces provided by the specification.
        String interfs = CheckObr
        .getAttributeInCap(CheckObr.getSpecCapability(dep.specification), "provide-interfaces");
        List<String> specInterfaces = Util.splitList(interfs);
        boolean mult;
        boolean first = true;
        for (AtomicDependency aDep : dep.dependencies) {
            // String fieldType = aDep.fieldType ;
            mult = CheckObr.getFieldType(aDep, component);
            if (first) {
                dep.isMultiple = mult;
                first = false;
            }
            // check multiplicity. All field must have same multiplicity.
            if (mult != dep.isMultiple) {
                if (mult)
                    System.err.println("ERROR: in " + component.getName() + " field " + aDep.fieldName
                            + " is a collection field, while other fields in same dependency are simple.");
                else
                    System.err.println("ERROR: in " + component.getName() + " field " + aDep.fieldName
                            + " is a simple field, while other fields in same dependency are collection.");
            }
            // check specification. All fields must refer to interfaces provided by the specification.
            if ((aDep.fieldType != null) && (specInterfaces != null) && (!specInterfaces.contains(aDep.fieldType))) {
                System.err.println("ERROR: in " + component.getName() + " Field " + aDep.fieldName + " is of type "
                        + aDep.fieldType
                        + " which is not implemented by specification " + dep.specification);
            }
        }
    }

    /**
     * Provided an atomic dependency, find the type, and set it into the dependency.
     * Returns if it is multiple or not.
     * 
     * @param dep
     * @param component
     * @return
     */
    public static boolean getFieldType(AtomicDependency dep, ApamComponentInfo component) {
        if (CheckObr.allFields.contains(dep.fieldName)) {
            System.err.println("ERROR: in " + component.getName() + " field " + dep.fieldName + " allready declared");
        }
        else {
            CheckObr.allFields.add(dep.fieldName);
        }
        Map fields = component.getClassChecker().getFields();
        boolean fieldMultiple = false;
        boolean typeUnknown = false;
        for (Object field : fields.keySet()) {
            if (((String) field).equals(dep.fieldName)) {
                String fieldType = (String) fields.get(field);
                // for arrays, remove the trailing "[]"
                if (fieldType.endsWith("[]")) {
                    fieldType = fieldType.substring(0, fieldType.length() - 2);
                    fieldMultiple = true;
                } else {
                    // check if it is a collection, (set, List, Collection or Vector)
                    if (Arrays.asList(CheckObr.fieldTypeMultiple).contains(fieldType)) {
                        fieldMultiple = true;
                        typeUnknown = true;
                    }
                }
                if ((fieldType != null) && (dep.fieldType != null) && !fieldType.equals(dep.fieldType)) {
                    System.err.println("ERROR: in " + component.getName() + " field " + dep.fieldName + " is of type "
                            + fieldType
                            + " but declared as type " + dep.fieldType);
                }
                // if a collection, the real type is unknown; let type be null.
                if (!typeUnknown)
                    dep.fieldType = fieldType;
                return fieldMultiple;
            }
        }
        System.err.println("ERROR: in " + component.getName() + " Field " + dep.fieldName
                + " declared but not existing in the code");
        return false;
    }
}
