package fr.imag.adele.apam.util;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.ASMImpl.ASMImplImpl;
import fr.imag.adele.apam.ASMImpl.ASMInstImpl;
import fr.imag.adele.apam.ASMImpl.ASMSpecImpl;
import fr.imag.adele.apam.apamAPI.AttributeManager;
import fr.imag.adele.apam.apamAPI.Manager;

public class AttributesImpl extends Dictionary<String, Object> implements Attributes {

    /** The properties. */
    private final Map<String, Object>    properties          = new ConcurrentHashMap<String, Object>();
    private static Set<AttributeManager> attrChangedManagers = new HashSet<AttributeManager>();

    public static void addAttrChanged(AttributeManager manager) {
        AttributesImpl.attrChangedManagers.add(manager);
    }

    public static void removeAttrChanged(AttributeManager manager) {
        AttributesImpl.attrChangedManagers.remove(manager);
    }

    public Dictionary<String, Object> attr2Dictionary() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.imag.adele.am.Property#getProperties()
     */
    @Override
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.imag.adele.am.Property#getProperty(java.lang.String)
     */
    @Override
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.imag.adele.am.Property#removeProperty(java.lang.String)
     */
    @Override
    public void removeProperty(String key) {
        properties.remove(key);
    }

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
    @Override
    public void setProperties(Map<String, Object> newProperties) {
        for (String prop : newProperties.keySet()) {
            setProperty0(prop, newProperties.get(prop), false);
        }
    }

    /**
     * Called when SAM notifies that the properties of an instance have been changed. The method checks which attributes
     * have been changed/created/deleted and asks the attribute managers to verify if this is legal (type checking ...).
     * If legal,the attributes of the associated APAM instance are changed accordingly; if not the old value replaces
     * the changed values in the SAM instance. WARNING : if SAM cannot revert that attribute in the real instance, the
     * Apam attribute and the real instance property may have different values.
     * 
     * @param newSamProperties as provided by SAM.
     */
    @Override
    public void setSamProperties(Map<String, Object> newProperties) {
        if (newProperties == null)
            return;
        for (String prop : newProperties.keySet()) {
            setProperty0(prop, newProperties.get(prop), true);
        }
    }

    private void setChangeInSam(String prop, Object propVal) {
        if ((prop == null) || (propVal == null))
            return;
        if (!(this instanceof ASMInstImpl))
            return;
        try {
            ((ASMInstImpl) this).getSAMInst().setProperty(prop, propVal);
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
    }

    private void removeChangeInSam(String prop) {
        if (prop == null)
            return;
        if (!(this instanceof ASMInstImpl))
            return;
        try {
            ((ASMInstImpl) this).getSAMInst().removeProperty(prop);
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
    }

    private void changedAttr(String prop, Object propVal, boolean samChange) {
        boolean ok = true;
        for (AttributeManager man : AttributesImpl.attrChangedManagers) {
            if (this instanceof ASMSpecImpl) {
                ok = man.attrSpecChanged((ASMSpecImpl) this, prop, propVal);
            } else if (this instanceof ASMImplImpl) {
                ok = man.attrImplChanged((ASMImplImpl) this, prop, propVal);
            } else if (this instanceof ASMInstImpl) {
                ok = man.attrInstChanged((ASMInstImpl) this, prop, propVal);
            }
            if (!ok)
                break;
        }
        if (ok) { // propagate the change in ASM
            properties.put(prop, propVal);
            changeScope(prop, propVal);
            if (!samChange) { // propagate also in SAM
                setChangeInSam(prop, propVal);
            }
        } else { // not Ok
            if (samChange) { // revert the change in SAM
                if (properties.get(prop) != null)
                    setChangeInSam(prop, properties.get(prop));
            }
        }
    }

    private void addedAttr(String prop, Object propVal, boolean samChange) {
        boolean ok = true;
        for (AttributeManager man : AttributesImpl.attrChangedManagers) {
            if (this instanceof ASMSpecImpl) {
                ok = man.attrSpecAdded((ASMSpecImpl) this, prop, propVal);
            } else if (this instanceof ASMImplImpl) {
                ok = man.attrImplAdded((ASMImplImpl) this, prop, propVal);
            } else if (this instanceof ASMInstImpl) {
                ok = man.attrInstAdded((ASMInstImpl) this, prop, propVal);
            }
            if (!ok) {
                break;
            }
        }
        if (ok) {
            properties.put(prop, propVal);
            changeScope(prop, propVal);
            if (!samChange) { // propagate also in SAM
                setChangeInSam(prop, propVal);
            }
        } else { // not Ok
            if (samChange) { // revert the change in SAM
                removeChangeInSam(prop);
            }
        }
    }

    private void changeScope(String attr, Object scope) {
        if ((scope instanceof String) && attr.equals(CST.A_SCOPE)) {
            if (((String) scope).equals(CST.V_APPLI) || ((String) scope).equals(CST.V_LOCAL)
                    || ((String) scope).equals(CST.V_COMPOSITE)
                    || ((String) scope).equals(CST.V_GLOBAL)) {
                properties.put(attr, scope);
            } else
                System.err.println("ERROR in " + this + " : invalid shared value : " + scope);
        }
    }

    /**
     * called by Apam and its managers.
     */
    @Override
    public void setProperty(String prop, Object propVal) {
        if ((prop == null) || (propVal == null))
            return;

        setProperty0(prop, propVal, false);
    }

    /**
     * called either by Apam or by SAM
     * 
     * @param prop
     * @param propVal
     * @param samChange true if it is a change in SAM
     */
    public void setProperty0(String prop, Object propVal, boolean samChange) {
        if (properties.containsKey(prop)) {
            Object attrVal = properties.get(prop);
            if (((propVal instanceof String) && (!propVal.equals(attrVal))) || (propVal != attrVal)) {
                changedAttr(prop, propVal, samChange);
            }
        } else { // Look for a new property
            addedAttr(prop, propVal, samChange);
        }
    }

    @Override
    public void setProperty(Manager manager, String key, Object value) {
        if ((manager == null) || (key == null) || (value == null))
            return;

        if (AttributesImpl.attrChangedManagers.contains(manager)) {
            properties.put(key, value);
            changeScope(key, value);
        } else {
            setProperty(key, value);
        }
    }

    @Override
    public void setProperties(Manager manager, Map<String, Object> properties) {
        if ((manager == null) || (properties == null))
            return;
        if (AttributesImpl.attrChangedManagers.contains(manager)) {
            this.properties.putAll(properties);
        }
        if (this.properties.get(CST.A_SCOPE) != null) {
            changeScope(CST.A_SCOPE, properties.get(CST.A_SCOPE));
        }
    }

    @Override
    public Enumeration<Object> elements() {
        return new Iterator2Enumeration(properties.values().iterator());
    }

    @Override
    public Object get(Object key) {
        if (key == null)
            return null;
        return properties.get(key);
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public Enumeration<String> keys() {
        return new StringIterator2Enumeration(properties.keySet().iterator());
    }

    @Override
    public Object put(String key, Object value) {
        if (key == null)
            return null;
        return properties.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        if (key == null)
            return null;
        return properties.remove(key);
    }

    @Override
    public int size() {
        return properties.size();
    }

    public class StringIterator2Enumeration implements Enumeration<String> {
        public StringIterator2Enumeration(Iterator<String> iterator) {
            _iterator = iterator;
        }

        @Override
        public boolean hasMoreElements() {
            return _iterator.hasNext();
        }

        @Override
        public String nextElement() throws NoSuchElementException {
            return _iterator.next();
        }

        private final Iterator<String> _iterator;
    }

    public class Iterator2Enumeration implements Enumeration<Object> {
        public Iterator2Enumeration(Iterator<Object> iterator) {
            _iterator = iterator;
        }

        @Override
        public boolean hasMoreElements() {
            return _iterator.hasNext();
        }

        @Override
        public Object nextElement() throws NoSuchElementException {
            return _iterator.next();
        }

        private final Iterator<Object> _iterator;
    }

    /**
     * WARNING : valid only if all values are string !
     * 
     * @return
     */
    @Override
    public Properties attr2Properties() {
        Properties prop = new Properties();
        prop.putAll(properties);
        return prop;
    }

}
