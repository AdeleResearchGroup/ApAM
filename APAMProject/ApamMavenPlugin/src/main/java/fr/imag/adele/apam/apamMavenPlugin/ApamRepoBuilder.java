package fr.imag.adele.apam.apamMavenPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.xml.parser.SchemaResolver;

import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.core.AtomicImplementationDeclaration;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.ImplementationDeclaration;
import fr.imag.adele.apam.core.InstanceDeclaration;
import fr.imag.adele.apam.core.InterfaceReference;
import fr.imag.adele.apam.core.MessageReference;
import fr.imag.adele.apam.core.PropertyDefinition;
import fr.imag.adele.apam.core.ResourceReference.ResourceType;
import fr.imag.adele.apam.core.SpecificationDeclaration;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.ApamComponentXML;
import fr.imag.adele.apam.util.OBR;
//import fr.imag.adele.apam.util.ApamComponentXML.ComponentDeclaration;
//import fr.imag.adele.apam.util.ApamComponentXML.SimpleProperty;
//import fr.imag.adele.apam.util.Dependency.SpecificationDependency;
import fr.imag.adele.apam.util.Util;

public class ApamRepoBuilder {

    /**
     * Metadata (in internal format).
     */


    /**
     * Flag describing if we need or not use local XSD files (i.e. use the {@link SchemaResolver} or not). If
     * <code>true</code> the local XSD are not used.
     */

    public ApamRepoBuilder(String defaultOBRRepo) {
        // OBRManager obr = new OBRManager(defaultOBRRepo);
        CheckObr.init(defaultOBRRepo + "\\repository.xml");
    }

    public StringBuffer writeOBRFile(Set<ComponentDeclaration> components) {
        StringBuffer obrContent = new StringBuffer("<obr> \n");
        for (ComponentDeclaration comp : components) {
            // printElement(comp.m_componentMetadata, "");
            printOBRElement(obrContent, comp, "");
        }
        obrContent.append("</obr> \n");
        return obrContent;
    }

    private void printProvided(StringBuffer obrContent, ComponentDeclaration component) {
        obrContent.append("      <p n='name' v='" + component.getName() + "' />\n");

        String interfaces = component.getProvidedRessourceString(ResourceType.INTERFACE);
        if ((interfaces != null) && !interfaces.isEmpty())
            obrContent.append("      <p n='" + OBR.A_PROVIDE_INTERFACES + "' v='" + interfaces + "' /> \n");

        String messages = component.getProvidedRessourceString(ResourceType.MESSAGE);
        if (messages != null)
            obrContent.append("      <p n='" + OBR.A_PROVIDE_MESSAGES + "' v='" + messages + "' />\n");

        if (component instanceof ImplementationDeclaration) {
            String spec = ((ImplementationDeclaration) component).getSpecification().getName();
            if ((spec != null) && !spec.isEmpty()) {
                obrContent.append("      <p n='" + OBR.A_PROVIDE_SPECIFICATION + "' v='" + spec + "' />\n");
                CheckObr.checkImplProvide(component.getName(), spec, interfaces, messages);
            }
        }
    }


    private void printProperties(StringBuffer obrContent, ComponentDeclaration component) {
        // property attributes
        Map<String, Object> properties = component.getProperties();
        for (String propertyName : properties.keySet()) {
            obrContent.append("      <p n='" + propertyName + "' v='"
                    + properties.get(propertyName) + "' />\n");
        }

        // definition attributes

        List<PropertyDefinition> definitions = component.getPropertyDefinitions();
        for (PropertyDefinition definition : definitions) {
            String tempContent = "      <p n='" + OBR.A_DEFINITION_PREFIX + definition.getName() + "'";
            if (definition.getDefaultValue() != null) {
                tempContent = tempContent + (" v='" + (definition.getDefaultValue()) + "'");
            }
            tempContent = tempContent + " />\n";
            obrContent.append(tempContent);
        }

        // Check Consistency
        if (component instanceof ImplementationDeclaration)
            CheckObr.checkImplAttributes((ImplementationDeclaration) component);
    }

    private void printRequire(StringBuffer obrContent, ComponentDeclaration component) {
        if (component instanceof SpecificationDeclaration) {
            for (DependencyDeclaration dep : component.getDependencies()) {
                if (dep.getResource() instanceof InterfaceReference) {
                    obrContent.append("      <p n='" + OBR.A_REQUIRE_INTERFACE + "' v='" + dep.getResource().getName()
                            + "' /> \n");
                } else if (dep.getResource() instanceof SpecificationReference) {
                    obrContent.append("      <p n='" + OBR.A_REQUIRE_SPECIFICATION + "' v='"
                            + dep.getResource().getName()
                            + "' /> \n");
                } else if (dep.getResource() instanceof MessageReference) {
                    obrContent.append("      <p n='" + OBR.A_REQUIRE_MESSAGE + "' v='" + dep.getResource().getName()
                            + "' /> \n");
                }
            }
            return;
        }

        // composite and implems
        CheckObr.checkRequire(component);
    }

    private void
    printOBRElement(StringBuffer obrContent, ComponentDeclaration component, String indent) {
        // String spec = component.getSpecification();
        // messages
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

        //headers
        if (component instanceof AtomicImplementationDeclaration) {
            obrContent.append("   <capability name='" + OBR.CAPABILITY_IMPLEMENTATION + "'>\n");
        }
        if (component instanceof CompositeDeclaration) {
            obrContent.append("   <capability name='" + OBR.CAPABILITY_IMPLEMENTATION + "'>\n");
            obrContent.append("      <p n='" + CST.A_COMPOSITE + "' v='" + (component instanceof CompositeDeclaration)
                    + "' />\n");
            obrContent.append("      <p n='" + CST.A_MAIN_IMPLEMENTATION + "' v='"
                    + ((CompositeDeclaration) component).getMainImplementation().getName()
                    + "' />\n");
            CheckObr.checkCompoMain((CompositeDeclaration) component);
        }
        if (component instanceof SpecificationDeclaration) {
            obrContent.append("   <capability name='" + OBR.CAPABILITY_SPECIFICATION + "'>\n");
        }

        if (component instanceof InstanceDeclaration) {
            CheckObr.checkInstance((InstanceDeclaration) component);
            return;
        }

        // provide clause
        printProvided(obrContent, component);

        // definition attributes
        printProperties(obrContent, component);

        // Require, fields and constraints
        printRequire(obrContent, component);

        obrContent.append("   </capability>\n");

    }

}
