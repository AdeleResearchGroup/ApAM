package fr.imag.adele.apam.apamMavenPlugIn;

import java.io.ByteArrayInputStream;
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
import java.util.List;
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
        List<ComponentInfo> components = null;
        if (is == null)
            components = getMetadataInfo(metadata);
        else
            components = getMetadataInfo(is);

        for (ComponentInfo comp : components) {
            // printElement(comp.m_componentMetadata, "");
            printOBRElement(obrContent, comp, "", jarFile);
        }
        return true;
    }

    private void printOBRElement(StringBuffer obrContent, ComponentInfo component, String indent, JarFile jarfile) {
        Element elem = component.m_componentMetadata;
        if (((elem != null) && (elem.getName().equals("component")) && ((elem.getAttribute("apam-specification") != null) || (elem
                .getAttribute("apam-implementation") != null)))) {
            obrContent.append("   <capability name='apam-component'>\n");
            obrContent.append("      <p n='name' v='" + elem.getAttribute("name") + "' />\n");
            if (elem.getAttribute("apam-specification") != null)
                obrContent.append("      <p n='apam-specification' v='" + elem.getAttribute("apam-specification")
                        + "' />\n");
            if (elem.getAttribute("apam-implementation") != null)
                obrContent.append("      <p n='apam-implementation' v='" + elem.getAttribute("apam-implementation")
                        + "' />\n");
            obrContent.append("   </capability>\n");
        }

        if (jarfile == null)
            return;
        List<String> interfaces;
        try {
            byte[] classByte = getBytecode(component.m_classname, jarfile);
            interfaces = getInterfaces(classByte);
        } catch (IOException e) {
            // System.err.println("Cannot extract bytecode for component '" + component.m_classname + "'");
            return;
        }

        if (interfaces != null) {
            for (int j = 0; j < interfaces.size(); j++) {
                obrContent.append("   <capability name='apam-interface'>\n");
                obrContent.append("      <p n='name' v='" + interfaces.get(j).toString() + "' />\n");
                obrContent.append("   </capability>\n");
            }
        }
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
        ;
        Element[] elems = elem.getElements();
        for (Element el : elems) {
            printElement(el, indent + "  ");
        }
    }

    public List<String> getInterfaces(byte[] origin) throws IOException {
        InputStream is1 = new ByteArrayInputStream(origin);

        // First check if the class is already manipulated :
        ClassReader ckReader = new ClassReader(is1);
        ClassChecker ck = new ClassChecker();
        ckReader.accept(ck, ClassReader.SKIP_FRAMES);
        is1.close();

        // Get interfaces and super class.
        return ck.getInterfaces();
    }

    public List<ComponentInfo> getMetadataInfo(InputStream metadata) {
        if (metadata != null) {
            parseXMLMetadata(metadata);
        }
        // Get the list of declared component
        return computeDeclaredComponents();
    }

    public List<ComponentInfo> getMetadataInfo(File metadataFile) {
        if (metadataFile != null) {
            parseXMLMetadata(metadataFile);
        }
        // Get the list of declared component
        return computeDeclaredComponents();
    }

    /**
     * Return the list of "concrete" component.
     */
    private List<ComponentInfo> computeDeclaredComponents() {
        List<ComponentInfo> componentClazzes = new ArrayList<ComponentInfo>();
        for (int i = 0; i < m_metadata.size(); i++) {
            Element meta = m_metadata.get(i);
            String name = meta.getAttribute("classname");
            if (name != null) { // Only handler and component have a classname attribute
                name = name.replace('.', '/');
                name += ".class";
                componentClazzes.add(new ComponentInfo(name, meta));
            }
        }
        return componentClazzes;
    }

    // === inner class from ipojo
    /**
     * Component Info. Represent a component type to be manipulated or already manipulated.
     * 
     * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
     */
    private class ComponentInfo {
        /**
         * Component Type metadata.
         */
        Element m_componentMetadata;

        /**
         * Component Type implementation class.
         */
        String  m_classname;

        /**
         * Is the class already manipulated.
         */
        boolean m_isManipulated;

        /**
         * Constructor.
         * 
         * @param cn : class name
         * @param met : component type metadata
         */
        ComponentInfo(String cn, Element met) {
            m_classname = cn;
            m_componentMetadata = met;
            m_isManipulated = false;
        }
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

}
