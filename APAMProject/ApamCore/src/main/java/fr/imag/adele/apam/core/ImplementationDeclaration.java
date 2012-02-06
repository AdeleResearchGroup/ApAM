package fr.imag.adele.apam.core;

import java.util.List;

/**
 * This class represents all the common declarations for an implementation of a service 
 * provider.
 * 
 * @author vega
 *
 */
public abstract class ImplementationDeclaration extends ComponentDeclaration /*implements PropertyScope*/{

    /**
     * The specification implemented by this implementation
     */
    private final SpecificationReference specification;

    /**
     * The definition of properties used to distinguish instances of this implementation 
     */
    private ImplementationDeclaration definitions;

    protected ImplementationDeclaration(String name, SpecificationReference specification) {
        super(name);

        assert specification != null;

        this.specification = specification;
    }


    /**
     * Get the specification implemented by this implementation
     * @return
     */
    public SpecificationReference getSpecification() {
        return specification;
    }


    //    @Override
    //    public List<PropertyDefinition> getPropertyDefinitions() {
    //        return definitions.getPropertyDefinitions();
    //    }
    //
    //    @Override
    //    public boolean isDefined(String propertyName) {
    //        return definitions.isDefined(propertyName);
    //    }
    //
    //    @Override
    //    public PropertyDefinition getPropertyDefinition(String propertyName) {
    //        return definitions.getPropertyDefinition(propertyName);
    //    }


}
