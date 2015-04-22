package fr.imag.adele.apam.apform.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.ipojo.ConfigurationException;
import org.osgi.framework.Version;

import fr.imag.adele.apam.AttrType;
import fr.imag.adele.apam.declarations.PropertyDefinition;
import fr.imag.adele.apam.util.Attribute;

/**
 * This is the callback associated to the APAM property changes
 * 
 * @author vega
 */
public class PropertyCallback extends InstanceCallback<Object> {

    private final PropertyDefinition property;

    public PropertyCallback(ApamInstanceManager.Apform instance, PropertyDefinition property) throws ConfigurationException {
		super(instance,property.getCallback());
		
		this.property = property;
		/*
		 * We force reflection meta-data calculation in the constructor to signal errors as soon as
		 * possible. This however has a cost in terms of early class loading.  
		 */
		try {
			searchMethod();
		} catch (NoSuchMethodException e) {
			throw new ConfigurationException("invalid method declaration in property callback "+getMethod());
		}
	}
	
	public boolean isTriggeredBy(String propertyName) {
		return this.property.getName().equals(propertyName);
	}
	

	@Override
	protected boolean isExpectedParameter(Class<?> parameterType) {
		
		if (this.property.isSet()) {

			/*
			 * Multiply valued  properties can be injected as sets of the basic type of the
			 * property.
			 */

			return 	parameterType.isAssignableFrom(Set.class);
			
		}
		else {

			/*
			 * For atomic values we perform automatic conversion to Strings
			 */
			if (parameterType.isAssignableFrom(String.class)) {
				return true;
			}

			/*
			 * Otherwise we try to match the type of the property
			 */
			int type = Attribute.splitType(this.property.getBaseType()).type;

			if (type == AttrType.STRING) {
				return parameterType.isAssignableFrom(String.class);
			}
			else if (type == AttrType.ENUM) {
				return parameterType.isAssignableFrom(String.class);
			}
			else if (type == AttrType.VERSION) {
				return parameterType.isAssignableFrom(Version.class);
			}
			else if (type == AttrType.BOOLEAN) {
				return parameterType.isAssignableFrom(Boolean.class) || parameterType.isAssignableFrom(Boolean.TYPE);
			}
			else if (type == AttrType.FLOAT) {
				return parameterType.isAssignableFrom(Float.class)  || parameterType.isAssignableFrom(Float.TYPE);
			}
			else if (type == AttrType.INTEGER) {
				return parameterType.isAssignableFrom(Integer.class)  || parameterType.isAssignableFrom(Integer.TYPE);
			}
			
		}
		
		return false;
		
	}

	/**
	 * Perform automatic conversions from the internal representation of property values, to
	 * parameters of the property configuration callback
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Object cast(Object argument) {
		
		if (this.property.isSet()) {

			/*
			 * Multiply valued  properties are internally stored as set of Strings (to be compatible to LDAP filter
			 * matching), to help developers writing callbacks, we automatically convert it to a immutable set of 
			 * values of the specific type of the property. 
			 */

			if (getArgumentType().isAssignableFrom(Set.class)) {

				int type = Attribute.splitType(this.property.getBaseType()).type;

				if (type == AttrType.STRING) {
					return Collections.unmodifiableSet((Set<String>)argument);
				}
				else if (type == AttrType.ENUM) {
					return Collections.unmodifiableSet((Set<String>)argument);
				}
				else if (type == AttrType.BOOLEAN) {
					Set<Boolean> value = new HashSet<Boolean>();
					for (String element : (Set<String>)argument) {
						value.add(Boolean.valueOf(element));
					}
					return Collections.unmodifiableSet(value);
				}
				else if (type == AttrType.FLOAT) {
					Set<Float> value = new HashSet<Float>();
					for (String element : (Set<String>)argument) {
						value.add(Float.valueOf(element));
					}
					return Collections.unmodifiableSet(value);
				}
				else if (type == AttrType.INTEGER) {
					Set<Integer> value = new HashSet<Integer>();
					for (String element : (Set<String>)argument) {
						value.add(Integer.valueOf(element));
					}
					return Collections.unmodifiableSet(value);
				}
				else if (type == AttrType.VERSION) {
					Set<Version> value = new HashSet<Version>();
					for (String element : (Set<String>)argument) {
						value.add(Version.parseVersion(element));
					}
					return Collections.unmodifiableSet(value);
				}

			}
			
		}
		else {

			/*
			 * For atomic values we perform automatic conversion to Strings
			 */
			
			if (getArgumentType().isAssignableFrom(String.class)) {
				return argument.toString();
			}
		}
		
		return argument;
	}
	
}
