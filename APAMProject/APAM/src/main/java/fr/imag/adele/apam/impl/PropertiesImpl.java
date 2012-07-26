package fr.imag.adele.apam.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Properties;
import fr.imag.adele.apam.util.Util;

public abstract class PropertiesImpl extends ConcurrentHashMap<String, Object> implements Properties {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private boolean allProperties =false;
    
    @Override
    public Map<String, Object> getAllProperties() {

        Map<String, Object> allProps = new HashMap<String, Object>(this);
        if (this instanceof Instance) { 
            allProps.putAll(((Instance)this).getImpl().getAllProperties());
            return allProps ;
        }
        if (this instanceof Implementation) {
            allProps.putAll(((Implementation)this).getSpec().getAllProperties());
            return allProps ;            
        }
        return allProps ;
    }

    @Override
    public Object getProperty(String attr) {
        Object ret = get(attr);
        if (ret != null) return ret ;
        if (this instanceof Instance) {
            return ((Instance)this).getImpl().getProperty(attr) ;
        } 
        if (this instanceof Implementation) {
            if (((Implementation) this).getSpec() != null)
                return ((Implementation)this).getSpec().getProperty(attr) ;
            // System.err.println("no spec for " + this);
        } 
        return null ;
    }

    @Override
    public void setProperty(String attr, Object value) {
        if (Util.validAttr(this, attr, value)){
            put(attr, value);
            if (!allProperties){
            	propertyChanged(attr,value);
            }
        }
    }

    @Override
    public void setAllProperties(Map<String, Object> properties) {
    	allProperties = true;
        for (String attr : properties.keySet()) {
            setProperty(attr, properties.get(attr));
        }
        
        if (allProperties){
        	propertiesChanged();
        	allProperties = false;
        }
    }
    
    /**
     * Notify the component when a property changed
     */
    protected abstract void propertyChanged(String attr, Object value);
    
    
    /**
     * Notify the component when properties changed
     */
    protected abstract void propertiesChanged();
}

