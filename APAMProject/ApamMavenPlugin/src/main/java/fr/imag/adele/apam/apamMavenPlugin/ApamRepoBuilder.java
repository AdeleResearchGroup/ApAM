package fr.imag.adele.apam.apamMavenPlugin;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.xml.parser.SchemaResolver;
import org.apache.maven.artifact.versioning.VersionRange;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.ComponentReference;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.ImplementationReference;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.core.ResourceReference;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.MessageReference;
import fr.imag.adele.apam.core.PropertyDefinition;
import fr.imag.adele.apam.core.ResolvableReference;
import fr.imag.adele.apam.core.SpecificationDeclaration;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.Util;

public class ApamRepoBuilder {

    /**
     * Metadata (in internal format).
     */

    private final List<ComponentDeclaration>  components;

    private final Set<SpecificationReference> bundleRequiresSpecifications = new HashSet<SpecificationReference>();

    /**
     * Flag describing if we need or not use local XSD files (i.e. use the {@link SchemaResolver} or not). If
     * <code>true</code> the local XSD are not used.
     */

    public ApamRepoBuilder(List<ComponentDeclaration> components, List<URL> OBRRepos) {
        this.components = components;
        ApamCapability.init(components, OBRRepos);
    }

    public StringBuffer writeOBRFile() {

        // if a component is defined twice, or error is name space, remove the second definition
        checkDoubleDefinition();

        StringBuffer obrContent = new StringBuffer("<obr> \n");

        // print maven groupId, artifactId and version as resource capability
        printOBRMavenElement(obrContent, "");

        for (ComponentDeclaration comp : components) {
            if (comp instanceof SpecificationDeclaration)
                printOBRElement(obrContent, comp, "");
        }
        for (ComponentDeclaration comp : components) {
            if (comp instanceof AtomicImplementationDeclaration)
                printOBRElement(obrContent, comp, "");
        }
        for (ComponentDeclaration comp : components) {
            if (comp instanceof CompositeDeclaration)
                printOBRElement(obrContent, comp, "");
        }
        for (ComponentDeclaration comp : components) {
            if (comp instanceof InstanceDeclaration)
                printOBRElement(obrContent, comp, "");
        }

        generateFilters(obrContent);

        obrContent.append("</obr> \n");
        return obrContent;
    }

    private void printOBRMavenElement(StringBuffer obrContent, String indent) {
        obrContent.append("   <capability name='" + CST.MAVEN + "'>\n");
        obrContent.append("      <p n='" + CST.GROUP_ID + "' v='" + OBRGeneratorMojo.currentProjectGroupId + "' />\n");
        obrContent.append("      <p n='" + CST.ARTIFACT_ID + "' v='" + OBRGeneratorMojo.currentProjectArtifactId + "' />\n");
        obrContent.append("      <p n='" + CST.VERSION + "' v='" + OBRGeneratorMojo.currentProjectVersion + "' />\n");
        obrContent.append("   </capability>\n");
    }

    private void printOBRElement(StringBuffer obrContent, ComponentDeclaration component, String indent) {
        System.out.print("Checking ");
        if (component instanceof ImplementationDeclaration)
            System.out.print("implementation ");
        if (component instanceof CompositeDeclaration)
            System.out.print("composite ");
        if (component instanceof InstanceDeclaration)
            System.out.print("instance ");
        if (component instanceof SpecificationDeclaration)
            System.out.print("specification ");
        System.out.println(component.getName() + " ...");

        // headers
        obrContent.append("   <capability name='" + CST.CAPABILITY_COMPONENT + "'>\n");
        generateProperty(obrContent, component, CST.NAME, component.getName());

        if (component instanceof ImplementationDeclaration) {
            generateProperty(obrContent, component, CST.COMPONENT_TYPE, CST.IMPLEMENTATION);
            generateProperty(obrContent, component, CST.IMPLNAME, component.getName());
        }

        if (component instanceof InstanceDeclaration) {
            generateProperty(obrContent, component, CST.COMPONENT_TYPE, CST.INSTANCE);
            generateProperty(obrContent, component, CST.INSTNAME, component.getName());
        }

        if (component instanceof CompositeDeclaration) {
            CompositeDeclaration composite = (CompositeDeclaration) component;
            generateProperty(obrContent, component, CST.APAM_COMPOSITE, CST.V_TRUE);
            generateProperty(obrContent, component, CST.APAM_MAIN_COMPONENT, composite.getMainComponent().getName());
            CheckObr.checkCompoMain((CompositeDeclaration) component);
            // check the information for composite : grant, start, own, visibility etc.
            CheckObr.checkCompositeContent((CompositeDeclaration) component);
        }

        if (component instanceof SpecificationDeclaration) {
            generateProperty(obrContent, component, CST.COMPONENT_TYPE, CST.SPECIFICATION);
            generateProperty(obrContent, component, CST.SPECNAME, component.getName());
        }

        // provide clause
        printProvided(obrContent, component);

        // properties
        printProperties(obrContent, component);

        // Require, fields and constraints
        printRequire(obrContent, component);

        generateTypedProperty(obrContent, component, "version", "version", OBRGeneratorMojo.thisBundleVersion);
        ApamCapability.get(component.getReference()).finalize();
        obrContent.append("   </capability>\n");
    }

    private void printProvided(StringBuffer obrContent, ComponentDeclaration component) {
        if (component instanceof InstanceDeclaration)
            return;

        Set<InterfaceReference> interfaces = component.getProvidedResources(InterfaceReference.class);
        String val = setReference2String(interfaces);
        if (val != null)
            generateProperty(obrContent, component, CST.PROVIDE_INTERFACES, setReference2String(interfaces));

        Set<MessageReference> messages = component.getProvidedResources(MessageReference.class);
        val = setReference2String(messages);
        if (val != null)
            generateProperty(obrContent, component, CST.PROVIDE_MESSAGES, val);

        if (component instanceof ImplementationDeclaration) {
            SpecificationReference spec = ((ImplementationDeclaration) component).getSpecification();
            if ((spec != null) && !spec.getName().isEmpty()) {
                generateProperty(obrContent, component, CST.PROVIDE_SPECIFICATION, spec.getName());
                bundleRequiresSpecifications.add(spec);
                CheckObr.checkImplProvide(component, spec.getName(), interfaces, messages);
            }
        }
    }

    private void printProperties(StringBuffer obrContent, ComponentDeclaration component) {
        Map<String, String> properties = CheckObr.getValidProperties(component);
        for (String attr : properties.keySet()) {
            generateProperty(obrContent, component, attr, properties.get(attr));
        }

        // definition attributes
        List<PropertyDefinition> definitions = component.getPropertyDefinitions();
        for (PropertyDefinition definition : definitions) {
            // String tempContent = "      <p n='" + CST.A_DEFINITION_PREFIX + definition.getName() + "'";
            String type = definition.getType();
            String defaultValue = definition.getDefaultValue();

            if (defaultValue == null)
                defaultValue = "";

            if (type != null) {
                String typeString = null;
                if (type.equals("string") || type.equals("int") || type.equals("boolean")) {
                    // Ignored because the value can be null
                    // if (Util.checkAttrType(definition.getName(), definition.getDefaultValue(), type))
                    typeString = type;
                } else {
                    // check for enum types
                    if ((type.charAt(0) == '{') || (type.charAt(0) == '[')) {
                        typeString = "[;";
                        for (String one : Util.split(type)) {
                            typeString += one + ";";
                        }
                        typeString += "]";
                    } else
                        CheckObr.error("Invalid type " + type + " in attribute definition " + definition.getName()
                                + ". Supported: string, int, boolean, enumeration.");
                }
                if (typeString != null) {
                    // tempContent = tempContent + (" v='" + typeString + "' />\n");
                    // obrContent.append(tempContent);
                    generateTypedProperty(obrContent, component, CST.DEFINITION_PREFIX + definition.getName(),
                            typeString, defaultValue);
                }
            }
        }
    }

    /**
     * provided a set of resources references (interface or messages) fr.mag....A , B, C references produces a string
     * "[;fr.imag....A;B;C;]"
     * 
     * @param refs
     * @return
     */

    private String setReference2String(Set<? extends ResolvableReference> refs) {
        if (refs.isEmpty())
            return null;
        int l = refs.size();
        int i = 1;

        String val = "[;";
        for (ResolvableReference mess : refs) {
            if (i < l)
                val += mess.getName() + ";";
            else
                val += mess.getName() + ";]";
            i++;
        }
        return val;
    }

    private void printRequire(StringBuffer obrContent, ComponentDeclaration component) {
        // We do not generate dependencies for specification to remain lazy
        // the spec version is mentionned in the implementations that implement that spec.
        if (component instanceof ImplementationDeclaration) {
            Set<ResolvableReference> resRef = new HashSet<ResolvableReference>();
            for (DependencyDeclaration dep : component.getDependencies()) {
                if (dep.getTarget().as(SpecificationReference.class) != null) {
                    bundleRequiresSpecifications.add(dep.getTarget().as(SpecificationReference.class));
                }
            }
        }

        // all components : checks dependencies and constraints
        CheckObr.checkRequire(component);
    }

    private void generateFilters(StringBuffer obrContent) {
        /**
         * Generate filters for all provided specifications
         */
        // VersionRange version ;
        for (SpecificationReference res : bundleRequiresSpecifications) {
            generateRequire(obrContent, res.getName(), getVersionExpression(res.getName()));
        }
    }

    private void generateProperty(StringBuffer obrContent, ComponentDeclaration component, String attr, String value) {
        if (ApamCapability.get(component.getReference()).putAttr(attr, value)) {
            obrContent.append("      <p n='" + attr + "' v='" + value + "' />\n");
            return;
        }
        // CheckObr.error ("Property " + attr + " already defined for  " + component.getName()) ;
    }

    private void generateTypedProperty(StringBuffer obrContent, ComponentDeclaration component, String attr,
            String type, String value) {
        if (ApamCapability.get(component.getReference()).putAttr(attr, value)) {
            obrContent.append("      <p n='" + attr + "' t='" + type + "' v='" + value + "' />\n");
            return;
        }
        // CheckObr.error ("Property " + attr + " already defined for  " + component.getName()) ;
    }

    private void generateRequire(StringBuffer obrContent, String target, String version) {
        if (version == null) {
            obrContent.append("   <require name='apam-component' filter='(name=" + target
                    + ")' extend='false' multiple='false' optional='false'>"
                    + " specification dependency toward " + target + "</require>\n");
        } else {
            obrContent.append("   <require name='apam-component' filter='(&amp;(name=" + target + ")" + version
                    + ")' extend='false' multiple='false' optional='false'>"
                    + " specification dependency toward " + target + "</require>\n");
        }
    }

    private String getVersionExpression(String name) {
        VersionRange range = getVersionRange(name);
        if (range == null)
            return null;
        String version;
        if (range.toString().indexOf('-') != -1)
            version = range.toString().substring(0, range.toString().indexOf('-'));
        else
            version = range.toString().replace('-', '.');
        return "(version&gt;=" + version + ")";
    }

    private VersionRange getVersionRange(String name) {
        if (OBRGeneratorMojo.versionRange.containsKey(name)) {
            return OBRGeneratorMojo.versionRange.get(name);
        }
        return null;
    }

    private void checkDoubleDefinition() {
        Set<String> defined = new HashSet<String>();
        List<ComponentDeclaration> dcl = new ArrayList<ComponentDeclaration>(components);

        /*
         * Verify in the current project
         */
        for (ComponentDeclaration comp : dcl) {
            if (defined.contains(comp.getName())) {
                CheckObr.error("In " + comp + ": component " + comp.getName() + " already defined");
                components.remove(comp);
            }
            else
                defined.add(comp.getName());
        }

        /*
         * Verify if the components we are building were not in already processed
         * in another project in the same built
         * 
         * NOTE this causes problems because a lot of information is kept in static
         * variables that are shared in the same build execution.
         */
        for (ComponentDeclaration comp : new ArrayList<ComponentDeclaration>(components)) {

            ApamCapability existingDefinition = ApamCapability.get(comp.getReference());

            /*
             * never built is OK to process
             */
            if (existingDefinition == null)
                continue;

            /*
             * already built in the repository is OK to process
             */
            if (!existingDefinition.isFinalized())
                continue;

            /*
             * Already processed in this build execution
             */
            CheckObr.error("Component " + comp.getName() + " is already defined in another project in this build");
            components.remove(comp);
        }

    }
}
