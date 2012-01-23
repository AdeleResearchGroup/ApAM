package fr.imag.adele.apam.apamMavenPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.xml.parser.SchemaResolver;

import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.util.ApamComponentXML;
import fr.imag.adele.apam.util.OBR;
import fr.imag.adele.apam.util.ApamComponentXML.ApamComponentInfo;
import fr.imag.adele.apam.util.ApamComponentXML.SimpleProperty;
import fr.imag.adele.apam.util.Dependency.SpecificationDependency;

public class ApamRepoBuilder {

    /**
     * Metadata (in internal format).
     */
    private final List<Element> m_metadata = new ArrayList<Element>();

    /**
     * Flag describing if we need or not use local XSD files (i.e. use the {@link SchemaResolver} or not). If
     * <code>true</code> the local XSD are not used.
     */

    public ApamRepoBuilder(String defaultOBRRepo) {
        // OBRManager obr = new OBRManager(defaultOBRRepo);
        CheckObr.init(defaultOBRRepo + "\\repository.xml");
    }

    public boolean writeOBRFile(String obrFileStr, File metadataFile, InputStream is,
            File jarFile, File outputDirectory) {
        File obrFile = new File(obrFileStr);
        boolean okMetadata = false;
        StringBuffer obrContent = new StringBuffer("<obr> \n");
        JarFile jar = null;
        try {
            jar = new JarFile(jarFile);
            okMetadata = writeMetadataOBRFile(obrContent, metadataFile, is, jar, outputDirectory);
            jar.close();
        } catch (IOException e2) {
            System.err.println("cannot read the jar file : " + jarFile.getAbsolutePath());
            return false;
        }
        // System.out.println(obrContent);

        if (!okMetadata)
            return false;
        obrContent.append("</obr> \n");

        OutputStream obr;
        try {
            obr = new FileOutputStream(obrFile);
        } catch (FileNotFoundException e) {
            // System.err.println("Cannot open for writing : " + obrFile.getAbsolutePath());
            return false;
        }
        try {
            obr.write(obrContent.toString().getBytes());
        } catch (IOException e) {
            // System.err.println("Cannot write into : " + obrFile.getAbsolutePath());
            try {
                obr.close();
            } catch (IOException e1) {
            }
            return false;
        }
        try {
            obr.flush();
            obr.close();
        } catch (IOException e) {
            // System.err.println("Cannot close : " + obrFile.getAbsolutePath());
            return false;
        }
        return true;
    }

    public boolean writeMetadataOBRFile(StringBuffer obrContent, File metadata, InputStream is, JarFile jarFile,
            File outputDirectory) {
        if (metadata == null) {
            System.out.println(" no metadata");
            return false;
        }
        ApamComponentXML compXML = new ApamComponentXML(outputDirectory, metadata, is, jarFile);
        List<ApamComponentInfo> components = compXML.getComponents();

        for (ApamComponentInfo comp : components) {
            // printElement(comp.m_componentMetadata, "");
            printOBRElement(obrContent, comp, "", jarFile);
        }
        return true;
    }

    private void printProvided(StringBuffer obrContent, ApamComponentInfo component, JarFile jarfile) {
        obrContent.append("      <p n='name' v='" + component.getName() + "' />\n");

        String interfaces = component.getInterfaces();
        if ((interfaces != null) && !interfaces.isEmpty())
            obrContent.append("      <p n='" + OBR.A_PROVIDE_INTERFACES + "' v='" + interfaces + "' /> \n");

        String messages = component.getMessages();
        if (messages != null)
            obrContent.append("      <p n='" + OBR.A_PROVIDE_MESSAGES + "' v='" + messages + "' />\n");

        String spec = component.getSpecification();
        if (spec != null)
            obrContent.append("      <p n='" + OBR.A_PROVIDE_SPECIFICATION + "' v='" + spec + "' />\n");

        // Checking consistency
        if (component.isImplementation()) {
            CheckObr.checkImplProvide(component.getName(), spec, interfaces, messages);
        }
    }

    private void printProperties(StringBuffer obrContent, ApamComponentInfo component) {
        // property attributes
        Map<String, String> properties = component.getProperties();
        for (String propertyName : properties.keySet()) {
            obrContent.append("      <p n='" + propertyName + "' v='"
                    + properties.get(propertyName) + "' />\n");
        }

        // definition attributes
        List<SimpleProperty> definitions = component.getDefinitions();
        for (SimpleProperty definition : definitions) {
            String tempContent = "      <p n='" + OBR.A_DEFINITION_PREFIX + definition.name + "'";
            if (definition.value != null) {
                tempContent = tempContent + (" v='" + (definition.value) + "'");
            }
            tempContent = tempContent + " />\n";
            obrContent.append(tempContent);
        }

        // Check Consistency
        CheckObr.checkImplAttributes(component);
    }

    private void printRequire(StringBuffer obrContent, ApamComponentInfo component) {
        if (component.isSpecification()) {
            for (SpecificationDependency dep : component.getSpecDependencies()) {
                // INTERFACE, PUSH_MESSAGE, PULL_MESSAGE, SPECIFICATION

                switch (dep.targetKind) {
                    case INTERFACE: {
                        obrContent.append("      <p n='" + OBR.A_REQUIRE_INTERFACE + "' v='" + dep.fieldType
                                + "' /> \n");
                        break;
                    }
                    case SPECIFICATION:
                        obrContent.append("      <p n='" + OBR.A_REQUIRE_SPECIFICATION + "' v='" + dep.fieldType
                                + "' /> \n");
                        break;
                    case PULL_MESSAGE:
                        obrContent.append("      <p n='" + OBR.A_REQUIRE_MESSAGE + "' v='" + dep.fieldType + "' /> \n");
                        break;
                    case PUSH_MESSAGE:
                        obrContent.append("      <p n='" + OBR.A_REQUIRE_MESSAGE + "' v='" + dep.fieldType + "' /> \n");
                        break;
                }
            }
            return;
        }

        // composite and implems
        CheckObr.checkRequire(component);

        if (component.isComposite())
            CheckObr.checkCompoMain(component);
    }

    private void printOBRElement(StringBuffer obrContent, ApamComponentInfo component, String indent, JarFile jarfile) {
        String spec = component.getSpecification();

        //headers
        if (component.isImplementation()) {
            obrContent.append("   <capability name='" + OBR.CAPABILITY_IMPLEMENTATION + "'>\n");
        }
        if (component.isComposite()) {
            obrContent.append("   <capability name='" + OBR.CAPABILITY_IMPLEMENTATION + "'>\n");
            obrContent.append("      <p n='" + CST.A_COMPOSITE + "' v='" + component.isComposite() + "' />\n");
            obrContent.append("      <p n='" + CST.A_MAIN_IMPLEMENTATION + "' v='"
                    + component.getApamMainImplementation()
                    + "' />\n");
        }
        if (component.isSpecification()) {
            obrContent.append("   <capability name='" + OBR.CAPABILITY_SPECIFICATION + "'>\n");
        }

        if (component.isInstance()) {
            CheckObr.checkInstance(component);
            return;
        }
        // provide clause
        printProvided(obrContent, component, jarfile);

        // definition attributes
        printProperties(obrContent, component);

        // Require, fields and constraints
        printRequire(obrContent, component);

        obrContent.append("   </capability>\n");

    }

}
