package fr.imag.adele.apam.apformipojo.handlers;

import java.util.Dictionary;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.metadata.Element;

import fr.imag.adele.apam.apamImpl.CST;
import fr.imag.adele.apam.apformipojo.ApformIpojoImplementation;
import fr.imag.adele.apam.apformipojo.ApformIpojoCompositeType;
import fr.imag.adele.apam.apformipojo.ImplementationHandler;

/**
 * This class handle APAM composite contextual properties for implementations.
 * 
 * @author vega
 * 
 */
public class CompositeTypeImplementationHandler extends ImplementationHandler {

    /**
     * Configuration element to handle APAM composite contextual implementation properties
     */
    private final static String IMPLEMENTATIONS_DECLARATION                = "implementations";

    /**
     * Configuration property to specify internal implementations of the composite type
     */
    private final static String PROPERTY_COMPOSITE_INTERNAL_IMPLEMENTATION = "internal";

    /**
     * Configuration property to specify local implementations of the composite type
     */
    private final static String PROPERTY_COMPOSITE_LOCAL_VISIBILITY        = "local";

    /**
     * Configuration property to specify the exported implementations of the composite type
     */
    private final static String PROPERTY_COMPOSITE_COMPOSITE_VISIBILITY    = "composite";

    /**
     * Utility method to add a new property to the implementation description
     */
    private static final void addProperty(ApformIpojoImplementation.Description implementationDescription, String name,
            String value, String type) {
        implementationDescription.addProperty(new PropertyDescription(name, type, value, true));
    }

    private static final void addProperty(ApformIpojoImplementation.Description implementationDescription, String name,
            String value) {
        CompositeTypeImplementationHandler.addProperty(implementationDescription, name, value, String.class
                .getName());
    }

    /**
     * 
     * Initializes the composite description to include APAM contextual properties.
     * 
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.
     *      ComponentTypeDescription, org.apache.felix.ipojo.metadata.Element)
     */
    @Override
    public void initializeComponentFactory(ComponentTypeDescription componentDescriptor, Element componentMetadata)
            throws ConfigurationException {

        /*
         * This handler works only for APAM native composite types
         */
        if (!(componentDescriptor instanceof ApformIpojoCompositeType.Description))
            throw new ConfigurationException("APAM implementations handler can only be used on APAM composite types"
                    + componentMetadata);

        ApformIpojoCompositeType.Description compositeTypeDescription = (ApformIpojoCompositeType.Description) componentDescriptor;
        String compositeTypeName = compositeTypeDescription.getName();

        /*
         * Statically validate the implementation properties and add them to the implementation description.
         */

        Element propertiesDeclarations[] = componentMetadata.getElements(
                CompositeTypeImplementationHandler.IMPLEMENTATIONS_DECLARATION,
                ImplementationHandler.APAM_NAMESPACE);

        if (!ImplementationHandler.isSingleton(propertiesDeclarations))
            throw new ConfigurationException("APAM composite type properties "
                    + compositeTypeName + ": "
                    + "a single implementations declaration must be specified");

        Element propertiesDeclaration = ImplementationHandler.singleton(propertiesDeclarations);

        /*
         * Handle APAM composite type specific properties
         */
        String internal = ImplementationHandler.booleanValue(propertiesDeclaration
                .getAttribute(CompositeTypeImplementationHandler.PROPERTY_COMPOSITE_INTERNAL_IMPLEMENTATION));
        if (internal != null)
            CompositeTypeImplementationHandler.addProperty(compositeTypeDescription, CST.A_INTERNALIMPL,
                    internal);

        String local = propertiesDeclaration
                .getAttribute(CompositeTypeImplementationHandler.PROPERTY_COMPOSITE_LOCAL_VISIBILITY);
        if (local != null)
            CompositeTypeImplementationHandler.addProperty(compositeTypeDescription, CST.A_LOCALVISIBLE,
                    local, "String[]");

        String composite = propertiesDeclaration
                .getAttribute(CompositeTypeImplementationHandler.PROPERTY_COMPOSITE_COMPOSITE_VISIBILITY);
        if (composite != null)
            CompositeTypeImplementationHandler.addProperty(compositeTypeDescription, CST.A_COMPOSITEVISIBLE,
                    composite, "String[]");

    }

    /**
     * APAM properties describe the implementation, they do not apply to instances, so we do not configure them.
     * 
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void configure(Element componentMetadata, Dictionary configuration) throws ConfigurationException {
    }

    @Override
    public String toString() {
        return "APPAM properties manager for " + getInstanceManager().getInstanceName();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

}
