package fr.imag.adele.apam;

import java.util.Map;

public interface Properties {
    public Map<String, Object> getAllProperties();

    public Object getProperty(String attr);

    public void setProperty(String attr, Object value);

    public void setAllProperties(Map<String, Object> properties);

}
