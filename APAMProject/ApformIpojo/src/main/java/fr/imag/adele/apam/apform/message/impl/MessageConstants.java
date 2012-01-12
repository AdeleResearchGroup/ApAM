package fr.imag.adele.apam.apform.message.impl;

public class MessageConstants {
	
	/**
	 * Configuration element to handle provides APAM messages
	 */
	public final static String MESSAGE_PROVIDES_DECLARATION = "message-provides";

	/**
	 * Configuration element to handle APAM message
	 */
	public final static String MESSAGE_DECLARATION = "message";

	
	/**
	 * Configuration property to specify the user defined messages array
	 */
	public final static String MESSAGES_ARRAY_PROPERTY = "messages";
	
	
	/**
	 * Configuration property to specify the user defined message's name
	 */
	public final static String MESSAGE_NAME_PROPERTY = "name";

	/**
	 * Configuration property to specify the user defined message's strategy
	 */
	public final static String MESSAGE_STRATEGY_PROPERTY = "strategy";

	
	/**
	 * Configuration property to specify the injected field (with abstract consumer)
	 */
	public final static String MESSAGE_PRODUCER_FIELD_PROPERTY = "message-field";
	
	/**
	 * Configuration property to specify the injected field (with abstract consumer)
	 */
	public final static String MESSAGE_PRODUCERS_FIELD_PROPERTY = "message-fields";
	
	
	/**
	 * Configuration property to specify the injected field  (with abstract producer)
	 */
	public final static String MESSAGE_CONSUMER_FIELD_PROPERTY = "field";

	/**
	 * Configuration property to specify the callback method
	 */
	public final static String MESSAGE_METHOD_PROPERTY = "method";

	/**
	 * Configuration property to specify the number of paramters for a method
	 */
	public final static String MESSAGE_METHOD_ARGUMENT_NUMBER_PROPERTY = "parameters";
	
	/**
	 * Registration property as a header for the consumer pid
	 */
	public static final String MESSAGE_CONSUMER_APAM = "apam.message.consumer";

	
	/**
	 * Registration property as a header for the producer pid
	 */
	public static final String MESSAGE_PRODUCER_APAM = "apam.message.producer";
	
	/**
	 * Registration property for the consumer
	 */
	public static final String FLAVOR_FOR_ABSTRACT_PRODUCERS_PROPERTY = "flavor.for.abstract.producers";

}
