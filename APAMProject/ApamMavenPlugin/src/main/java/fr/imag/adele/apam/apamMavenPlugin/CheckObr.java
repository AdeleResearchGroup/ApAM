package fr.imag.adele.apam.apamMavenPlugin;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
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
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Dependency.AtomicDependency;
import fr.imag.adele.apam.util.Dependency.ImplementationDependency;
import fr.imag.adele.apam.util.Dependency.SpecificationDependency;

//import fr.imag.adele.obrMan.OBRManager;
//import fr.imag.adele.obrMan.OBRManager.Selected;

public class CheckObr {

    private static RepositoryImpl                repo;
    private static Resource[]                    resources;
    private static String[]                      predefAttributes =
                                                                  { "scope", "shared", "visible", "name",
                                                                  "apam-composite", "apam-main-implementation",
                                                                  "require-interface", "require-specification",
                                                                  "require-message" };

    private static final Map<String, Capability> readSpecs        = new HashMap<String, Capability>();

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
        if (spec == null)
            return;
        List<String> implList = new ArrayList<String>();
        for (String messageName : ApamComponentXML.parseArrays(impl)) {
            implList.add(messageName);
        }
        // each element of sp must be found in implList
        for (String sp : ApamComponentXML.parseArrays(spec)) {
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
                System.err.println("validating filter " + f);
                parsedFilter.validateAttr(validAttr, f);
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
        for (String predef : CheckObr.predefAttributes) {
            validAttrs.add(predef);
        }
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

    public static void checkCompoRequire(String implName, String spec, Set<SpecificationDependency> deps) {
        // TODO
    }

    public static void checkImplRequire(String implName, String spec, Set<ImplementationDependency> deps) {
        String fieldType;
        // The dependencies of that implementation
//        Set<String> interfDependencies = new HashSet<String>();
//        Set<String> msgDependencies = new HashSet<String>();
        Set<String> validAttrs = CheckObr.getValidAttributes(CheckObr.getSpecCapability(spec));
        for (ImplementationDependency dep : deps) {
            // System.err.println("validating dependency constraints and preferences....");
            CheckObr.checkFilterList(dep.implementationConstraints, validAttrs);
            CheckObr.checkFilterList(dep.instanceConstraints, validAttrs);
            CheckObr.checkFilterList(dep.implementationPreferences, validAttrs);
            CheckObr.checkFilterList(dep.instancePreferences, validAttrs);

            // Checking complex dependencies
            if (dep.specification != null) {
                for (AtomicDependency aDep : dep.dependencies) {
                    fieldType = aDep.fieldType;
                    // Check if field type is really in the specification
                    String interf = CheckObr.getAttributeInCap(CheckObr.getSpecCapability(dep.specification),
                            "provide-interfaces");
                    if (interf != null) {
                        boolean found = false;
                        for (String inter : ApamComponentXML.parseArrays(interf)) {
                            if (inter.equals(fieldType)) {
                                found = true;
                                break;
                            }
                            if (!found)
                                System.err.println("ERROR: field " + aDep.fieldName + " is of type " + fieldType
                                        + " which does not pertain to specification " + dep.specification);
                        }
                    }
                }
            } // end complex dependency
              // atomic dependency : spec, interface or message. Allready checked
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

}
