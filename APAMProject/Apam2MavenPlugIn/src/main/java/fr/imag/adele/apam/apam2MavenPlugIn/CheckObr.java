package fr.imag.adele.apam.apam2MavenPlugIn;

import java.io.File;
import java.io.ObjectInputStream.GetField;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.apache.felix.ipojo.manipulation.ClassChecker;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Resource;

import fr.imag.adele.apam.apamImpl.Dependency.AtomicDependency;
import fr.imag.adele.apam.apamImpl.Dependency.ImplementationDependency;
import fr.imag.adele.apam.apamImpl.Dependency.SpecificationDependency;
import fr.imag.adele.apam.util.ApamFilter;

//import fr.imag.adele.obrMan.OBRManager;
//import fr.imag.adele.obrMan.OBRManager.Selected;

public class CheckObr {

    // private static OBRManager obr;
    private static String         defaultOBRRepo;
    private static RepositoryImpl repo;
    private static Resource[]     resources;
    private static String[]       predefAttributes = { "scope", "shared", "visible", "name" };

    public static void init(String defaultOBRRepo) {
        File theRepo = new File(defaultOBRRepo);
        CheckObr.defaultOBRRepo = defaultOBRRepo;
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
        for (String messageName : Apam2RepoBuilder.parseArrays(impl)) {
            implList.add(messageName);
        }
        // each element of sp must be found in implList
        for (String sp : Apam2RepoBuilder.parseArrays(spec)) {
            if (!implList.contains(sp)) {
                System.err.println(msg + sp + ". Declared: " + impl);
            }
        }
    }

    public static void checkFilterList(List<String> filters) {
        for (String f : filters) {
            try {
                ApamFilter parsedFilter = ApamFilter.newInstance(f);
                System.err.println("validating filter " + f);
                parsedFilter.validateAttr();
            } catch (InvalidSyntaxException e) {
                e.printStackTrace();
            }
        }

    }

    private static boolean capContainsDefAttr(Capability cap, String attr) {
        for (String predef : CheckObr.predefAttributes) {
            if (attr.equalsIgnoreCase(predef))
                return true;
        }
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
        Set<String> interfDependencies = new HashSet<String>();
        Set<String> msgDependencies = new HashSet<String>();

        for (ImplementationDependency dep : deps) {
            System.err.println("validating dependency constraints and preferences....");
            CheckObr.checkFilterList(dep.implementationConstraints);
            CheckObr.checkFilterList(dep.instanceConstraints);
            CheckObr.checkFilterList(dep.implementationPreferences);
            CheckObr.checkFilterList(dep.instancePreferences);

            // Checking complex dependencies
            if (dep.specification != null) {
                for (AtomicDependency aDep : dep.dependencies) {
                    fieldType = aDep.fieldType;
                    // Check if field type is really in the specification
                    String interf = CheckObr.getAttributeInCap(CheckObr.getSpecCapability(dep.specification),
                            "provide-interfaces");
                    if (interf != null) {
                        boolean found = false;
                        for (String inter : Apam2RepoBuilder.parseArrays(interf)) {
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

//            else {
//                AtomicDependency aDep = (AtomicDependency) dep.dependencies.toArray()[0];
//                fieldType = aDep.fieldType;
//                switch (aDep.targetKind) {
//                    // INTERFACE, PUSH_MESSAGE, PULL_MESSAGE, SPECIFICATION
//                    case INTERFACE:
//                        interfDependencies.add(fieldType);
//                        break;
//                    case PULL_MESSAGE:
//                        msgDependencies.add(fieldType);
//                        break;
//                    case PUSH_MESSAGE:
//                        msgDependencies.add(fieldType);
//                        break;
//                }
//            }
//        }
//    }
    // the arrays : interfDependencies, msgDependenciesare are ready
    // compare with its spec dependencies to see if the impl has at least the same dependencies.
//        Capability specCap = CheckObr.getSpecCapability(spec);
//        if (specCap == null)
//            return;
//        String interfaces = CheckObr.getAttributeInCap(specCap, "provide-interfaces");
//        for (String inter : Apam2RepoBuilder.parseArrays(interfaces)) {
//            if (!interfDependencies.contains(inter)) {
//                System.err.println("ERROR: Implementation " + implName + " should implement interface " + inter
//                        + " as declared in " + spec);
//            }
//        }
//        
//        String msgs = CheckObr.getAttributeInCap(specCap, "provide-messages");
//        for (String inter : Apam2RepoBuilder.parseArrays(msgs)) {
//            if (!msgDependencies.contains(inter)) {
//                System.err.println("ERROR: Implementation " + implName + " should produce messages " + inter
//                        + " as declared in " + spec);
//            }
//        }
//    }

//    private static String checkField(String fieldType, String fieldName) {
//        String compFieldType = CheckObr.getFieldType(Apam2RepoBuilder.ck, fieldName);
//        if (!fieldType.equals(compFieldType)) {
//            System.err.println("ERROR: type of field " + fieldName + " should be " + compFieldType
//                        + " instead of " + fieldType);
//        }
//        return compFieldType;
//    }

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
        for (Resource res : CheckObr.resources) {
            for (Capability cap : res.getCapabilities()) {
                if (cap.getName().equals("apam-specification")
                        && (CheckObr.getAttributeInCap(cap, "name").equals(name))) {
                    System.out.println("Specification " + name + " found in bundle " + res.getId());
                    return cap;
                }
            }
        }
        System.err.println("Specification " + name + " not found. ");
        return null;
    }

    private static Capability getImplCapability(String name) {
        for (Resource res : CheckObr.resources) {
            for (Capability cap : res.getCapabilities()) {
                if (cap.getName().equals("apam-implementation")
                        && (CheckObr.getAttributeInCap(cap, name) != null))
                    return cap;
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

//    public static String getFieldType(ClassChecker ck, String fieldName) {
//        Map fields = ck.getFields();
//        for (Object field : fields.keySet()) {
//            if (((String) field).equals(fieldName)) {
//                return (String) fields.get(field);
//            }
//        }
//        return null;
//    }
}
