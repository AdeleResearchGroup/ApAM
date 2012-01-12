/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.imag.adele.apam.apformipojo.handlers;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Callback;
import org.osgi.service.wireadmin.WireConstants;

import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.message.impl.MessageAbstractProducer;
import fr.imag.adele.apam.apform.message.impl.MessageConstants;
import fr.imag.adele.apam.apform.message.impl.MessageUtils;
import fr.imag.adele.apam.apformipojo.ApformIpojoImplementation;
import fr.imag.adele.apam.apformipojo.ApformIpojoInstance;
import fr.imag.adele.apam.apformipojo.Dependency;
import fr.imag.adele.apam.apformipojo.ImplementationHandler;
import fr.imag.adele.apam.apformipojo.legacy.ApformIpojoLegacyInstance;
import fr.imag.adele.apam.message.AbstractProducer;
import fr.imag.adele.apam.message.Message;
import fr.imag.adele.apam.message.Strategy;

/**
 * This class handle message declarations for APAM implementations.
 * 
 * @author Mehdi
 * 
 */
public class MessageConsumerHandler extends PrimitiveHandler {

	/**
	 * The apform instance for legacy iPojo components
	 */
	private ApformIpojoLegacyInstance apformLegacyInstance;

	/**
	 * this is the Factory of the WireAdmin Consumer witch will act as a Pull
	 * Consumer Injected by iPOJO
	 */
	private Factory wireAdminConsumer;

	/**
	 * Link flavor and the abstract producers (needed for the registration)
	 */
	private Map<String, List<MessageAbstractProducer>> flavorToMessageAbstractProducers;

	/**
	 * Represent the instance of wireAdmin consumer
	 */
	private ComponentInstance wa_consumerInstance;

	public enum ComponentType {
		SPECIFICATION, IMPLEMENTATION, INSTANCE
	}

	/*
	 * @see
	 * org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.
	 * felix.ipojo.architecture. ComponentTypeDescription,
	 * org.apache.felix.ipojo.metadata.Element)
	 */
	@Override
	public void initializeComponentFactory(ComponentTypeDescription componentDescriptor, Element componentMetadata)
			throws ConfigurationException {

		/*
		 * This handler works only for APAM native implementations
		 */
		if (!(componentDescriptor instanceof ApformIpojoImplementation.Description))
			throw new ConfigurationException("APAM message handler can only be used on APAM components "
					+ componentMetadata);

		boolean isApamImplementation = componentDescriptor instanceof ApformIpojoImplementation.Description;
		ApformIpojoImplementation.Description implementationDescription = (ApformIpojoImplementation.Description) componentDescriptor;

		String componentName = componentDescriptor.getName();

		ComponentType componentType = MessageUtils.getComponentType(componentMetadata);
		if (componentType == null)
			throw new ConfigurationException(
					"APAM message handler can only be used on APAM specification, implementation or instance "
							+ componentMetadata);

		boolean hasInstrumentedCode = (!isApamImplementation)
				|| implementationDescription.getFactory().hasInstrumentedCode();

		Class<?> instrumentedCode = null;
		try {
			instrumentedCode = hasInstrumentedCode ? getFactory().loadClass(getFactory().getClassName()) : null;

		} catch (ClassNotFoundException e) {
			throw new ConfigurationException("iPojo component " + ImplementationHandler.quote(componentName) + ": "
					+ "the component class " + getFactory().getClassName() + " can not be loaded");
		}

		/*
		 * Iterate over user defined messages
		 */
		Element messageDeclarations[] = componentMetadata.getElements(MessageConstants.MESSAGE_DECLARATION,
				ApformIpojoImplementation.APAM_NAMESPACE);

		for (Element messageDeclaration : messageDeclarations) {

			String messageName = messageDeclaration.getAttribute(MessageConstants.MESSAGE_NAME_PROPERTY);
			String messageStrategy = messageDeclaration.getAttribute(MessageConstants.MESSAGE_STRATEGY_PROPERTY);
			String messageFieldName = null;
			String messageMethodName = null;
			String messageTypeFieldOrMethod = null;
			switch (componentType) {

				case INSTANCE:
					// TODO have to be specified

				case IMPLEMENTATION:
					messageFieldName = messageDeclaration
							.getAttribute(MessageConstants.MESSAGE_CONSUMER_FIELD_PROPERTY);
					messageMethodName = messageDeclaration.getAttribute(MessageConstants.MESSAGE_METHOD_PROPERTY);
					if (hasInstrumentedCode && messageFieldName == null && messageMethodName == null) {
						throw new ConfigurationException("APAM Message " + ImplementationHandler.quote(componentName)
								+ ": " + "a field or a method, must be specified");
					}

					if (hasInstrumentedCode) {
						/*
						 * Check the field on Pull strategy
						 */
						if (messageFieldName != null) {
							/*
							 * Add Strategy to the component description if it's
							 * not declared
							 */

							if (messageStrategy == null) {
								messageStrategy =Strategy.pull.name();
								messageDeclaration.addAttribute(new Attribute(
										MessageConstants.MESSAGE_STRATEGY_PROPERTY,messageStrategy));
								
							}

							/*
							 * Try to check the parameter type of the Field
							 * AbstractProducer
							 */
							Type messageType = MessageUtils.getArgumentParameterType(instrumentedCode,
									AbstractProducer.class, messageFieldName, componentName);
							
							if (messageType instanceof ParameterizedType) {
								ParameterizedType parameterized = (ParameterizedType) messageType;
									Class<?> raw = (Class<?>) parameterized.getRawType();
									messageTypeFieldOrMethod= raw.getCanonicalName();
							}else {
								messageTypeFieldOrMethod = ((Class<?>)messageType) .getCanonicalName();
							}
							
							
						}
						/*
						 * Check the method on Push strategy
						 */
						else if (messageMethodName != null) {

							/*
							 * Add Strategy to the component description if it's
							 * not declared
							 */

							if (messageStrategy == null) {
								messageStrategy =Strategy.push.name();
								messageDeclaration.addAttribute(new Attribute(
										MessageConstants.MESSAGE_STRATEGY_PROPERTY, messageStrategy));
							}

							/*
							 * search the method on the class
							 */
							Method method = searchMethodInMethodArray(instrumentedCode.getDeclaredMethods(),
									messageMethodName, 2);

							if (method != null) {
								/*
								 * the method must have only one parameter
								 */
								if (method.getGenericParameterTypes().length>=1){
									
								
									Type parameterType = method.getGenericParameterTypes()[0];
						
									if (parameterType instanceof ParameterizedType) {
										
										ParameterizedType fieldParametizedType = (ParameterizedType) parameterType;
										if (Message.class.equals(fieldParametizedType.getRawType())) {
											Type[] parameters = fieldParametizedType.getActualTypeArguments();
											if ((parameters != null) && (parameters.length == 1) && (parameters[0] instanceof Class))
												messageTypeFieldOrMethod= ((Class<?>)parameters[0]).getCanonicalName();
										}
										/*
										 * in case of Message.class as argument
										 */
	//									if (messageTypeClass.isInstance(ParameterizedType) {
										
//											parameterType
//											Type[] parameters = parameterType.class.getTypeParameters();
//											if ((parameters.length == 1) && (parameters[0] instanceof Class))
//												messageTypeFieldOrMethod = ((Class<?>)parameters[0]).getCanonicalName();
	//									} else {
	//										messageTypeClass = null;
	//									}
									}else {
										messageTypeFieldOrMethod = ((Class<?>) parameterType).getCanonicalName();
									}
								}
							} else {
								throw new ConfigurationException(
										"APAM Message "
												+ ImplementationHandler.quote(componentName + "." + messageMethodName)
												+ ": "
												+ "the specified method "
												+ ImplementationHandler.quote(messageMethodName)
												+ " is not declared in the implementation class, the method must have only one parameter");
							}
						}
						if (messageName==null && messageTypeFieldOrMethod == null) {
							throw new ConfigurationException("APAM Message "
									+ ImplementationHandler.quote(componentName ) + ": "
									+ "message name is not specified and the field (or method) has no parameter type " );
						}

						if (messageName!=null && messageTypeFieldOrMethod != null) {
							if (!messageName.equals(messageTypeFieldOrMethod)){
								String responsable = messageFieldName;
								if (messageMethodName!=null){
									responsable = messageMethodName;
								}
								throw new ConfigurationException("APAM Message "
										+ ImplementationHandler.quote(componentName + "." + responsable) + ": "
										+ "the specified message name " + ImplementationHandler.quote(messageName)
										+ " and type of AbstractProducer " + ImplementationHandler.quote(messageTypeFieldOrMethod)
										+ " are not the same!");
							}
						
							System.out.println("init >>>>>>>>>>>>>" + messageName);
						}else if (messageName==null && messageTypeFieldOrMethod != null) {
							messageName = messageTypeFieldOrMethod;
							messageDeclaration.addAttribute(new Attribute(MessageConstants.MESSAGE_NAME_PROPERTY,
									messageName));
							System.out.println("init >>>>>>>>>>>>>" + messageName);
						}
					}

				case SPECIFICATION:
					if ((messageName == null) && (messageFieldName == null))
						throw new ConfigurationException("APAM message " + implementationDescription.getName() + ": "
								+ "a message declaration must specifiy a name");

					if (messageStrategy == null) {
						System.out.println("[Warning] " +componentName +" has no message strategy defined, it will be checked on the implementation.");
					}
					break;
			}

		}

	}

	/**
	 * Searches the {@link Method} in the given method arrays.
	 * 
	 * @param methods
	 *            the method array in which the method should be found
	 * @param methodName
	 *            the name of the method to search
	 * @param argLength
	 *            the length of arguments of the method
	 * @return the method object or <code>null</code> if not found
	 */
	private Method searchMethodInMethodArray(Method[] methods, String methodName, int argLength) {
		for (int i = 0; i < methods.length; i++) {
			// First check the method name
			if (methods[i].getName().equals(methodName)) {
				// Check arguments
				Class<?>[] parametersTypes = methods[i].getParameterTypes();
				if (parametersTypes.length <= argLength) { // Test size to avoid
															// useless
					// loop

					return methods[i]; // It is the looked method.
				}
			}
		}
		return null;
	}

	@Override
	public void configure(Element componentMetadata, Dictionary configuration) throws ConfigurationException {

		/*
		 * Add interceptors and callbaks to manage the message consumption
		 * 
		 * NOTE All validations were already performed when validating the
		 * factory @see initializeComponentFactory, including initializing
		 * unspecified properties with appropriate default values. Here we just
		 * assume metadata is correct.
		 */

		Element messageDeclarations[] = componentMetadata.getElements(MessageConstants.MESSAGE_DECLARATION,
				ApformIpojoImplementation.APAM_NAMESPACE);
		flavorToMessageAbstractProducers = new Hashtable<String, List<MessageAbstractProducer>>();
		for (Element messageDeclaration : messageDeclarations) {

			String messageName = messageDeclaration.getAttribute(MessageConstants.MESSAGE_NAME_PROPERTY);
			String messageFieldName = messageDeclaration.getAttribute(MessageConstants.MESSAGE_CONSUMER_FIELD_PROPERTY);
			String messageMetohdName = messageDeclaration.getAttribute(MessageConstants.MESSAGE_METHOD_PROPERTY);
			String strategy = messageDeclaration.getAttribute(MessageConstants.MESSAGE_STRATEGY_PROPERTY);
			System.out.println("configure >>>>>>>>>>>>>" + messageName);
			InstanceManager attachedInstance = getInstanceManager();
			Dependency.Resolver resolver = null;

			if (attachedInstance instanceof ApformIpojoInstance) {
				resolver = (ApformIpojoInstance) attachedInstance;
			} else {
				apformLegacyInstance = new ApformIpojoLegacyInstance(attachedInstance);
				resolver = apformLegacyInstance;
			}

			/*
			 * Depending in the strategy wee should create a field interceptor,
			 * a callback method or both of them.
			 */

			ComponentType componentType = MessageUtils.getComponentType(componentMetadata);
			if (flavorToMessageAbstractProducers.get(messageName) == null) {
				flavorToMessageAbstractProducers.put(messageName, new ArrayList<MessageAbstractProducer>());
			}
			// consumerflavors.add(messageName);
			switch (componentType) {
				case IMPLEMENTATION:
					if (Strategy.push.name().equalsIgnoreCase(strategy)) {
						MethodMetadata methodMetadata = getPojoMetadata().getMethod(messageMetohdName);
						Callback callback = new Callback(methodMetadata, getInstanceManager());
						MessageAbstractProducer messageProducer = new MessageAbstractProducer(getFactory(), resolver,
								messageName, Strategy.push, callback);

						flavorToMessageAbstractProducers.get(messageName).add(messageProducer);

					}

					else if (Strategy.pull.name().equalsIgnoreCase(strategy)) {

						/*
						 * register the Pull Consumer injector to handle all
						 * declared fields
						 */

						@SuppressWarnings({ "rawtypes" })
						MessageAbstractProducer messageAbstractProducer = new MessageAbstractProducer(getFactory(),
								resolver, messageName, Strategy.pull);

						flavorToMessageAbstractProducers.get(messageName).add(messageAbstractProducer);

						getInstanceManager().register(getFactory().getPojoMetadata().getField(messageFieldName),
								messageAbstractProducer);
					}

					break;

				default:
					break;
			}

		}

	}

	/**
	 * This method is called when the component state changed.
	 * 
	 * In the case of hybrid configurations where iPjo components use APAM
	 * resolution, handles synchronization of the component with APAM.
	 * 
	 * @param state
	 *            the new instance state {@link ComponentInstance}
	 */
	@Override
	public void stateChanged(int state) {

		super.stateChanged(state);

		/*
		 * Handle only hybrid appearing instances
		 */
		if (apformLegacyInstance == null)
			return;

		if (state == ComponentInstance.VALID) {

			Apform2Apam.newInstance(apformLegacyInstance.getName(), apformLegacyInstance);
			this.start();
			if (wa_consumerInstance != null && !wa_consumerInstance.isStarted()) {
				wa_consumerInstance.start();
			}

		}

		if (state == ComponentInstance.INVALID) {
			Apform2Apam.vanishInstance(apformLegacyInstance.getName());
			// Désenregistrer le Pullconsumer
			this.stop();
		}

	}

	@Override
	public String toString() {
		return "APAM Message manager for " + getInstanceManager().getInstanceName();
	}

	@Override
	public void stop() {
		if (wa_consumerInstance != null) {
			wa_consumerInstance.dispose();
		}
	}

	@Override
	public void start() {
		if (wa_consumerInstance == null) {
			// Enregistrer le Pullconsumer
			Dictionary configuration = new Hashtable();
			String[] flavors = flavorToMessageAbstractProducers.keySet().toArray(new String[0]);
			configuration.put(WireConstants.WIREADMIN_CONSUMER_FLAVORS, flavors);
			configuration.put(WireConstants.WIREADMIN_CONSUMER_PID, MessageConstants.MESSAGE_CONSUMER_APAM + "."
					+ getInstanceManager().getInstanceName());
			configuration
					.put(MessageConstants.FLAVOR_FOR_ABSTRACT_PRODUCERS_PROPERTY, flavorToMessageAbstractProducers);
			configuration.put("instance.name", wireAdminConsumer.getName() + "_for_"
					+ getInstanceManager().getInstanceName());
			try {
				wa_consumerInstance = wireAdminConsumer.createComponentInstance(configuration);
				System.out.println("wa_con: >>" + wa_consumerInstance);
			} catch (UnacceptableConfiguration e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				this.stop();
			} catch (MissingHandlerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				this.stop();
			} catch (ConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				this.stop();
			}
		}
	}
}
