package fr.imag.adele.apam.util;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fr.imag.adele.apam.ASM;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.AttributeManager;
import fr.imag.adele.apam.apamAPI.Manager;
import fr.imag.adele.apam.samAPIImpl.ASMImplImpl;
import fr.imag.adele.apam.samAPIImpl.ASMInstImpl;
import fr.imag.adele.apam.samAPIImpl.ASMSpecImpl;

public class ApamProperty extends Dictionary<String, Object> implements Attributes {

	/** The properties. */
	private Map<String, Object> properties = new ConcurrentHashMap<String, Object>();
	private ASMSpec theSpec = null ;
	private ASMImpl theImpl = null ;
	private ASMInst theInst = null ;
	private static Set <AttributeManager> attrChangedManagers = new HashSet <AttributeManager> () ;

	public static void addAttrChanged (AttributeManager manager) {
		attrChangedManagers.add (manager) ;
	}
	public static void removeAttrChanged (AttributeManager manager) {
		attrChangedManagers.remove (manager) ;
	}

	/*
	 * (non-Javadoc)
	 * @see fr.imag.adele.am.Property#getProperties()
	 */
	public Map<String, Object> getProperties()  {
		return Collections.unmodifiableMap(this.properties);
	}

	/*
	 * (non-Javadoc)
	 * @see fr.imag.adele.am.Property#getProperty(java.lang.String)
	 */
	public Object getProperty(String key)  {
		return this.properties.get(key);
	}

	/*
	 * (non-Javadoc)
	 * @see fr.imag.adele.am.Property#removeProperty(java.lang.String)
	 */
	public void removeProperty(String key)  {
		this.properties.remove(key);
	}

	/*
	 * (non-Javadoc)
	 * @see fr.imag.adele.am.Property#setProperties(java.util.Map)
	 */
	@Override
	public void setProperties(Map<String, Object> newProperties) {
		for (String prop : newProperties.keySet()) {
			setProperty(prop, newProperties.get(prop)) ;
		}
	}

	private void changedAttr (String prop, Object propVal) {
		boolean ok = true ;
		for (AttributeManager man : attrChangedManagers) {
			if (this instanceof ASMSpecImpl) {
				ok = man.attrSpecChanged((ASMSpecImpl)this, prop, propVal) ;
			} else if (this instanceof ASMImplImpl) {
				ok = man.attrImplChanged((ASMImplImpl)this, prop, propVal) ;
			}
			else if (this instanceof ASMInstImpl) {
				ok = man.attrInstChanged((ASMInstImpl)this, prop, propVal) ;
			}
			if (!ok) break ;
		}
		if (ok) {
			properties.put (prop, propVal) ;
			changeShared (prop, propVal) ;
		}
	}

	private void addedAttr (String prop, Object propVal) {
		boolean ok = true ;
		for (AttributeManager man : attrChangedManagers) {
			if (this instanceof ASMSpecImpl) {
				ok = man.attrSpecAdded((ASMSpecImpl)this, prop, propVal) ;
			} else if (this instanceof ASMImplImpl) {
				ok = man.attrImplAdded((ASMImplImpl)this, prop, propVal) ;
			}
			else if (this instanceof ASMInstImpl) {
				ok = man.attrInstAdded((ASMInstImpl)this, prop, propVal) ;
			}
			if (!ok) {
				break ;
			}
		}
		if (ok) {
			properties.put (prop, propVal) ;
			changeShared (prop, propVal) ;
		}
	}

	private void changeShared (String attr, Object shared) {
		if ((shared instanceof String) && attr.equals (ASM.PSHARED)) {
			if (this instanceof ASMSpecImpl) {
				((ASMSpecImpl)this).setShared(ASM.shared2Int((String)shared)) ;
				return ;
			}  
			if (this instanceof ASMImplImpl) {
				((ASMImplImpl)this).setShared(ASM.shared2Int((String)shared)) ;
				return ;
			}
			((ASMInstImpl)this).setShared(ASM.shared2Int((String)shared)) ;
		}
	}

	@Override
	public void setProperty(String prop, Object propVal) {
		if (properties.containsKey(prop)) {
			Object attrVal = properties.get(prop) ;
			if (((propVal instanceof String) && (!propVal.equals(attrVal))) 
					|| (propVal != attrVal)) {
				changedAttr (prop, propVal) ;
			}
		} else { //Look for a new property
			addedAttr (prop,  propVal) ;
		}	
	}  


	@Override
	public void setProperty(Manager manager, String key, Object value) {
		if (attrChangedManagers.contains(manager)) {
			this.properties.put(key, value);
			changeShared(key, value) ;
		}
		else {
			setProperty(key, value) ;
		}
	}
	
	@Override
	public void setProperties(Manager manager, Map<String, Object> properties) {
		if (attrChangedManagers.contains(manager)) {
			this.properties.putAll(properties);
		}
		if (this.properties.get(ASM.PSHARED) != null) 	{
			changeShared(ASM.PSHARED, properties.get(ASM.PSHARED)) ;
		}
	}

	@Override
	public Enumeration<Object> elements() {
		return new Iterator2Enumeration(properties.values().iterator());
	}

	@Override
	public Object get(Object key) {
		return properties.get(key);
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public Enumeration<String> keys() {
		return  new StringIterator2Enumeration(properties.keySet().iterator()) ;
	}

	@Override
	public Object put(String key, Object value) {
		return properties.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return properties.remove(key);
	}

	@Override
	public int size() {
		return properties.size();
	}

	public  class StringIterator2Enumeration implements Enumeration<String> {
		public StringIterator2Enumeration(Iterator<String> iterator) {
			_iterator = iterator;
		}
		public boolean hasMoreElements() {
			return _iterator.hasNext();
		}
		public String nextElement() throws NoSuchElementException {
			return _iterator.next();
		}
		private Iterator<String> _iterator;
	}


	public  class Iterator2Enumeration implements Enumeration<Object> {
		public Iterator2Enumeration(Iterator<Object> iterator) {
			_iterator = iterator;
		}
		public boolean hasMoreElements() {
			return _iterator.hasNext();
		}
		public Object nextElement() throws NoSuchElementException {
			return _iterator.next();
		}
		private Iterator<Object> _iterator;
	}

}
