package fr.imag.adele.apam.core;

import java.util.HashSet;
import java.util.Set;

import fr.imag.adele.apam.core.InjectedField;
import fr.imag.adele.apam.core.InjectedField.InjectedType;
import fr.imag.adele.apam.core.InjectedField.PullConsumer;
import fr.imag.adele.apam.core.InjectedField.PushProducer;
import fr.imag.adele.apam.core.InjectedField.RequiredInterface;
import fr.imag.adele.apam.core.MethodCallback.PushConsumer;
import fr.imag.adele.apam.core.ResourceReference.ResourceType;

/**
 * This class represents the declaration of a java implementation of a service provider
 * @author vega
 *
 */
public class AtomicImplementationDeclaration extends ImplementationDeclaration {

    /**
     * The class name of the object implementing the component
     */
    private final String className;

    private final Set<InjectedField> injectedFields;

    private final Set<MethodCallback> callbacks;

    public AtomicImplementationDeclaration(String name, SpecificationReference specification, String className) {
        super(name, specification);

        assert className != null;

        this.className 		= className;
        injectedFields	= new HashSet<InjectedField>();
        callbacks		= new HashSet<MethodCallback>();
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
    //	public Set<ResourceReference> getResourceInjections(DependencyDeclaration dependency, ResourceType resource) {
    //		Set<ResourceReference> dependencyInjections = new HashSet<RequiredInterface>();
    //		for (InjectedField field : injectedFields) {
    //			if (! (field.getResource().isInterfaceReference()))
    //				continue;
    //			RequiredInterface requiredInterface = (RequiredInterface) field;
    //			
    //			if (! requiredInterface.getDependency().equals(dependency))
    //				continue;
    //			
    //			dependencyInjections.add(requiredInterface);
    //			
    //		}
    //		return dependencyInjections;
    //	}


    /**
     * The list of fields, pertaining to the given dependency, and of the given type.
     * INTERFACE, PUSHPRODUCER, PULLCONSUMER
     */
    public Set<InjectedField> getFieldInjections(DependencyDeclaration dependency, InjectedType type) {
        assert type != InjectedType.PUSHPRODUCER;
        assert dependency != null;

        Set<InjectedField> consumerInjections = new HashSet<InjectedField>();

        for (InjectedField field : injectedFields) {
            if ((field.injectedType == type) && field.getDependency().equals(dependency))
                consumerInjections.add(field);
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
