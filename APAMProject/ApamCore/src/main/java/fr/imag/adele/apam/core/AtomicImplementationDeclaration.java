package fr.imag.adele.apam.core;

import java.util.HashSet;
import java.util.Set;

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

    private final Set<DependencyInjection> injectedFields;

    private final Set<MethodCallback> callbacks;

    public AtomicImplementationDeclaration(String name, SpecificationReference specification, String className) {
        super(name, specification);

        assert className != null;

        this.className 	= className;
        injectedFields	= new HashSet<DependencyInjection>();
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
    public Set<DependencyInjection> getDependencyInjections() {
        return injectedFields;
    }

 
    /**
     * The list of callbacks that must be invoked
     */
    public Set<MethodCallback> getCallbacks() {
        return callbacks;
    }



}
