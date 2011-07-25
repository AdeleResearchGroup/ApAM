package fr.imag.adele.apam.apamAPI;

import java.util.Set;

import fr.imag.adele.apam.util.Attributes;

public interface Application {

    /**
     * Provides the root composite for that application instance
     * 
     * @return
     */
    public Composite getMainImplComposite();

    /**
     * Provides the root composite for that application instance
     * 
     * @return
     */
    public Composite getMainInstComposite();

    /**
     * Provides the main implementation for that application.
     * 
     * @return
     */
    public ASMImpl getMainImpl();

    /**
     * return the main specification. Never null.
     * 
     * @return
     */
    public ASMSpec getMainSpec();

    /**
     * returns the name of that application
     * 
     * @return
     */
    public String getName();

    /**
     * Provides the list of all composites in this application instance
     * 
     * @return
     */
    public Set<Composite> getComposites();

    /**
     * Provides the composite with that name in this application instance
     * 
     * @return
     */
    public Composite getComposite(String name);

    /**
     * creates an instance of the main implementation associated with the root composite. i.e. starts the application.
     */
    //public void execute(Attributes properties);

}
