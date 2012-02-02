package fr.imag.adele.apam.core;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.apam.core.InjectedField;
import fr.imag.adele.apam.core.InjectedField.PullConsumer;
import fr.imag.adele.apam.core.InjectedField.PushProducer;
import fr.imag.adele.apam.core.InjectedField.RequiredInterface;
import fr.imag.adele.apam.core.MethodCallback.PushConsumer;

/**
 * This class represents the declaration of a java implementation of a service provider
 * @author vega
 *
 */
public class JavaImplementationDeclaration extends ImplementationDeclaration {

	/**
	 * The class name of the object implementing the component
	 */
	private final String className;

	private final Set<InjectedField> injectedFields;
	
	private final Set<MethodCallback> callbacks;
	
	public JavaImplementationDeclaration(String name, SpecificationReference specification, String className) {
			super(name, specification);
			
			assert className != null;
			
			this.className 		= className;
			this.injectedFields	= new HashSet<InjectedField>();
			this.callbacks		= new HashSet<MethodCallback>();
	}

	/**
	 * The name of the class implementing the service provider
	 */
	public String getClassName() {
		return className;
	}
	
	/**
	 * The list of fields that must be injected in this implementation
	 */
	public Set<InjectedField> getInjectedFields() {
		return injectedFields;
	}
	
	/**
	 * The list of fields that must be injected with an interface reference when the given
	 * dependency is resolved
	 */
	public Set<RequiredInterface> getInterfaceInjections(DependencyDeclaration dependency) {
		Set<RequiredInterface> dependencyInjections = new HashSet<RequiredInterface>();
		for (InjectedField field : injectedFields) {
			if (! (field instanceof RequiredInterface))
				continue;
			RequiredInterface requiredInterface = (RequiredInterface) field;
			
			if (! requiredInterface.getDependency().equals(dependency))
				continue;
			
			dependencyInjections.add(requiredInterface);
			
		}
		return dependencyInjections;
	}


	/**
	 * The list of fields that must be injected into message consumers to pull messages
	 * when the given dependency is resolved
	 */
	public Set<PullConsumer> getConsumerInjections(DependencyDeclaration dependency) {
		Set<PullConsumer> consumerInjections = new HashSet<PullConsumer>();
		for (InjectedField field : injectedFields) {
			if (! (field instanceof PullConsumer))
				continue;
			PullConsumer pullConsumerInjection = (PullConsumer) field;
			
			if (! pullConsumerInjection.getDependency().equals(dependency))
				continue;
			
			consumerInjections.add(pullConsumerInjection);
			
		}
		return consumerInjections;
	}

	
	/**
	 * The list of fields that must be injected into message producers to push messages
	 */
	public Set<PushProducer> getProducerInjections() {
		Set<PushProducer> producerInjections = new HashSet<PushProducer>();
		for (InjectedField field : injectedFields) {
			if (! (field instanceof PushProducer))
				continue;
			
			PushProducer producerInjection = (PushProducer) field;
			producerInjections.add(producerInjection);
			
		}
		
		return producerInjections;
	}
	
	/**
	 * The list of callbacks that must be invoked
	 */
	public Set<MethodCallback> getCallbacks() {
		return callbacks;
	}
	
	/**
	 * The list of callbacks that must be invoked into message consumers to push messages
	 * when the given dependency is resolved
	 */
	public Set<PushConsumer> getConsumerCallback(DependencyDeclaration dependency) {
		Set<PushConsumer> consumerCallbacks = new HashSet<PushConsumer>();
		for (MethodCallback callback : callbacks) {
			if (! (callback instanceof PushConsumer))
				continue;
			
			PushConsumer pullConsumerInjection = (PushConsumer) callback;
			
			if (! pullConsumerInjection.getDependency().equals(dependency))
				continue;
			
			consumerCallbacks.add(pullConsumerInjection);
			
		}
		return consumerCallbacks;
	}


}
