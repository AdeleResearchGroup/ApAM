package fr.imag.adele.apam.apamMavenPlugin;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
import fr.imag.adele.apam.core.SpecificationDeclaration;
import fr.imag.adele.apam.core.SpecificationReference;
import fr.imag.adele.apam.util.OBR;

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

    public StringBuffer writeOBRFile(List<ComponentDeclaration> components) {
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

        Set<InterfaceReference> interfaces = component.getProvidedResources(InterfaceReference.class);
        if ((interfaces != null) && !interfaces.isEmpty()) {
            int l = interfaces.size();
            int i = 1;
            obrContent.append("      <p n='" + OBR.A_PROVIDE_INTERFACES + "' v='[");
            for (InterfaceReference interf : interfaces) {
                if (i<l)
                    obrContent.append(interf.getJavaType() + ", ");
                else
                    obrContent.append(interf.getJavaType() + "]' /> \n");
                i++ ;
            }
        }

        Set<MessageReference> messages = component.getProvidedResources(MessageReference.class);
        if ((messages != null) && !messages.isEmpty()) {
            int l = messages.size();
            int i = 1;
            obrContent.append("      <p n='" + OBR.A_PROVIDE_MESSAGES + "' v='[");
            for (MessageReference mess : messages) {
                if (i < l)
                    obrContent.append(mess.getJavaType() + ", ");
                else
                    obrContent.append(mess.getJavaType() + "]' /> \n");
                i++;
            }
        }

        if (component instanceof ImplementationDeclaration) {
            SpecificationReference spec = ((ImplementationDeclaration) component).getSpecification();
            if ((spec != null) && !spec.getName().isEmpty()) {
                obrContent.append("      <p n='" + OBR.A_PROVIDE_SPECIFICATION + "' v='" + spec + "' />\n");
                CheckObr.checkImplProvide(component.getName(), spec.getName(), interfaces, messages);
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
            String type = definition.getType();
            if (type != null) {
                if (type.equals("string") || type.equals("int") || type.equals("bool")) {
                    tempContent = tempContent + (" v='" + (definition.getType()) + "'");
                    CheckObr.checkAttrType(definition.getName(), (String) definition.getDefaultValue(), type);
                } else
                    CheckObr.error("Invalid type " + type + " in attribute definition " + definition.getName()
                            + ". Supported: string, int, bool.");
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
            	
            	InterfaceReference refInterface			= dep.getTarget().as(InterfaceReference.class);
            	MessageReference refMessage 			= dep.getTarget().as(MessageReference.class);	
            	SpecificationReference refSpecification = dep.getTarget().as(SpecificationReference.class);;
            	
                if (refInterface != null) {
                    obrContent.append("      <p n='" + OBR.A_REQUIRE_INTERFACE + "' v='" + refInterface.getJavaType()
                            + "' /> \n");
                }
                else if (refSpecification != null) {
                    obrContent.append("      <p n='" + OBR.A_REQUIRE_SPECIFICATION + "' v='"
                            + refSpecification.getName()
                            + "' /> \n");
                }
                else if (refMessage != null) {
                    obrContent.append("      <p n='" + OBR.A_REQUIRE_MESSAGE + "' v='" + refMessage.getJavaType()
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
        	
        	CompositeDeclaration composite = (CompositeDeclaration) component;
            obrContent.append("   <capability name='" + OBR.CAPABILITY_IMPLEMENTATION + "'>\n");
            obrContent.append("      <p n='" + CST.A_COMPOSITE + "' v='true' />\n");
            obrContent.append("      <p n='" + CST.A_MAIN_IMPLEMENTATION + "' v='"
                    + composite.getMainImplementation().getName()
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
