package fr.imag.adele.apam.apamAPI;

import java.net.URL;
import java.util.Set;

import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.util.Attributes;

public interface Apam {

    /**
     * creates an application from an existing implementation found in SAM. Does not start the application. Call execute
     * for that.
     * 
     * @param compositeName the name of the application : the name of the root composite
     * @param models optional : the list of models for that root composite.
     * @param samImplName the implementation name as known by Sam.
     * @param implName The logical name for the Application Main implementation
     * @param specName optional : the logical name of the associated specification
     * @param properties The initial properties for the main Implementation.
     * @return The new created application.
     */
    public Application createAppli(String appliName, Set<ManagerModel> models, String samImplName, String implName,
            String specName, Attributes properties);

    /**
     * Creates a composite in the application in which the source pertains. A single composite with this name can exist
     * in an application. Return null if name conflicts.
     * 
     * @param source The origin if the dependency relationship. The embedding composite.
     * @param name the symbolic name. Unique in this application
     * @param models optionnal : the associated models.
     * @return
     */
    public Composite createComposite(Composite source, String name, Set<ManagerModel> models);

    /**
     * Creates an application from scratch, by deploying an implementation. First creates the root composites
     * (compositeName), associates its models (models). Then installs and creates the specification (from the url or
     * only by its interfaces) then installs and creates the main implementation (implName) from its URL, considered as
     * the application Main. At least the following parameters MUST be non null: appliName, implName, implUrl, specName,
     * interfaces
     * 
     * @param compositeName The name of the root composite.
     * @param models The manager models
     * @param implName The logical name for the Application Main implementation
     * @param implUrl Location of the Main executable.
     * @param implType Type of packaging for main executable.
     * @param specName optional : the logical name of the associated specification
     * @param specUrl Location of the code (interfaces) associated with the main specification.
     * @param specType Type of packaging for the code (interfaces) associated with the main specification.
     * @param interfaces List of the interfaces implemented by this specificaiton.
     * @param properties The initial properties for the Implementation.
     * @return The new created application.
     */
    public Application createAppliDeploySpec(String appliName, Set<ManagerModel> models, String specName, URL specUrl,
            String specType, String[] interfaces, Attributes properties);

    /**
     * Creates an application from scratch, by deploying an implementation. First creates the root composites
     * (compositeName), associates its models (models). Then install an implementation (implName) from its URL,
     * considered as the application Main.
     * 
     * @param compositeName The name of the root composite.
     * @param models The manager models
     * @param implName The logical name for the Application Main implementation
     * @param url Location of the Main executable.
     * @param type Type of packaging.
     * @param specName optional : the logical name of the associated specification
     * @param properties The initial properties for the Implementation.
     * @return The new created application.
     */
    public Application createAppliDeployImpl(String appliName, Set<ManagerModel> models, String implName, URL url,
            String type, String specName, Attributes properties);

    /**
     * Returns the application that has that name. WARNING : more than one application may have same name; in that case
     * return one arbitrarily.
     * 
     * @param name
     * @return
     */
    public Application getApplication(String name);

    /**
     * Returns all the known applications
     * 
     * @return
     */
    public Set<Application> getApplications();

    public ASMSpecBroker getSpecBroker();

    public ASMImplBroker getImplBroker();

    public ASMInstBroker getInstBroker();

    // public void dumpApam();
    //
    // public void dumpAppli(String appliName);
    //
    // public void dumpState(ASMInst inst, String indent, String dep);

}
