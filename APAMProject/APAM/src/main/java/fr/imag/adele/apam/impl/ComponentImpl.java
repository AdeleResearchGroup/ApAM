package fr.imag.adele.apam.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.ApamManagers;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apform.ApformComponent;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.PropertyDefinition;
import fr.imag.adele.apam.util.ApamFilter;
import fr.imag.adele.apam.util.Util;

public abstract class ComponentImpl extends ConcurrentHashMap<String, String> implements Component, Comparable<Component> {

	private final Object  componentId				= new Object();                 // only for hashCode
	private static final long serialVersionUID 		= 1L;

	protected static Logger logger = LoggerFactory.getLogger(ComponentImpl.class);

	private final ApformComponent      apform ;
	private final ComponentDeclaration declaration;


	public ComponentImpl(ApformComponent apform) {
		
		assert apform != null;

		this.apform 		= apform;
		this.declaration	= apform.getDeclaration();
		
	}

	/**
	 * to be called once the Apam entity is fully initialized.
	 * Computes all its attributes, including inheritance. 
	 */
	public void initializeProperties (Map<String, String> initialProperties) {
		/*
		 * get the initial attributes from declaration and overriden initial
		 * properties
		 */
		Map<String, String> props = new HashMap<String, String> (getDeclaration().getProperties()) ;
		if (initialProperties != null)
			props.putAll(initialProperties);

		ComponentImpl group = (ComponentImpl)getGroup () ;

		//First eliminate the attributes which are not valid.
		for (String attr : props.keySet()) {
			if (Util.validAttr(this.getName(), attr)) {
				//At initialization, all valid attributes are ok for specs
				if (group == null || validDef (attr, props.get(attr)))
					put (attr, props.get(attr)) ;
			}
		}

		//then add those coming from its group, avoiding overloads.
		if (group != null) {
			for (String attr : group.getAllProperties().keySet()) {
				if (get(attr) == null) 
					put (attr, ((ComponentImpl)group).get(attr)) ;
			}
		} 

		/*
		 * Add the default values specified in the group for properties not
		 * explicitly specified
		 */
		if (group != null) {
			for (PropertyDefinition definition : group.getDeclaration().getPropertyDefinitions()) {
				if ( definition.getDefaultValue() != null && get(definition.getName()) == null) 
					put (definition.getName(),definition.getDefaultValue()) ;
			}
		} 

		//Finally add the specific attributes. Should be the only place where instanceof is used.
		put (CST.A_NAME, apform.getDeclaration().getName()) ;
		if (this instanceof Specification) {
			put (CST.A_SPECNAME, apform.getDeclaration().getName()) ;
		} else {
			if (this instanceof Implementation) {
				put (CST.A_IMPLNAME, apform.getDeclaration().getName()) ;	
				if (this instanceof CompositeType) {
					put(CST.A_COMPOSITETYPE, CST.V_TRUE);
				}
				if (this instanceof Composite) {
					put(CST.A_COMPOSITE, CST.V_TRUE);
					put(CST.A_MAIN_INSTANCE, ((Composite)this).getMainInst().getName());
				}
			} else  {
				if (this instanceof Instance) {
					put (CST.A_INSTNAME, apform.getDeclaration().getName()) ;
					//put (CST.A_COMPOSITE, ((Instance)this).getComposite().getName());
				}
			}
		}

		//and propagate, to the platform and to members, in case the spec has been created after the implem
		for (String attr : getAllProperties().keySet()) {
			propagateInit (attr, get(attr)) ;
		}
	}

	/**
	 * This methods adds a newly created component to the Apam state model, so that it is visible to the
	 * external API
	 */
	public abstract void register(Map<String, String> initialProperties);


	/**
	 * This method removes a component from the Apam state model, it must ensure that the component is
	 * no longer referenced by any other component or visible by the external API
	 */
	public abstract void unregister();

	/**
	 * Components are uniquely represented in the Apam state model, so we use reference equality
	 * in all comparisons.
	 */
	@Override
	public final boolean equals(Object o) {
		return (this == o);
	}

	/**
	 * TODO Assumes that all components are in the same name space, including instance !!
	 */
	@Override
	public int compareTo(Component that) {  
		return this.getName().compareTo(that.getName());
	}

	/**
	 * Override to make hash code conform to the equality definition
	 */
	@Override
	public final int hashCode() {
		return componentId.hashCode();
	}

	public final String getName () {
		return declaration.getName() ;
	}

	@Override
	public String toString() {
		return getName();
	}

	public final ApformComponent getApformComponent () {
		return apform ;
	}

	public final ComponentDeclaration getDeclaration () {
		return declaration ;
	}

	/**
	 * Get the value of the property.
	 * 
	 * Attributes are supposed to be correct and inherited staticaly
	 * 
	 */
	@Override
	public String getProperty(String attr) {
		return get(attr);
	}

	/**
	 * Get the value of all the properties in the component.
	 *  
	 */

	@Override
	public Map<String, String> getAllProperties() {
		return Collections.unmodifiableMap(this);
	}

	/**
	 * Set the value of the property in the Apam state model. Changing an attribute notifies
	 * the property manager, the underlying execution platform (apform), and propagates to members
	 */
	@Override
	public boolean setProperty(String attr, String value) {
		// attribute names are in lower case
		//attr = attr ;

		/*
		 * Validate that the property is defined and the value is valid 
		 */
		if (!Util.validAttr(this.getName(), attr))
			return false;

		if (!validDef (attr, value))
			return false ;

		//does the change, notifies, changes the platform and propagate to members
		this.propagate (attr, value) ;
		return true ;
	}
	
	/**
	 * During initialisation, set the new (attrbute, value) in the object, 
	 * in the platform, and propagates to the members recursively.
	 * Does not notify managers.
	 * @param com the component to which is added the attribute.
	 * @param attr
	 * @param value
	 */
	private void propagateInit (String attr, String value) {
		//Notify the execution platform
		getApformComponent().setProperty (attr,value);

		//Propagate to members recursively
		if (getMembers() == null)
			return;
		
		for (Component member : getMembers()) {
			((ComponentImpl)member).propagate (attr, value) ;
		}
	}


	/**
	 * set the value, update apform and the platform, notify managers and propagates to the members, recursively
	 * @param com the component on which ot set the attribute
	 * @param attr attribute name
	 * @param value attribute value
	 */

	private void propagate (String attr, String value) {
		//Change value and notify managers
		setInternalProperty(attr,value);

		//Notify the execution platform
		getApformComponent().setProperty (attr,value);

		//Propagate to members recursively
		
		if (getMembers() == null)
			return;
		
		for (Component member : getMembers()) {
			((ComponentImpl)member).propagate (attr, value) ;
		}
	}
	
	/**
	 * Sets the value of a property changed in the state and notifies property managers,
	 * but doesn't call back the execution platform.
	 * 
	 * TODO,IMPORTANT This method should be private, but it is actually directly invoked by 
	 * apform to avoid notification loops. We need to refactor the different APIs of Apam.  
	 */
	public void setInternalProperty(String attr, String value) {
		String oldValue = get(attr);
		put(attr, value);
		/*
		 * notify property managers
		 */
		if (oldValue == null)
			ApamManagers.notifyAttributeAdded(this, attr, value) ;
		else
			ApamManagers.notifyAttributeChanged(this, attr, value, oldValue);
	}

	/**
	 * Sets all the values of the specified properties
	 * 
	 * We validate all attributes before actually modifying
	 * the value to avoid partial modifications ?
	 */
	@Override
	public boolean setAllProperties(Map<String, String> properties) {
		for (String attr : properties.keySet()) {
			String value = properties.get(attr);
			if (! setProperty(attr, value))
				return false;
		}

		return true ;
	}


	/**
	 * Removes the specifed property
	 * 
	 * TODO Should we add this to the API? how to notify apform?
	 */
	public boolean removeProperty(String attr) {
		
		String oldValue = getProperty(attr) ;
		
		if (oldValue == null) {
			logger.error("ERROR: \"" + attr + "\" not instanciated");
			return false;			
		}

		if (Util.isFinalAttribute(attr)) {
			logger.error("ERROR: \"" + attr + "\" is a final attribute");
			return false;
		}

		if (Util.isReservedAttributePrefix(attr)) {
			logger.error("ERROR: \"" + attr + "\" is a reserved attribute");
			return false;
		}
		
		PropertyDefinition propDef = getAttrDefinition(attr) ;
		if (propDef != null && propDef.getField() != null) {
			logger.error("In " + this + " attribute " + attr +  " is a program field and cannot be removed.");
			return false;
		}
		
		if (getGroup() != null && getGroup().getProperty(attr) != null) {
			logger.error("In " + this + " attribute " + attr +  " inherited and cannot be removed.");
			return false;			
		}
		
		//it is ok, remove it and propagate to members, recursively
		propagateRemove(attr) ;
		
		//TODO. Should we notify at all levels ?
		ApamManagers.notifyAttributeRemoved(this, attr, oldValue);
		
		return true ;	
	}

	/**
	 * TODO. Should we notify at all levels ?
	 * @param ent
	 * @param attr
	 */
	private void propagateRemove (String attr) {
		
		remove(attr) ;

		if (getMembers() == null)
			return;
		
		for (Component member : getMembers ()) {
			((ComponentImpl)member).propagateRemove(attr) ;
		}
		
	}

	/**
	 * Tries to find the definition of attribute "attr" associated with component "component".
	 * Returns null if the attribute is not explicitly defined 
	 * @param component
	 * @param attr
	 * @return
	 */
	private PropertyDefinition getAttrDefinition (String attr) {
		Component group = this.getGroup() ;

		while (group != null) {
			PropertyDefinition definition =  group.getDeclaration().getPropertyDefinition(attr);
			if (definition != null)
				return definition;
			
			group = group.getGroup () ;
		}
		
		return null ;
	}


	/**
	 * An attribute is valid if declared in an ancestor, and not set in an ancestor.
	 * Check if the value is conforming to its type (string, int, boolean).
	 * Internal attribute (associated with a field) cannot be set.
	 * Must be called on the level above.
	 * 
	 * Checks if attr is correctly defined for component ent
	 * 	it must be explicitly defined in its upper groups,
	 *  for the top group, it must be already existing. 
	 * @param ent
	 * @param attr
	 * @param value
	 * @return
	 */
	private boolean validDef (String attr, String value) {
		//attr = attr.toLowerCase() ;
		if (Util.isPredefinedAttribute(attr))
			return true;

		Component group = this.getGroup() ;

		//if the same attribute exists above, it is a redefinition.
		if (group != null && group.getProperty(attr) != null) {
			logger.error("cannot redefine attribute \"" + attr + "\"");
			return false;
		}

		//It is a spec. There is no definition
		//for specs the attribute must be already existing
		if (group == null)
			return this.get(attr) != null ;

		PropertyDefinition definition = this.getAttrDefinition (attr) ;

		if (definition == null) {
			logger.error("Attribute \"" + attr + "=" + value + "\" is undefined.");
			return false;
		}
		
		// there is a definition for attr
		if (definition.isInternal()) {
			logger.error("In " + group + " attribute " + attr +  " is internal and cannot be set.");
			return false;
		}
		
		return Util.checkAttrType(attr, value, definition.getType());

	}
	/*
	 * Filter evaluation on the properties of this component
	 */

	@Override
	public boolean match(String goal) {
		return goal == null || match(ApamFilter.newInstance(goal));
	}

	@Override
	public boolean match(Filter goal) {
		return goal == null || match(Collections.singleton(goal));
	}

	@Override
	public boolean match(Set<Filter> goals) {
		if ((goals == null) || goals.isEmpty())
			return true;

		Map<String,String> props = getAllProperties() ;
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

}

