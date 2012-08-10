package fr.imag.adele.apam.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.CompositeDeclaration;
import fr.imag.adele.apam.core.PropertyDefinition;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Util;

public abstract class ComponentImpl extends ConcurrentHashMap<String, Object> implements Component {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger logger = LoggerFactory.getLogger(Util.class);

	protected ComponentDeclaration declaration;
	protected ApformComponent      apform           = null;

	private final Object  componentId                = new Object();                 // only for hashCode

	
	public ComponentImpl(ApformComponent apform, Map<String, Object> properties) {
		this.apform = apform;
		this.declaration = apform.getDeclaration() ;
        putAll(apform.getDeclaration().getProperties());

		if (properties != null) putAll (properties);
	}

	@Override
	public boolean equals(Object o) {
		return (this == o);
	}

	@Override
	public int hashCode() {
		return componentId.hashCode();
	}

	public String getName () {
		return declaration.getName() ;
	}

	public ApformComponent getApformComponent () {
		return apform ;
	}

	public ComponentDeclaration getDeclaration () {
		return declaration ;
	}

	@Override
	public boolean match(String filter) {
		return match(ApamFilter.newInstance(filter));
	}

	@Override
	public boolean match(Filter goal) {
		if (goal == null)
			return true;
		try {
			return ((ApamFilter) goal).matchCase(getAllProperties());
		} catch (Exception e) {
		}
		return false;
	}

	@Override
	public boolean match(Set<Filter> goals) {
		if ((goals == null) || goals.isEmpty())
			return true;
		Map props = getAllProperties() ;
		try {
			for (Filter f : goals) {
				if (!((ApamFilter) f).matchCase(props)) {
					return false ;
				}
			}
			return true;
		} catch (Exception e) {
			return false ;
		}
	}



	//properties

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

	public boolean removeProperty(String attr) {
		PropertyDefinition propDef = Util.getAttrDefinition(this, attr) ;
		if (propDef != null && propDef.getField() != null) {
			logger.error("In " + this + " attribute " + attr +  " is a program field and cannot be removed.");
			return false;
		}
		
		Object oldValue = get(attr) ;
		if (oldValue != null) {
			remove(attr);
			ApamManagers.notifyAttributeRemoved(this, attr, oldValue);
			return true ;
		}
		return false ;	
	}

	
	@Override
	public boolean setProperty(String attr, Object value) {
		PropertyDefinition propDef = Util.getAttrDefinition(this, attr) ;
		if (propDef != null && propDef.isInternal() == true) {
			logger.error("In " + this + " attribute " + attr +  " is internal and cannot be set.");
			return false;
		}
		if (Util.validAttr(this, attr, value)){
			Object oldValue = get(attr) ;
			put(attr, value);
			getApformComponent().setProperty (attr, value) ;
			if (oldValue == null) {
				ApamManagers.notifyAttributeAdded(this, attr, value) ;
			} else {
				ApamManagers.notifyAttributeChanged(this, attr, value, oldValue) ;
			}
			return true ;
		}
		return false ;
	}

	/*
	 * 
	 * same as setAttribute but only called by apform 
	 * when the attribute is changed by its associated field in the code.
	 */
	public boolean setInternalProperty(String attr, Object value) {
		//if (Util.validAttr(this, attr, value)){
		put(attr, value);
		propertyChanged(attr,value);
		return true ;
		//        }
		//        return false ;
	}


	@Override
	public boolean setAllProperties(Map<String, Object> properties) {
		for (String attr : properties.keySet()) {
			if (Util.validAttr(this, attr, properties.get(attr))){
				put(attr, properties.get(attr));
			} else return false ;
		}
		propertiesChanged();
		return true ;
	}

	/**
	 * Notify the component when a property changed
	 */
	private void propertyChanged(String attr, Object value){
		
		
	}


	/**
	 * Notify the component when properties changed
	 */
	private void propertiesChanged(){
		
	}
}

