package fr.imag.adele.apam.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulation.ClassChecker;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.xml.parser.SchemaResolver;
import org.apache.felix.ipojo.xml.parser.XMLMetadataParser;
//import org.apache.maven.plugin.MojoExecutionException;
import org.objectweb.asm.ClassReader;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

//import fr.imag.adele.apam.apam2MavenPlugIn.Apam2RepoBuilder;
//import fr.imag.adele.apam.apam2MavenPlugIn.Apam2RepoBuilder.ApamComponentInfo;
//import fr.imag.adele.apam.util.ComponentXML;
//import fr.imag.adele.apam.apam2MavenPlugIn.Apam2RepoBuilder.SimpleProperty;
import fr.imag.adele.apam.util.Dependency.AtomicDependency;
import fr.imag.adele.apam.util.Dependency.ImplementationDependency;
import fr.imag.adele.apam.util.Dependency.SpecificationDependency;
import fr.imag.adele.apam.util.Dependency.TargetKind;

public class ApamComponentXML {

    /**
     * Metadata (in internal format).
     */
    private List<Element>     m_metadata = new ArrayList<Element>();

    /**
     * Flag describing if we need or not use local XSD files (i.e. use the {@link SchemaResolver} or not). If
     * <code>true</code> the local XSD are not used.
     */
    private boolean           m_ignoreLocalXSD;
    private final File        classDirectory;
    private final JarFile     jarFile;
    private final File        metadata;
    private final InputStream is;

    public ApamComponentXML(File classDirectory, File metadata, InputStream is, JarFile jarFile) {
        this.classDirectory = classDirectory;
        this.jarFile = jarFile;
        this.metadata = metadata;
        this.is = is;
    }

    /**
     * Gets a byte array that contains the bytecode of the given classname. This method can be overridden by
     * sub-classes.
     * 
     * @param classname name of a class to be read
     * @return a byte array
     * @throws IOException if the classname cannot be read
     */
    protected byte[] getBytecode(final String classname, JarFile jarFile) throws IOException {

        InputStream currIn = null;
        byte[] in = new byte[0];
        try {
            // Get the stream to read
            currIn = getInputStream(classname, jarFile);
            int c;

            // Fill the byte array with IS content
            while ((c = currIn.read()) >= 0) {
                byte[] in2 = new byte[in.length + 1];
                System.arraycopy(in, 0, in2, 0, in.length);
                in2[in.length] = (byte) c;
                in = in2;
            }
        } finally {
            // Close the stream
            if (currIn != null) {
                try {
                    currIn.close();
                } catch (IOException e) {
                    // Ignored
                }
            }
        }

        return in;
    }

    /**
     * Gets an input stream on the given class. This methods manages Jar files and directories. If also looks into
     * WEB-INF/classes to support WAR files. This method may be overridden.
     * 
     * @param classname the class name
     * @return the input stream
     * @throws IOException if the file cannot be read
     */
    protected InputStream getInputStream(String classname, JarFile inputJar) throws IOException {
        if (inputJar != null) {
            // Fix entry name if needed
            if (!classname.endsWith(".class")) {
                classname += ".class";
            }
            JarEntry je = inputJar.getJarEntry(classname);
            if (je == null) {
                // Try in WEB-INF/classes (WAR files)
                je = inputJar.getJarEntry("WEB-INF/classes/" + classname);
                if (je == null) {
                    // If still null, throw an exception.
                    throw new IOException("The class " + classname + " connot be found in the input Jar file");
                }
            }
            return inputJar.getInputStream(je);
        } else {
            // Directory
            if (classDirectory.exists() && classDirectory.isDirectory()) {
                File file = new File(classDirectory, classname);
                return new FileInputStream(file);
                // System.out.println("should use the class directory");
            }
        }
        return null;
    }

    private void printElement(Element elem, String indent) {
        System.out.println(indent + "element : " + elem.getName());
        indent = indent + "  ";
        Attribute[] attrs = elem.getAttributes();
        for (Attribute attr : attrs) {
            System.out.println(indent + attr.getName() + " = " + attr.getValue());
        }

        Element[] elems = elem.getElements();
        for (Element el : elems) {
            printElement(el, indent + "  ");
        }
    }

    public List<ApamComponentInfo> getComponents() {
        if (is == null)
            return getMetadataInfo(metadata);
        else
            return getMetadataInfo(is);
//        ApamComponentXML.ck = newComponent(jarFile);
//        interfaces = _getInterfaceList();
//        return ApamComponentXML.components;
    }

    private List<ApamComponentInfo> getMetadataInfo(InputStream metadata) {
        if (metadata != null) {
            parseXMLMetadata(metadata);
        }
        // Get the list of declared component
        return computeDeclaredComponents();
    }

    private List<ApamComponentInfo> getMetadataInfo(File metadataFile) {
        if (metadataFile != null) {
            parseXMLMetadata(metadataFile);
        }
        // Get the list of declared component
        return computeDeclaredComponents();
    }

    /**
     * Return the list of "concrete" component.
     */
    private List<ApamComponentInfo> computeDeclaredComponents() {
        List<ApamComponentInfo> apamComponents = new ArrayList<ApamComponentInfo>();
        for (int i = 0; i < m_metadata.size(); i++) {
            Element meta = m_metadata.get(i);
            if (isApamComponent(meta)) {
                apamComponents.add(new ApamComponentInfo(meta));
            }
        }
        return apamComponents;
    }

    /**
     * Whether an ipojo metada corresponds to an APAM component
     * 
     * @param meta
     * @return
     */
    private boolean isApamComponent(Element meta) {

        boolean isApam = (meta.getNameSpace() != null) && meta.getNameSpace().equals(ApamComponentInfo.APAM_NAMESPACE);

        boolean isImplementation = meta.getName().equalsIgnoreCase("implementation")
                && (meta.getAttribute("classname") != null);
        boolean isComposite = meta.getName().equalsIgnoreCase("composite");
        boolean isSpecification = meta.getName().equalsIgnoreCase("specification");
        return isApam && (isImplementation || isComposite || isSpecification);
    }

    public static class SimpleProperty {
        public String name;
        public String type;
        public String value;

        public SimpleProperty(String name, String type, String value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }
    }

    /**
     * Component Info. Represent a component type to be manipulated.
     * 
     * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
     */
    public final class ApamComponentInfo {

        final static String        APAM_NAMESPACE               = "fr.imag.adele.apam";
        final static String        APAM_SPECIFICATION_PROPERTY  = "specification";
        final static String        APAM_INTERFACES_PROPERTY     = "interfaces";
        final static String        APAM_MESSAGES_PROPERTY       = "messages";
        final static String        APAM_IMPLEMENTATION_PROPERTY = "mainImplem";

        private final List<String> interfaces;
        private final ClassChecker ck;
        /**
         * Component Type metadata.
         */
        private final Element      m_componentMetadata;

        /**
         * Constructor.
         * 
         * @param met : component type metadata
         */
        public ApamComponentInfo(Element met) {
            m_componentMetadata = met;
            ck = newComponent();
            interfaces = _getInterfaceList();
        }

        public ClassChecker newComponent() {
            if (!isImplementation())
                return null;
            ClassChecker ck = null;
            String className = null;
            try {
                className = m_componentMetadata.getAttribute("classname");
                if (className == null) {
                    System.err.println("Invalid implementation component, className missing");
                    return null;
                }
                className = className.replace('.', '/');
                className += ".class";

                InputStream byteCodeStream = getInputStream(className, jarFile);

                if (byteCodeStream != null) {
                    ClassReader ckReader = new ClassReader(byteCodeStream);
                    ck = new ClassChecker();
                    ckReader.accept(ck, ClassReader.SKIP_FRAMES);
                    byteCodeStream.close();
                }
            } catch (IOException e) {
                System.err.println("Could not parse " + className);
            }
            return ck;
        }

        /**
         * The name of the component
         */
        public String getName() {
            return m_componentMetadata.getAttribute("name");
        }

        /**
         * Whether this is a composite definition
         * 
         * @return
         */
        public boolean isComposite() {
            return m_componentMetadata.getName().equalsIgnoreCase("composite");
        }

        /**
         * Whether this is a specification definition
         * 
         * @return
         */
        public boolean isSpecification() {
            return m_componentMetadata.getName().equalsIgnoreCase("specification");
        }

        /**
         * Whether this is a specification definition
         * 
         * @return
         */
        public boolean isImplementation() {
            return m_componentMetadata.getName().equalsIgnoreCase("implementation");
        }

        /**
         * Get the apam implementation of the main specification for a composite.
         */
        public String getApamMainImplementation() {
            if (isComposite())
                return m_componentMetadata.getAttribute(ApamComponentInfo.APAM_IMPLEMENTATION_PROPERTY);

            return null;
        }

        public String getMessages() {
            return m_componentMetadata.getAttribute(ApamComponentInfo.APAM_MESSAGES_PROPERTY);
        }

        public List<String> getMessageList() {
            List<String> messages = new ArrayList<String>();
            String message = m_componentMetadata.getAttribute(ApamComponentInfo.APAM_MESSAGES_PROPERTY);
            if (message != null) {
                for (String messageName : ApamComponentXML.parseArrays(message)) {
                    messages.add(messageName);
                }
            }
            return messages;
        }

        public String getSpecification() {
            return m_componentMetadata.getAttribute(ApamComponentInfo.APAM_SPECIFICATION_PROPERTY);
        }

        /**
         * Get the list of provided interfaces of this component
         */

        public String getInterfaces() {
            List<String> interfaces = getInterfaceList();
            String interfs = "{";
            for (String inter : interfaces) {
                interfs += inter + ", ";
            }
            return (interfs.substring(0, interfs.length() - 2) + "}");
        }

        public List<String> getInterfaceList() {
            return interfaces;
        }

        @SuppressWarnings("unchecked")
        private List<String> _getInterfaceList() {

            /*
             * For primitive components if not specification is explicitly specified,
             * get all the implemented interfaces from the implementation class
             */
            if (isImplementation()) {
                return ck.getInterfaces();
            }

            /*
             * For composite components get the explicitly specified interfaces
             */
            List<String> interfaces = new ArrayList<String>();
            String encodedInterfaces = m_componentMetadata.getAttribute(ApamComponentInfo.APAM_INTERFACES_PROPERTY);
            if (encodedInterfaces != null) {
                for (String interfaceName : ApamComponentXML.parseArrays(encodedInterfaces)) {
                    interfaces.add(interfaceName);
                }
            }

            return interfaces;
        }

        public Set<ImplementationDependency> getImplemDependencies() {
            Set<ImplementationDependency> implDeps = new HashSet<ImplementationDependency>();
            // Complex dependencies
            for (Element deps : optional(m_componentMetadata.getElements("dependencies",
                    ApamComponentInfo.APAM_NAMESPACE))) {

                for (Element property : optional(deps.getElements("specification",
                        ApamComponentInfo.APAM_NAMESPACE))) {
                    Attribute attr = property.getAttributes()[0];
                    String name = attr.getValue();
                    Set<AtomicDependency> aDeps = new HashSet<AtomicDependency>();
                    ImplementationDependency dependency = new ImplementationDependency(name, aDeps, false);
                    getFilters(property, dependency);
                    implDeps.add(dependency);
                    for (Element aDep : optional(property.getElements("interface",
                            ApamComponentInfo.APAM_NAMESPACE))) {
                        String field = aDep.getAttribute("field");
                        String type = getFieldType(aDep.getAttribute("type"), field);
                        aDeps.add(new AtomicDependency(TargetKind.INTERFACE, field, type));
                    }
                    for (Element aDep : optional(property.getElements("message",
                            ApamComponentInfo.APAM_NAMESPACE))) {
                        String field = aDep.getAttribute("name");
                        String type = getFieldType(aDep.getAttribute("type"), field);
                        if (field != null) {
                            aDeps.add(new AtomicDependency(TargetKind.PULL_MESSAGE, field, type));
                        } else {
                            field = aDep.getAttribute("method");
                            aDeps.add(new AtomicDependency(TargetKind.PUSH_MESSAGE, field, type));
                        }
                    }
                }

                // Atomic dependencies
                for (Element property : optional(deps.getElements("interface",
                         ApamComponentInfo.APAM_NAMESPACE))) {
                    Set<AtomicDependency> aDeps = new HashSet<AtomicDependency>();
                    ImplementationDependency dependency = new ImplementationDependency(null, aDeps, false);
                    getFilters(property, dependency);
                    implDeps.add(dependency);
                    String field = property.getAttribute("field");
                    String type = getFieldType(property.getAttribute("type"), field);
                    aDeps.add(new AtomicDependency(TargetKind.INTERFACE, field, type));
                }

                for (Element property : optional(deps.getElements("message",
                             ApamComponentInfo.APAM_NAMESPACE))) {
                    Set<AtomicDependency> aDeps = new HashSet<AtomicDependency>();
                    ImplementationDependency dependency = new ImplementationDependency(null, aDeps, false);
                    getFilters(property, dependency);
                    implDeps.add(dependency);
                    String field = property.getAttribute("name");
                    String type; // = deps.getAttribute("type");
                    if (field != null) {
                        type = getFieldType(property.getAttribute("type"), field);
                        aDeps.add(new AtomicDependency(TargetKind.PULL_MESSAGE, field, type));
                    } else {
                        field = property.getAttribute("method");
                        type = property.getAttribute("type");
                        aDeps.add(new AtomicDependency(TargetKind.PUSH_MESSAGE, field, type));
                    }
                }

            }

            // System.out.println(implDeps);
            return implDeps;
        }

        public Set<SpecificationDependency> getSpecDependencies() {
            Set<SpecificationDependency> specDeps = new HashSet<SpecificationDependency>();
            for (Element deps : optional(m_componentMetadata.getElements("dependencies",
                    ApamComponentInfo.APAM_NAMESPACE))) {

                for (Element property : optional(deps.getElements("specification",
                        ApamComponentInfo.APAM_NAMESPACE))) {
                    String name = property.getAttribute("name");
                    SpecificationDependency dependency = new SpecificationDependency(TargetKind.SPECIFICATION, name);
                    getFilters(property, dependency);
                    specDeps.add(dependency);
                }

                for (Element property : optional(deps.getElements("interface",
                        ApamComponentInfo.APAM_NAMESPACE))) {
                    String name = property.getAttribute("name");
                    SpecificationDependency dependency = new SpecificationDependency(TargetKind.INTERFACE, name);
                    getFilters(property, dependency);
                    specDeps.add(dependency);
                }

                for (Element property : optional(deps.getElements("message",
                        ApamComponentInfo.APAM_NAMESPACE))) {
                    String name = property.getAttribute("name");
                    SpecificationDependency dependency = new SpecificationDependency(TargetKind.PUSH_MESSAGE, name);
                    getFilters(property, dependency);
                    specDeps.add(dependency);
                }
            }
            // System.out.println(specDeps);
            return specDeps;
        }

        private void getFilters(Element dependencyMetadata, Dependency dependency) {
            /*
             * Get filters on constraints in global variables
             */
            Element constraintsDeclarations[] = dependencyMetadata.getElements(
                    "constraints", ApamComponentInfo.APAM_NAMESPACE);
            dependency.implementationConstraints = new ArrayList<String>();
            dependency.instanceConstraints = new ArrayList<String>();

            for (Element constraintsDeclaration : optional(constraintsDeclarations)) {
                Element implementationConstraints[] = constraintsDeclaration.getElements(
                        "implementation", ApamComponentInfo.APAM_NAMESPACE);
                for (Element implementationConstraint : optional(implementationConstraints)) {
                    dependency.implementationConstraints.add(implementationConstraint.getAttribute("filter"));
                }
                Element instanceConstraints[] = constraintsDeclaration.getElements(
                        "instance", ApamComponentInfo.APAM_NAMESPACE);
                for (Element instanceConstraint : optional(instanceConstraints)) {
                    dependency.instanceConstraints.add(instanceConstraint.getAttribute("filter"));
                }
            }

            /*
             * Get filters on preferences
             */
            dependency.implementationPreferences = new ArrayList<String>();
            dependency.instancePreferences = new ArrayList<String>();
            Element preferencesDeclarations[] = dependencyMetadata.getElements(
                    "preferences", ApamComponentInfo.APAM_NAMESPACE);

            for (Element preferencesDeclaration : optional(preferencesDeclarations)) {

                Element implementationPreferences[] = preferencesDeclaration.getElements(
                        "implementation", ApamComponentInfo.APAM_NAMESPACE);
                for (Element implementationPreference : optional(implementationPreferences)) {
                    dependency.implementationPreferences.add(implementationPreference.getAttribute("filter"));
                }

                Element instancePreferences[] = preferencesDeclaration.getElements(
                        "instance", ApamComponentInfo.APAM_NAMESPACE);
                for (Element instancePreference : optional(instancePreferences)) {
                    dependency.instancePreferences.add(instancePreference.getAttribute("filter"));
                }
            }
        }

        /**
         * Get the list of properties defined for this component
         */
        public Map<String, String> getProperties() {
            Map<String, String> properties = new HashMap<String, String>();
            for (Element propertiesDeclaration : optional(m_componentMetadata.getElements("properties",
                    ApamComponentInfo.APAM_NAMESPACE))) {
                for (Attribute attribute : optional(propertiesDeclaration.getAttributes())) {
                    properties.put(attribute.getName(), attribute.getValue());
                }

                for (Element property : optional(propertiesDeclaration.getElements("property",
                        ApamComponentInfo.APAM_NAMESPACE))) {
                    for (Attribute attr : property.getAttributes()) {
                        if (!attr.getName().equalsIgnoreCase("type") && !attr.getName().equalsIgnoreCase("field")) {
                            properties.put(attr.getName(), attr.getValue());
                        }
                    }
                }
            }
            return properties;
        }

        private String getFieldType(String fieldType, String fieldName) {
            String compFieldType = getFieldType(fieldName);
            if ((fieldType != null) && !fieldType.equals(compFieldType)) {
                System.err.println("ERROR: type of field " + fieldName + " should be " + compFieldType
                         + " instead of " + fieldType);
            }
            return compFieldType;
        }

        public String getFieldType(String fieldName) {
            Map fields = ck.getFields();
            for (Object field : fields.keySet()) {
                if (((String) field).equals(fieldName)) {
                    // System.out.println("field " + fieldName + " is of type " + fields.get(field));
                    return (String) fields.get(field);
                }
            }
            System.err.println("ERROR: Field " + fieldName + " declared but not existing in the code");
            return null;
        }

        /**
         * Get the list of properties defined for this component
         */
        public List<SimpleProperty> getDefinitions() {
            List<SimpleProperty> definitions = new ArrayList<SimpleProperty>();
            for (Element propertiesDeclaration : optional(m_componentMetadata.getElements("definitions",
                    ApamComponentInfo.APAM_NAMESPACE))) {
                for (Attribute attribute : optional(propertiesDeclaration.getAttributes())) {
                    definitions.add(new SimpleProperty(attribute.getName(), null, attribute.getValue()));
                }

                for (Element property : optional(propertiesDeclaration.getElements("definition",
                        ApamComponentInfo.APAM_NAMESPACE))) {
                    if (property.containsAttribute("name") && property.containsAttribute("type")) {
                        definitions.add(new SimpleProperty(property.getAttribute("name"),
                                property.getAttribute("type"),
                                property.getAttribute("value")));
                    }
                }
            }
            return definitions;
        }
    }

    /**
     * Constant to represent undefined elements
     */
    final static Element[]   EMPTY_ELEMENT_LIST   = new Element[0];

    /**
     * Constant to represent undefined attributes
     */
    final static Attribute[] EMPTY_ATTRIBUTE_LIST = new Attribute[0];

    /**
     * Utility method to facilitate iteration over possibly null element lists
     */
    private final Element[] optional(Element[] elements) {
        return elements != null ? elements : ApamComponentXML.EMPTY_ELEMENT_LIST;
    }

    /**
     * Utility method to facilitate iteration over possibly null attribute lists
     */
    private final Attribute[] optional(Attribute[] attributes) {
        return attributes != null ? attributes : ApamComponentXML.EMPTY_ATTRIBUTE_LIST;
    }

    /**
     * Parse the XML metadata from the given file.
     * 
     * @param metadataFile the metadata file
     */
    private void parseXMLMetadata(File metadataFile) {
        if (metadataFile.isDirectory()) {
            // Traverse the directory and parse all files.
            File[] files = metadataFile.listFiles();
            for (File file : files) {
                parseXMLMetadata(file);
            }
        } else if (metadataFile.getName().endsWith(".xml")) { // Detect XML by extension,
            // others are ignored.
            try {
                InputStream stream = null;
                URL url = metadataFile.toURI().toURL();
                if (url == null) {
                    // warn("Cannot find the metadata file : " + metadataFile.getAbsolutePath());
                    m_metadata = new ArrayList<Element>();
                } else {
                    stream = url.openStream();
                    parseXMLMetadata(stream); // m_metadata is set by the method.
                }
            } catch (MalformedURLException e) {
                // error("Cannot open the metadata input stream from " + metadataFile.getAbsolutePath() + ": "
                // + e.getMessage());
                m_metadata = null;
            } catch (IOException e) {
                // error("Cannot open the metadata input stream: " + metadataFile.getAbsolutePath() + ": "
                // + e.getMessage());
                m_metadata = null;
            }

            // m_metadata can be either an empty array or an Element
            // array with component type description. It also can be null
            // if no metadata file is given.
        }
    }

    /**
     * Parses XML Metadata.
     * 
     * @param stream metadata input stream.
     */
    private void parseXMLMetadata(InputStream stream) {
        Element[] meta = null;
        try {
            XMLReader parser = XMLReaderFactory.createXMLReader();
            XMLMetadataParser handler = new XMLMetadataParser();
            parser.setContentHandler(handler);
            parser.setFeature("http://xml.org/sax/features/validation",
                    true);
            parser.setFeature("http://apache.org/xml/features/validation/schema",
                    true);

            parser.setErrorHandler(handler);

            if (!m_ignoreLocalXSD) {
                parser.setEntityResolver(new SchemaResolver());
            }

            InputSource is = new InputSource(stream);
            parser.parse(is);
            meta = handler.getMetadata();
            stream.close();

        } catch (Exception e) {
        }

        if ((meta == null) || (meta.length == 0)) {
            // warn("Neither component types, nor instances in the metadata");
        }

        m_metadata.addAll(Arrays.asList(meta));
    }

    public Manifest getManifestFromFile(File file) throws IOException {
        Manifest manifest = null;
        JarFile inJarFile = null;
        try {
            inJarFile = new JarFile(file);
            manifest = inJarFile.getManifest();
        } catch (IOException e) {
            throw new IOException("Unable to access jar:" + file.getAbsolutePath());
        }
        try {
            inJarFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return manifest;
    }

    /**
     * Parses the iPOJO string form of an array as {a, b, c}
     * or [a, b, c].
     * 
     * @param str the string form
     * @return the resulting string array
     */
    public static String[] parseArrays(String str) {
        if ((str == null) || (str.length() == 0)) {
            return new String[0];
        }

        // Remove { and } or [ and ]
        if (((str.charAt(0) == '{') && (str.charAt(str.length() - 1) == '}'))
                || ((str.charAt(0) == '[') && (str.charAt(str.length() - 1) == ']'))) {
            String internal = (str.substring(1, str.length() - 1)).trim();
            // Check empty array
            if (internal.length() == 0) {
                return new String[0];
            }
            return ApamComponentXML.split(internal, ",");
        } else {
            return new String[] { str };
        }
    }

    /**
     * Split method.
     * This method is equivalent of the String.split in java 1.4
     * The result array contains 'trimmed' String
     * 
     * @param toSplit the String to split
     * @param separator the separator
     * @return the split array
     */
    public static String[] split(String toSplit, String separator) {
        StringTokenizer tokenizer = new StringTokenizer(toSplit, separator);
        String[] result = new String[tokenizer.countTokens()];
        int index = 0;
        while (tokenizer.hasMoreElements()) {
            result[index] = tokenizer.nextToken().trim();
            index++;
        }
        return result;
    }

}
