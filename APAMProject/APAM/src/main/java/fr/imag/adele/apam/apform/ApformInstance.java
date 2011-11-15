package fr.imag.adele.apam.apform;

import java.util.Map;

import fr.imag.adele.apam.Instance;

public interface ApformInstance {

    /**
     * 
     * @return the symbolic name of that instance. Provided by the platform.
     */
    public String getName();

    /**
     * 
     * @return the real object implementing that instance.
     */

    public Object getServiceObject();

    /**
     * 
     * @return the name of the corresponding implementation. Cannot return null.
     */
    public String getImplemName();

    /**
     * returns the array of attributes.
     * 
     * @return
     */
    public Map<String, Object> getProperties();

    /**
     * provide the destination real address for the provided dependency.
     * Usually performed as the return of method newWire (when lazy)
     * 
     * @param dependency Name of the dependency (field name)
     * @param destInst. Real address of the destination.
     * @return False if it cannot be performed : legacy.
     */
    public boolean setWire(Instance destInst, String depName);

    /**
     * Remove a wire. That dependency is no longer valid (disappear or other reason)
     * 
     * @param dependency name of that dependency
     * @param destInst the old destination object (if multiple).
     * @return false if it could not be performed: legacy.
     */
    public boolean remWire(Instance destInst, String depName);

    /**
     * Change a dependency by another one.
     * 
     * @param dependency
     * @param oldDestInst the previous destination. Can be null if cardinality one.
     * @param newDestInst The new destination.
     * @return false if it could not be performed: legacy.
     */
    public boolean substWire(Instance oldDestInst, Instance newDestInst, String depName);

    public void setInst(Instance asmInstImpl);

}
