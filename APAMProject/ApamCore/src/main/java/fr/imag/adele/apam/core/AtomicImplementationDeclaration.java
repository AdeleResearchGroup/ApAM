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
     * An interface giving access to instrumentation data associated with this implementation
     */
    public interface Instrumentation {
    	
    	/**
    	 * The name of the associated java class
    	 */
    	public String getClassName();
    	
    	/**
    	 * The type of the specified java field
    	 */
    	public ResourceReference getType(String field);
    	
    	/**
    	 * The cardinality of the specified java field
    	 */
    	public boolean isCollection(String field);
    }

    /**
     * A reference to the instrumentation data associated with this implementation
     */
    private final Instrumentation instrumentation;

    /**
     * The list of injected fields declared for this implementation
     */
    private final Set<DependencyInjection> injectedFields;


    public AtomicImplementationDeclaration(String name, SpecificationReference specification, Instrumentation instrumentation) {
        super(name, specification);

        assert instrumentation != null;

        this.instrumentation 	= instrumentation;
        this.injectedFields		= new HashSet<DependencyInjection>();
    }

	/**
	 * A reference to an atomic implementation
	 */
    private static class Reference extends ImplementationReference<AtomicImplementationDeclaration> {

		public Reference(String name) {
			super(name);
		}

	}

    /**
     * Generates the reference to this implementation
     */
    @Override
    protected ImplementationReference<AtomicImplementationDeclaration> generateReference() {
    	return new Reference(getName());
    }
    
    /**
     * The instrumentation data associated with this implementation
     */
    public Instrumentation getInstrumentation() {
    	return instrumentation;
    }
    
    /**
     * The name of the class implementing the service provider
     */
    public String getClassName() {
        return instrumentation.getClassName();
    }

    /**
     * The list of fields that must be injected in this implementation
     */
    public Set<DependencyInjection> getDependencyInjections() {
        return injectedFields;
    }


    @Override
    public String toString() {
        String ret = super.toString();
        if (injectedFields.size() != 0) {
            ret += "\n    Injected fields : ";
            for (DependencyInjection injection : injectedFields) {
                ret += " " + injection.getFieldName();
            }
        }
        ret += "\n   Class Name: " + getClassName();
        return ret;
    }


}
