package fr.imag.adele.apam.apform.message.impl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.metadata.Element;

import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.apformipojo.ImplementationHandler;
import fr.imag.adele.apam.apformipojo.handlers.MessageConsumerHandler.ComponentType;

public class MessageUtils {
	/**
	 * 
	 * @param componentMetadata
	 * @return
	 */
	public static ComponentType getComponentType(Element componentMetadata) {

		if (componentMetadata.getName().equals(CST.A_SPECIFICATION)) {
			return ComponentType.SPECIFICATION;
		} else if (componentMetadata.getName().equals(CST.A_IMPLEMENTATION)) {
			return ComponentType.IMPLEMENTATION;
		} else if (componentMetadata.getName().equals(CST.A_INSTANCE)) {
			return ComponentType.INSTANCE;
		}
		return null;
	}

	/**
	 * This method return the parameter type of a field
	 * 
	 * @param clazz
	 *            of the field
	 * @param authorizedFieldType
	 *            the type field witch want to intercept
	 * @param fieldName
	 *            the name of the field to intercept
	 * @param componentName
	 *            the name of the component
	 * @param messageName
	 *            the name of message type
	 * @return the parameter type of the field as a Type or null if the
	 *         parameter is not found
	 * @throws ConfigurationException
	 *             when the field is not accessible or is not declared
	 */
	public static String getFieldParameterType(Class<?> clazz, Class<?> authorizedFieldType, String fieldName,
			String componentName) throws ConfigurationException {
		Field field;
		try {
			field = clazz.getDeclaredField(fieldName);

			Type fieldType = field.getGenericType();
			if (fieldType instanceof ParameterizedType) {
				ParameterizedType fieldParametizedType = (ParameterizedType) field.getGenericType();
				if (authorizedFieldType.equals(fieldParametizedType.getRawType())) {
					Type[] parameters = fieldParametizedType.getActualTypeArguments();
					if ((parameters != null) && (parameters.length == 1) && (parameters[0] instanceof Class))
						return ((Class<?>)parameters[0]).getCanonicalName();
				}
			}

		} catch (SecurityException e) {
			throw new ConfigurationException("APAM Message "
					+ ImplementationHandler.quote(componentName + "." + fieldName) + ": " + "the specified field "
					+ ImplementationHandler.quote(fieldName) + " is not accesible in the implementation class or is wrong spelled");
		} catch (NoSuchFieldException e) {
			throw new ConfigurationException("APAM Message "
					+ ImplementationHandler.quote(componentName + "." + fieldName) + ": " + "the specified field "
					+ ImplementationHandler.quote(fieldName) + " is not declared in the implementation class or is wrong spelled");
		}
		return null;
	}

	/**
	 * This method return the parameter type of an argument
	 * 
	 * @param clazz
	 *            of the field
	 * @param authorizedFieldType
	 *            the type field witch want to intercept
	 * @param fieldName
	 *            the name of the field to intercept
	 * @param componentName
	 *            the name of the component
	 * @param messageName
	 *            the name of message type
	 * @return the parameter type of the field as a Type or null if the
	 *         parameter is not found
	 * @throws ConfigurationException
	 *             when the field is not accessible or is not declared
	 */
	public static Type getArgumentParameterType(Class<?> clazz, Class<?> authorizedFieldType, String fieldName,
			String componentName) throws ConfigurationException {
		Field field;
		try {
			field = clazz.getDeclaredField(fieldName);

			Type fieldType = field.getGenericType();
			if (fieldType instanceof ParameterizedType) {
				ParameterizedType fieldParametizedType = (ParameterizedType) field.getGenericType();
				if (authorizedFieldType.equals(fieldParametizedType.getRawType())) {
					Type[] parameters = fieldParametizedType.getActualTypeArguments();
					if ((parameters != null) && (parameters.length == 1)) {
						return parameters[0];
						
					}

				}
			}

		} catch (SecurityException e) {
			throw new ConfigurationException("APAM Message "
					+ ImplementationHandler.quote(componentName + "." + fieldName) + ": " + "the specified field "
					+ ImplementationHandler.quote(fieldName) + " is not accesible in the implementation class");
		} catch (NoSuchFieldException e) {
			throw new ConfigurationException("APAM Message "
					+ ImplementationHandler.quote(componentName + "." + fieldName) + ": " + "the specified field "
					+ ImplementationHandler.quote(fieldName) + " is not declared in the implementation class");
		}
		return null;
	}

}
