package fr.imag.adele.apam.apamAPI;

import java.net.URL;
import java.util.Set;

import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Implementation;

public interface CompExType extends ASMImpl, Composite {

    /**
     * Creates a composite type and instance from a composite Object (found by a manager, or in a repository (OBR or
     * other)) and starts the application.
     * 
     * @param Father
     * @param name
     * @return
     */
    //    public ASMInst createCompExType(Composite father, String name);

    /**
     * Creates a composite type and returns an instance from a composite Object deployed in SAM.
     * The father composite is this
     * 
     * @param samImpl the sam object representing the composite object
     * @return the CompExInst.
     */

    //public static CompExType createCompExType(Composite implComposite, Implementation samImpl);

    //    /**
    //     * Creates a composite type and instance from an existing implementation (found by a manager, in SAM or in a
    //     * repository (OBR or
    //     * other)) and starts the application.
    //     * 
    //     * @param compositeName the name of the application : the name of the root composite
    //     * @param models optional : the list of models for that root composite.
    //     * @param samImplName the implementation name as known by Sam.
    //     * @param implName The logical name for the Application Main implementation
    //     * @param specName optional : the logical name of the associated specification
    //     * @param properties The initial properties for the main Implementation.
    //     * @return The new created application.
    //     */
    //    public Application createCompExType(String implName, Attributes properties);

    //
    //    /**
    //     * Creates an application from scratch, by deploying a specification, and its main implementation. First creates the
    //     * root composites (compositeName), associates its models (models).
    //     * Then installs and creates the specification (from the url or
    //     * only by its interfaces) then installs and creates the main implementation (implName) from its URL, considered as
    //     * the application Main. At least the following parameters MUST be non null: appliName, implName, implUrl, specName,
    //     * interfaces
    //     * 
    //     * @param compositeName The name of the the application and of its root composite.
    //     * @param models.Optional. The manager models
    //     * @param implName.Optional. The logical name for the Application Main implementation
    //     * @param implUrl Location of the Main executable.
    //     * @param implType Type of packaging for main executable.
    //     * @param specName. optional : the logical name of the associated specification
    //     * @param specUrl Location of the code (interfaces) associated with the main specification.
    //     * @param specType Type of packaging for the code (interfaces) associated with the main specification.
    //     * @param interfaces. List of the interfaces implemented by this specification.
    //     * @param properties. Optional. The initial properties for the Implementation.
    //     * @return The new created application.
    //     */
    //    public CompExType createCompExType(URL specUrl, String[] interfaces, Attributes properties);

    //
    //    /**
    //     * Creates an application from scratch, by deploying an implementation. First creates the root composites
    //     * (compositeName), associates its models (models). Then install an implementation (implName) from its URL,
    //     * considered as the application Main, and instantiates the implementation.
    //     * 
    //     * @param compositeName. The name of the application and of its root composite.
    //     * @param models. Optional. The manager models
    //     * @param implName. Optional. The logical name for the Application Main implementation
    //     * @param url Location of the Main executable.
    //     * @param type Type of packaging (bundle, ....)
    //     * @param specName optional : the logical name of the associated specification
    //     * @param properties. Optional. The initial properties for the Implementation.
    //     * @return The new created application.
    //     */
    //    public CompExType createCompExType(URL implUrl, Attributes properties);

    public ASMImpl getMainImpl();

    public String getMainImplName();

	public ASMSpec getMainSpec();
}
