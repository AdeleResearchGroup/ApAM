package fr.imag.adele.apam.util;

import java.util.Map;
//import java.util.Properties;

import fr.imag.adele.apam.apamAPI.Manager;

//import fr.imag.adele.am.exception.ConnectionException;

	public interface Attributes {

	    /**
	     * Get the property of a targeted entity for the given key.
	     * 
	     * @param key the key
	     * @return the property
	     * @throws ConnectionException the connection exception
	     */
	    public Object getProperty(String key) ;

	    /**
	     * Get the set of properties for a targeted entity.
	     * 
	     * @return the properties
	     * @throws ConnectionException the connection exception
	     */
	    public Map<String, Object> getProperties() ;

	    /**
	     * Set a property for a targeted entity.
	     * 
	     * @param key the key
	     * @param value the value
	     * @throws ConnectionException the connection exception
	     */
	    public void setProperty(Manager manager, String key, Object value) ;
	    public void setProperty(String key, Object value) ;

	    /**
	     * Set a set of properties for a targeted entity.
	     * 
	     * @param properties the properties
	     * @throws ConnectionException the connection exception
	     */
	    public void setProperties(Map<String, Object> properties);
	    public void setProperties(Manager manager, Map<String, Object> properties);

	    /**
	     * 
	     * @param key 
	     * @throws ConnectionException
	     */
	    public void removeProperty(String key) ;
}

