package fr.imag.adele.apam.util;

import java.util.Map;
import java.util.Properties;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.apamAPI.Manager;

//import fr.imag.adele.am.exception.ConnectionException;

public interface Attributes {

    // Name of the application that created that object
    public static final String APAMAPPLI = "ApamApplication";
    public static final String APAMCOMPO = "ApamComposite";

    /**
     * Get the property of a targeted entity for the given key.
     * 
     * @param key the key
     * @return the property
     * @throws ConnectionException the connection exception
     */
    public Object getProperty(String key);

    /**
     * Get the set of properties for a targeted entity.
     * 
     * @return the properties
     * @throws ConnectionException the connection exception
     */
    public Map<String, Object> getProperties();

    public Properties attr2Properties();

    /**
     * The method asks the attribute managers to verify if that (attribute/value) pair is legal (type checking ...). If
     * legal,the attributes/value is added to the associated instance, and also changed in the SAM associated instance.
     * if not the change is ignored. WARNING : if SAM cannot set that attribute in the real instance, the Apam attribute
     * and the real instance property may have different values.
     * 
     * @param key the key
     * @param value the value
     */
    public void setProperty(String key, Object value);

    /**
     * Same as previous method, but if the manager is registered, the changes are accepted without check, and sent to
     * SAM.
     * 
     * @param manager
     * @param key
     * @param value
     */
    public void setProperty(Manager manager, String key, Object value);

    /**
     * The method is called by Apam managers (when creating ASM entities for example). The method checks which
     * attributes have been changed/created/deleted (if the entity was already existing) and asks the attribute managers
     * to verify if each (attribute/value) pair is legal (type checking ...). The legal (attributes/value) pairs are
     * added to the associated APAM instance, and also changed in the SAM associated instance. The illegal
     * (attributes/value) pairs are ignored. WARNING : if SAM cannot set these attributes in the real instance, the Apam
     * attributes and the real instance properties may have different values.
     * 
     * @param properties the properties
     */
    public void setProperties(Map<String, Object> properties);

    /**
     * Same as previous method, but if the manager is registered, the changes are accepted without check, and sent to
     * SAM.
     * 
     * @param manager
     * @param properties
     */
    public void setProperties(Manager manager, Map<String, Object> properties);

//    /**
//     * Called when SAM notifies that the properties of an instance have been changed. The method checks which attributes
//     * have been changed/created/deleted and asks the attribute managers to verify if this is legal (type checking ...).
//     * If legal,the attributes of the associated APAM instance are changed accordingly; if not the old value replaces
//     * the changed values in the SAM instance. WARNING : if SAM cannot revert that attribute in the real instance, the
//     * Apam attribute and the real instance property may have different values.
//     * 
//     * @param newSamProperties as provided by SAM.
//     */
//    public void setSamProperties(Map<String, Object> newSamProperties);

    /**
     * 
     * @param key
     * @throws ConnectionException
     */
    public void removeProperty(String key);
}
