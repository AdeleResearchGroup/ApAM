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
     * Creates an application from scratch, by deploying an implementation. First creates the root composites
     * (compositeName), associates its models (modles). Then install an implementation (implName) from its URL,
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
    public Application createAppli(String appliName, Set<ManagerModel> models, String implName, URL url, String type,
            String specName, Attributes properties);

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

    public void dumpApam();
}
