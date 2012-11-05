package fr.imag.adele.apam.apamMavenPlugIn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.ipojo.manipulation.ClassChecker;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.xml.parser.SchemaResolver;
import org.apache.felix.ipojo.xml.parser.XMLMetadataParser;
import org.apache.maven.plugin.MojoExecutionException;
import org.objectweb.asm.ClassReader;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class ApamRepoBuilder {

    /**
     * Metadata (in internal format).
     */
    private List<Element> m_metadata = new ArrayList<Element>();

    /**
     * Flag describing if we need or not use local XSD files (i.e. use the {@link SchemaResolver} or not). If
     * <code>true</code> the local XSD are not used.
     */
    private boolean       m_ignoreLocalXSD;
    private File          classDirectory;

    public boolean
            writeOBRFile(String obrFileStr, File metadataFile, InputStream is, File jarFile, File outputDirectory) {
        classDirectory = outputDirectory;
        File obrFile = new File(obrFileStr);
        boolean okMetadata = false;
        StringBuffer obrContent = new StringBuffer("<obr> \n");
        JarFile jar = null;
        try {
            jar = new JarFile(jarFile);
            okMetadata = writeMetadataOBRFile(obrContent, metadataFile, is, jar);
            jar.close();
        } catch (IOException e2) {
            // System.err.println("cannot read the jar file : " + jarFile.getAbsolutePath());
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

    public boolean writeMetadataOBRFile(StringBuffer obrContent, File metadata, InputStream is, JarFile jarFile) {
        if (metadata == null) {
            // System.out.println(" no metadata");
            return false;
        }
        List<ApamComponentInfo> components = null;
        if (is == null)
            components = getMetadataInfo(metadata);
        else
            components = getMetadataInfo(is);

        for (ApamComponentInfo comp : components) {
            // printElement(comp.m_componentMetadata, "");
            printOBRElement(obrContent, comp, "", jarFile);
        }
        return true;
    }

    private void printOBRElement(StringBuffer obrContent, ApamComponentInfo component, String indent, JarFile jarfile) {

        // apam attributes
        obrContent.append("   <capability name='apam-component'>\n");

        // The ipojo name
        obrContent.append("      <p n='name' v='" + component.getName() + "' />\n");

        // The name of the APAM provided specification. Optional: if not defined, the interfaces will be used
        if (component.getApamSpecification() != null)
            obrContent.append("      <p n='apam-specification' v='" + component.getApamSpecification()
                    + "' />\n");

        // Information for composites
        if (component.isComposite()) {
            obrContent.append("      <p n='apam-composite' v='" + component.isComposite() + "' />\n");
            obrContent.append("      <p n='apam-main-implementation' v='" + component.getApamMainImplementation()
                    + "' />\n");
        }

        // property attributes
        Map<String, String> properties = component.getProperties();
        for (String propertyName : properties.keySet()) {
            obrContent.append("      <p n='" + propertyName + "' v='"
                    + properties.get(propertyName) + "' />\n");
        }

        // interfaces
        List<String> interfaces = component.getInterfaces(jarfile);
        if (!interfaces.isEmpty()) {
            obrContent.append("      <p n='interfaces' v='");
            for (int j = 0; j < interfaces.size(); j++) {
                if (!interfaces.get(j).startsWith("java.lang.")) {
                    obrContent.append(";");
                    obrContent.append(interfaces.get(j));
                }
            }
            obrContent.append(";' />\n");
        }

        obrContent.append("   </capability>\n");
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

    public List<ApamComponentInfo> getMetadataInfo(InputStream metadata) {
        if (metadata != null) {
            parseXMLMetadata(metadata);
        }
        // Get the list of declared component
        return computeDeclaredComponents();
    }

    public List<ApamComponentInfo> getMetadataInfo(File metadataFile) {
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

        return isApam && (isImplementation || isComposite);
    }

    /**
     * Component Info. Represent a component type to be manipulated.
     * 
     * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
     */
    private final class ApamComponentInfo {

        final static String   APAM_NAMESPACE               = "fr.imag.adele.apam";

        final static String   APAM_SPECIFICATION_PROPERTY  = "specification";

        final static String   APAM_INTERFACES_PROPERTY     = "interfaces";

        final static String   APAM_IMPLEMENTATION_PROPERTY = "mainImplem";

        /**
         * Component Type metadata.
         */
        private final Element m_componentMetadata;

        /**
         * Constructor.
         * 
         * @param met : component type metadata
         */
        public ApamComponentInfo(Element met) {
            m_componentMetadata = met;
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
         * Get the apam provided specification name. For composites it correspond to the specification
         * provided by the main implementation.
         */

        public String getApamSpecification() {
            return m_componentMetadata.getAttribute(ApamComponentInfo.APAM_SPECIFICATION_PROPERTY);
        }

        /**
         * Get the apam implementation of the main specification for a composite.
         */
        public String getApamMainImplementation() {
            if (isComposite())
                return m_componentMetadata.getAttribute(ApamComponentInfo.APAM_IMPLEMENTATION_PROPERTY);

            return null;
        }

        /**
         * Get the list of provided interfaces of this component
         */
        @SuppressWarnings("unchecked")
        public List<String> getInterfaces(JarFile jarfile) {

            List<String> interfaces = new ArrayList<String>();

            /*
             * For primitive components if not specification is explicitly specified,
             * get all the implemented interfaces from the implementation class
             */
            if (!isComposite()) {
                try {

                    String className = m_componentMetadata.getAttribute("classname");
                    className = className.replace('.', '/');
                    className += ".class";

                    InputStream byteCodeStream = getInputStream(className, jarfile);

                    if (byteCodeStream != null) {
                        ClassReader ckReader = new ClassReader(byteCodeStream);
                        ClassChecker ck = new ClassChecker();
                        ckReader.accept(ck, ClassReader.SKIP_FRAMES);
                        byteCodeStream.close();

                        interfaces.addAll(ck.getInterfaces());
                    }
                } catch (IOException e) {
                }

            }

            /*
             * For composite components get the explicitly specified interfaces
             */
            if (isComposite()) {

                String encodedInterfaces = m_componentMetadata.getAttribute(ApamComponentInfo.APAM_INTERFACES_PROPERTY);
                if (encodedInterfaces != null) {
                    for (String interfaceName : ApamRepoBuilder.parseArrays(encodedInterfaces)) {
                        interfaces.add(interfaceName);
                    }
                }
            }

            return interfaces;
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
                    if (property.containsAttribute("value")) {
                        properties.put(property.getAttribute("name"), property.getAttribute("value"));
                    }
                }
            }

            return properties;

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
        return elements != null ? elements : ApamRepoBuilder.EMPTY_ELEMENT_LIST;
    }

    /**
     * Utility method to facilitate iteration over possibly null attribute lists
     */
    private final Attribute[] optional(Attribute[] attributes) {
        return attributes != null ? attributes : ApamRepoBuilder.EMPTY_ATTRIBUTE_LIST;
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

    public Manifest getManifestFromFile(File file) throws MojoExecutionException {
        Manifest manifest = null;
        JarFile inJarFile = null;
        try {
            inJarFile = new JarFile(file);
            manifest = inJarFile.getManifest();
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to access jar:" + file.getAbsolutePath());
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
        if (str.length() == 0) {
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
            return ApamRepoBuilder.split(internal, ",");
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
