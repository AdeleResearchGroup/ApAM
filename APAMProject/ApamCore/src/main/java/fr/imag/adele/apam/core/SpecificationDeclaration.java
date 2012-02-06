package fr.imag.adele.apam.core;

import java.util.List;

/**
 * This class represents the declaration of a service provider specification.
 * 
 * This class abstracts over a set of implementations, and declares the provided and
 * required resources common to all these implementations.
 * 
 * It also defines the property scope for all the properties distinguishing the different
 * implementations
 * 
 * @author vega
 *
 */
public class SpecificationDeclaration extends ComponentDeclaration {

    //    private final PropertyScopeImplementation definitions;
    //
    public SpecificationDeclaration(String name) {
        super(name);
        //        definitions = new PropertyScopeImplementation();
    }
    //
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
