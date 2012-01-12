package fr.imag.adele.apam.apformipojo.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.apache.felix.ipojo.parser.ParseUtils;
import org.osgi.service.wireadmin.WireConstants;

import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.message.impl.MessageAbstractConsumer;
import fr.imag.adele.apam.apform.message.impl.MessageConstants;
import fr.imag.adele.apam.apform.message.impl.MessageUtils;
import fr.imag.adele.apam.apformipojo.ApformIpojoImplementation;
import fr.imag.adele.apam.apformipojo.ApformIpojoInstance;
import fr.imag.adele.apam.apformipojo.Dependency;
import fr.imag.adele.apam.apformipojo.ImplementationHandler;
import fr.imag.adele.apam.apformipojo.handlers.MessageConsumerHandler.ComponentType;
import fr.imag.adele.apam.apformipojo.legacy.ApformIpojoLegacyInstance;
import fr.imag.adele.apam.message.AbstractConsumer;
import fr.imag.adele.apam.message.Strategy;

public class MessageProviderHandler extends PrimitiveHandler {

	/**
	 * The apform instance for legacy iPojo components
	 */
	private ApformIpojoLegacyInstance apformLegacyInstance;

	// /**
	// * Link a flavor to an abstract consumer field
	// */
	// private Map<String, String> flavorToFieldName;

	/**
	 * List of field interceptor for abstract message consumer
	 */
	private List<MessageAbstractConsumer> abstractMessageConsumers;

	/**
	 * this is the Factory of the WireAdmin Producer witch will act as a Push
	 * Producer Injected by iPOJO
	 */
	private Factory wireAdminProducer;

	/**
	 * Represent the instance of wireAdmin producer
	 */

	private ComponentInstance wa_producerInstance;

	/**
	 * Represent the producer flavors (Registration Property)
	 */
	private String[] messageflavors;

	@Override
	public void initializeComponentFactory(ComponentTypeDescription componentDescriptor, Element componentMetadata)
			throws ConfigurationException {

		// super.initializeComponentFactory(typeDesc, metadata);

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

		String messageNames = componentMetadata.getAttribute(MessageConstants.MESSAGE_DECLARATION);
		String messageStrategy = componentMetadata.getAttribute(MessageConstants.MESSAGE_STRATEGY_PROPERTY);

		/*
		 * Check for alternate syntax for message attribute.
		 */
		if (messageNames == null) {
			String alternateMessageNames = componentMetadata.getAttribute(MessageConstants.MESSAGES_ARRAY_PROPERTY);
			if (alternateMessageNames != null) {
				messageNames = alternateMessageNames;
				componentMetadata.addAttribute(new Attribute(MessageConstants.MESSAGE_DECLARATION, messageNames));
			}
		}
		String[] messagefalvors = null;
		if (messageNames != null) {
			messagefalvors = ParseUtils.parseArrays(messageNames);
		}

		String messageFieldNames = null;
		switch (componentType) {

			case INSTANCE:
				// TODO have to be specified

			case IMPLEMENTATION:
				messageFieldNames = componentMetadata.getAttribute(MessageConstants.MESSAGE_PRODUCER_FIELD_PROPERTY);
				/*
				 * Check for alternate syntax for message attribute.
				 */
				if (messageFieldNames == null) {
					String alternateMessageFieldNames = componentMetadata
							.getAttribute(MessageConstants.MESSAGE_PRODUCERS_FIELD_PROPERTY);
					if (alternateMessageFieldNames != null) {
						messageFieldNames = alternateMessageFieldNames;
						componentMetadata.addAttribute(new Attribute(MessageConstants.MESSAGE_PRODUCER_FIELD_PROPERTY,
								messageFieldNames));
					}
				}

				if (hasInstrumentedCode && messageFieldNames == null && messageNames != null) {
					throw new ConfigurationException("APAM Message " + ImplementationHandler.quote(componentName)
							+ ": " + "a field must be specified");
				}

				if (messageFieldNames != null) {
					/*
					 * Add Strategy to the component description if it's not
					 * declared ONLY "PUSH" Strategy is specified for a Producer
					 */
					if (messageStrategy == null) {
						componentMetadata.addAttribute(new Attribute(MessageConstants.MESSAGE_STRATEGY_PROPERTY,
								Strategy.push.name()));
					}

					String[] messageFields = ParseUtils.parseArrays(messageFieldNames);

					Map<String, String> flavorToFieldName = new HashMap<String, String>();

					for (String messageField : messageFields) {
						/*
						 * Try to check the parameter type of the Field
						 * AbstractConsumer
						 */
						String messageTypeClass = MessageUtils.getFieldParameterType(instrumentedCode,
								AbstractConsumer.class, messageField, componentName);
						if (messageTypeClass != null) {
							flavorToFieldName.put(messageTypeClass, messageField);
						}
					}

					/*
					 * just converting message flavors array to Set
					 */
					Set<String> messageFlavorsFromMetadata = null;
					if (messagefalvors != null) {
						messageFlavorsFromMetadata = new HashSet<String>(Arrays.asList(messagefalvors));
					}

					if (!flavorToFieldName.keySet().isEmpty() && messageFlavorsFromMetadata != null) {
						/*
						 * check declared types that don't have an associated
						 * field
						 */
						Collection<String> typesWithoutField = new HashSet<String>(messageFlavorsFromMetadata);
						typesWithoutField.removeAll(flavorToFieldName.keySet());

						/*
						 * throw exception if there is a declared type without
						 * an associated message-fields but we can have
						 * message-fields without a declared message
						 */
						if (!typesWithoutField.isEmpty()) {
							throw new ConfigurationException("APAM Message "
									+ ImplementationHandler.quote(componentName) + ": "
									+ "those types are declared but are not assossiated to message-fields :  "
									+ Arrays.toString(typesWithoutField.toArray()) + "");
						}

					}

					/*
					 * merge the two list of types
					 */
					Set<String> allTypes = new HashSet<String>();
					allTypes.addAll(messageFlavorsFromMetadata);
					allTypes.addAll(flavorToFieldName.keySet());
					messagefalvors = allTypes.toArray(new String[0]);
					messageNames = Arrays.toString(messagefalvors);
					componentMetadata.addAttribute(new Attribute(MessageConstants.MESSAGE_DECLARATION, messageNames));
					for (String flavor : allTypes) {
						componentMetadata.addAttribute(new Attribute(flavor, flavorToFieldName.get(flavor)));
					}

				}
			case SPECIFICATION:
				/*
				 * is their a way to verify the specification provides?
				 */
				if (messageNames != null) {
					System.out.println("[INFO] - The component " + componentName + " produce the message types "
							+ messageNames);
				}
				break;
		}

	}

	@Override
	public void configure(Element componentMetadata, Dictionary configuration) throws ConfigurationException {
		/*
		 * Add interceptors and callbaks to manage the message production
		 * 
		 * NOTE All validations were already performed when validating the
		 * factory @see initializeComponentFactory, including initializing
		 * unspecified properties with appropriate default values. Here we just
		 * assume metadata is correct.
		 */

		String messageNames = componentMetadata.getAttribute(MessageConstants.MESSAGE_DECLARATION);
		if (messageNames == null) {
			return;
		}
		InstanceManager attachedInstance = getInstanceManager();
		Dependency.Resolver resolver = null;

		if (attachedInstance instanceof ApformIpojoInstance) {
			resolver = (ApformIpojoInstance) attachedInstance;
		} else {
			apformLegacyInstance = new ApformIpojoLegacyInstance(attachedInstance);
			resolver = apformLegacyInstance;
		}

		ComponentType componentType = MessageUtils.getComponentType(componentMetadata);
		messageflavors = ParseUtils.parseArrays(messageNames);

		switch (componentType) {

			case IMPLEMENTATION:
				if (abstractMessageConsumers == null) {
					abstractMessageConsumers = new ArrayList<MessageAbstractConsumer>();
				}
				/*
				 * Add an interceptor to all the fields and maintain a list of
				 * all the field interceptor
				 */
				for (String flavor : messageflavors) {
					String messageFieldName = componentMetadata.getAttribute(flavor);
					MessageAbstractConsumer messageAbstractConsumer = new MessageAbstractConsumer(getFactory(),
							resolver, flavor, Strategy.push, messageFieldName);
					getInstanceManager().register(getFactory().getPojoMetadata().getField(messageFieldName),
							messageAbstractConsumer);

					abstractMessageConsumers.add(messageAbstractConsumer);

				}

				break;
			default:
				break;
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
			if (wa_producerInstance == null) {
				// Enregistrer le Pullconsumer
				this.start();
			}
			if (wa_producerInstance != null && !wa_producerInstance.isStarted()) {
				wa_producerInstance.start();
			}

		}

		if (state == ComponentInstance.INVALID) {
			Apform2Apam.vanishInstance(apformLegacyInstance.getName());
			// Désenregistrer le Pullconsumer
			this.stop();
		}

	}

	@Override
	public void stop() {
		if (wa_producerInstance != null) {
			wa_producerInstance.dispose();
		}

	}

	@Override
	public void start() {
		if (wa_producerInstance == null) {
			// Enregistrer le Pullconsumer

			Dictionary configuration = new Hashtable();
			String flavors = Arrays.toString(messageflavors);
			configuration.put(WireConstants.WIREADMIN_PRODUCER_FLAVORS, flavors);
			configuration.put(WireConstants.WIREADMIN_PRODUCER_PID, MessageConstants.MESSAGE_PRODUCER_APAM + "."
					+ getInstanceManager().getInstanceName());

			configuration.put("instance.name", wireAdminProducer.getName() + "_for_"
					+ getInstanceManager().getInstanceName());
			try {
				wa_producerInstance = wireAdminProducer.createComponentInstance(configuration);
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
