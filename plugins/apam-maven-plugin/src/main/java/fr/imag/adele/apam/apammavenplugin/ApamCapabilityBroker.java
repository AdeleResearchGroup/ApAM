package fr.imag.adele.apam.apammavenplugin;

import fr.imag.adele.apam.declarations.*;
import fr.imag.adele.apam.util.CoreMetadataParser;
import org.apache.felix.bundlerepository.Resource;

import java.util.*;

/**
 * Created by thibaud on 11/08/2014.
 */
public class ApamCapabilityBroker {

    private static Map<String, ApamCapability> internalCapabilities = new HashMap<String, ApamCapability>();
    private static Map<String, ApamCapability> externalCapabilities = new HashMap<String, ApamCapability>();

    private static Map<String, List<String>> capabilityVersions = new HashMap<String, List<String>>();

    private static Set<String> missing = new HashSet<String>();


    // Only to return a true value
    public static ApamCapability trueCap = new ApamCapability();

    private static StandaloneACRParser acrResolver;

    public static void setStandaloneACRResolver(StandaloneACRParser acrResolver) {
        ApamCapabilityBroker.acrResolver=acrResolver;
    }


    private void ApamCapabilityBroker() {
    }


    public static void init(List<ComponentDeclaration> components,
                            List<ComponentDeclaration> dependencies) {
        internalCapabilities.clear();
        externalCapabilities.clear();
        capabilityVersions.clear();
        missing.clear();
        if(components!=null) {
            for (ComponentDeclaration dcl : components) {
                internalCapabilities.put(dcl.getName(), new ApamCapability(dcl));
            }
        }

        if(dependencies!= null) {
            for (ComponentDeclaration dcl : dependencies) {
                externalCapabilities.put(dcl.getName(),new ApamCapability(dcl));
            }
        }
    }

    public void addExternalCapability(ApamCapability cap) {
        externalCapabilities.put(cap.getName(),cap);
    }

    public static ApamCapability get(String name) {
        if (name == null) {
            return null;
        }

        // Step 1 : if the capability is declared inside the artifact being built
        ApamCapability cap = internalCapabilities.get(name);

        // Step 2 : if already declared outside (a dependency used several times
        if(cap ==null) {
            cap = externalCapabilities.get(name);
        }

        // Step 3 : try to find the dependency inside the OBR/ACR
        if(cap ==null &&acrResolver!= null) {
//            acrResolver.getApAMComponentResource(name,null);


            // Not inside this component, check if it exists in the ACRs
            // TODO check there in the ACR (and by default in local if no ACR specified)
            // Un problème avec les versions, on ne peux pas récupérer la version d'une dépendance ?
            for (ApamCapability singlecap : StandaloneACRParser.getApAMCapabilities(name, null)) {
                externalCapabilities.put(singlecap.getName(), singlecap);
            }

                //TODO try to build those allowedType props from metadatas
//                    allowedTypes.addAll(cap.getProvideResources());

                // check target's implementation class
//                    String implementationClass = cap.getImplementationClass();
//                    if (implementationClass != null)
//                        allowedTypes.add(new ImplementatioClassReference(
//                                implementationClass));
            cap = externalCapabilities.get(name);
        }

        return cap;
    }

    public static ApamCapability get(ComponentReference<?> reference) {
        if (reference == null) {
            return null;
        }
        if (reference.getName().equals(CoreMetadataParser.UNDEFINED)) {
            return null;
        }

        ApamCapability cap = get(reference.getName());


        //TODO, check the OBR there (plus simple et ça fait un endroit unique)
        // TODO build capability from the obr metadata (plus chaud)

        /**
         * Dependencies checking removed from there
         * They must be checked against the OBR
         *
         if (cap == null && !missing.contains(reference.getName())) {
         missing.add(reference.getName());
         CheckObr.error("Component " + reference.getName()
         + " is not in your Maven dependencies.");
         }
         */

        return cap;
    }

    public static ComponentDeclaration getDcl(String name) {
        if (name == null) {
            return null;
        }
        if (name.equals(CoreMetadataParser.UNDEFINED)) {
            return null;
        }

        if (get(name) != null) {
            return get(name).getDcl();
        }
        return null;
    }

    public static ComponentDeclaration getDcl(ComponentReference<?> reference) {
        if (reference == null) {
            return null;
        }
        if (reference.getName().equals(CoreMetadataParser.UNDEFINED)) {
            return null;
        }

        if (get(reference.getName()) != null) {
            return get(reference.getName()).getDcl();
        }
        return null;
    }

    public static ApamCapability getTrueCap() {
        return trueCap;
    }


    public static ApamCapability getGroup(String declarationName) {
        //WARNING of the infinite call because getgroup on a capability calls the Broker again
        ComponentDeclaration dcl = ApamCapabilityBroker.getDcl(declarationName);

        if(dcl!=null
                && dcl.getGroupReference() !=null
                && dcl.getGroupReference().getName() !=null) {
            return get(dcl.getGroupReference().getName());
        }
        return null;
    }

    /**
     * Warning: should be used only once in generateProperty. finalProperties
     * contains the attributes generated in OBR i.e. the right attributes.
     * properties contains the attributes found in the xml, i.e. before to check
     * and before to compute inheritance. At the end of the component processing
     * we switch in order to use the right attributes if the component is used
     * after its processing
     *
     * @param attr
     * @param value
     */
    public boolean putAttr(String declarationName, String attr, String value) {
        ApamCapability cap = ApamCapabilityBroker.get(declarationName);

        if(cap!=null) {
            return cap.putAttr(attr, value);
        } else {
            return false;
        }
    }

    public static void freeze(String declarationName) {
        ApamCapability cap = ApamCapabilityBroker.get(declarationName);

        if(cap!=null) {
            cap.freeze();
        }
    }

    public static boolean isFinalized(String declarationName) {
        ApamCapability cap = ApamCapabilityBroker.get(declarationName);

        if(cap!=null) {
            return cap.isFinalized();
        } else {
            return false;
        }
    }

    public static String getProperty(String declarationName, String attributeName) {
        ApamCapability cap = ApamCapabilityBroker.get(declarationName);

        if(cap!=null) {
            return cap.getProperty(attributeName);
        } else {
            return null;
        }
    }

    public static Map<String, String> getProperties(String declarationName) {
        ApamCapability cap = ApamCapabilityBroker.get(declarationName);

        if(cap!=null) {
            return cap.getProperties();
        } else {
            return null;
        }
    }


    // Return the definition at the current component level
    public static String getLocalAttrDefinition(String declarationName, String attributeName) {
        ApamCapability cap = ApamCapabilityBroker.get(declarationName);

        if(cap!=null) {
            return cap.getLocalAttrDefinition(attributeName);
        } else {
            return null;
        }
    }

    public static String getAttrDefinition(String declarationName, String attributeName) {
        ApamCapability cap = ApamCapabilityBroker.get(declarationName);

        if(cap!=null) {
            return cap.getAttrDefinition(attributeName);
        } else {
            return null;
        }
    }

    public static String getAttrDefault(String declarationName, String attributeName) {
        ApamCapability cap = ApamCapabilityBroker.get(declarationName);

        if(cap!=null) {
            return cap.getAttrDefault(attributeName);
        } else {
            return null;
        }
    }

    /**
     * returns all the attribute that can be found associated with this
     * component members. i.e. all the actual attributes plus those defined on
     * component, and those defined above.
     *
     * @return
     */
    public static Map<String, String> getValidAttrNames(String declarationName) {
        ApamCapability cap = ApamCapabilityBroker.get(declarationName);

        if(cap!=null) {
            return cap.getValidAttrNames();
        } else {
            return null;
        }
    }

    public static Set<InterfaceReference> getProvideInterfaces(String declarationName) {
        ComponentDeclaration dcl = ApamCapabilityBroker.getDcl(declarationName);

        if(dcl!=null) {
            return dcl.getProvidedResources(InterfaceReference.class);
        }
        return null;
    }

    public static Set<ResourceReference> getProvideResources(String declarationName) {
        ComponentDeclaration dcl = ApamCapabilityBroker.getDcl(declarationName);

        if(dcl!=null) {
            return dcl.getProvidedResources();
        }
        return null;
    }

    public static Set<MessageReference> getProvideMessages(String declarationName) {
        ComponentDeclaration dcl = ApamCapabilityBroker.getDcl(declarationName);

        if(dcl!=null) {
            return dcl.getProvidedResources(MessageReference.class);
        }
        return null;
    }

    public static String getImplementationClass(String declarationName) {
        ComponentDeclaration dcl = ApamCapabilityBroker.getDcl(declarationName);

        if (dcl !=null && dcl instanceof AtomicImplementationDeclaration) {
            return ((AtomicImplementationDeclaration) dcl).getClassName();
        } else {
            return null;
        }
    }


    public static String getName(String declarationName) {
        ComponentDeclaration dcl = ApamCapabilityBroker.getDcl(declarationName);
        if(dcl!=null) {
            return dcl.getName();
        }
        return null;
    }

    /**
     * return null if Shared is undefined, true of false if it is defined as
     * true or false.
     *
     * @return
     */
    public static String shared(String declarationName) {
        ComponentDeclaration dcl = ApamCapabilityBroker.getDcl(declarationName);

        if (dcl!=null &&dcl.isDefinedShared()) {
            return Boolean.toString(dcl.isShared());
        }
        return null;
    }

    /**
     * return null if Instantiable is undefined, true of false if it is defined
     * as true or false.
     *
     * @return
     */
    public static String instantiable(String declarationName) {
        ComponentDeclaration dcl = ApamCapabilityBroker.getDcl(declarationName);

        if (dcl != null &&dcl.isDefinedInstantiable()) {
            return Boolean.toString(dcl.isInstantiable());
        }
        return null;
    }

    /**
     * return null if Singleton is undefined, true of false if it is defined as
     * true or false.
     *
     * @return
     */
    public static String singleton(String declarationName) {
        ComponentDeclaration dcl = ApamCapabilityBroker.getDcl(declarationName);

        if (dcl != null&&dcl.isDefinedSingleton()) {
            return Boolean.toString(dcl.isSingleton());
        }
        return null;
    }

}
