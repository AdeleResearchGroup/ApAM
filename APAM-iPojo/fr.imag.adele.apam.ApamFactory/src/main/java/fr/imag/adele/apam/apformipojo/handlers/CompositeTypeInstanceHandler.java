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
 * This class handle APAM composite contextual properties for instances.
 * 
 * @author vega
 * 
 */
public class CompositeTypeInstanceHandler extends ImplementationHandler {

    /**
     * Configuration element to handle APAM composite contextual implementation properties
     */
    private final static String INSTANCES_DECLARATION              = "instances";

    /**
     * Configuration property to specify internal instances of the composite type
     */
    private final static String PROPERTY_COMPOSITE_INTERNAL        = "internal";

    /**
     * Configuration property to specify local instances of the composite type
     */
    private final static String PROPERTY_COMPOSITE_LOCAL_SCOPE     = "local";

    /**
     * Configuration property to specify the exported instances of the composite type
     */
    private final static String PROPERTY_COMPOSITE_COMPOSITE_SCOPE = "composite";

    /**
     * Configuration property to specify the application scoped instances of the composite type
     */
    private final static String PROPERTY_COMPOSITE_APPLI_SCOPE     = "appli";

    /**
     * Utility method to add a new property to the implementation description
     */
    private static final void addProperty(ApformIpojoImplementation.Description implementationDescription, String name,
            String value, String type) {
        implementationDescription.addProperty(new PropertyDescription(name, type, value, true));
    }

    private static final void addProperty(ApformIpojoImplementation.Description implementationDescription, String name,
            String value) {
        CompositeTypeInstanceHandler.addProperty(implementationDescription, name, value, String.class
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
            throw new ConfigurationException("APAM instances handler can only be used on APAM composite types"
                    + componentMetadata);

        ApformIpojoCompositeType.Description compositeTypeDescription = (ApformIpojoCompositeType.Description) componentDescriptor;
        String compositeTypeName = compositeTypeDescription.getName();

        /*
         * Statically validate the implementation properties and add them to the implementation description.
         */

        Element propertiesDeclarations[] = componentMetadata.getElements(
                CompositeTypeInstanceHandler.INSTANCES_DECLARATION, ImplementationHandler.APAM_NAMESPACE);

        if (!ImplementationHandler.isSingleton(propertiesDeclarations))
            throw new ConfigurationException("APAM composite type properties "
                    + compositeTypeName + ": "
                    + "a single instances declaration must be specified");

        Element propertiesDeclaration = ImplementationHandler.singleton(propertiesDeclarations);

        /*
         * Handle APAM composite type specific properties
         */
        String internal = ImplementationHandler.booleanValue(propertiesDeclaration
                .getAttribute(CompositeTypeInstanceHandler.PROPERTY_COMPOSITE_INTERNAL));
        if (internal != null)
            CompositeTypeInstanceHandler.addProperty(compositeTypeDescription, CST.A_INTERNALINST, internal);

        String local = propertiesDeclaration
                .getAttribute(CompositeTypeInstanceHandler.PROPERTY_COMPOSITE_LOCAL_SCOPE);
        if (local != null)
            CompositeTypeInstanceHandler.addProperty(compositeTypeDescription, CST.A_LOCALSCOPE, local,
                    "String[]");

        String composite = propertiesDeclaration
                .getAttribute(CompositeTypeInstanceHandler.PROPERTY_COMPOSITE_COMPOSITE_SCOPE);
        if (composite != null)
            CompositeTypeInstanceHandler.addProperty(compositeTypeDescription, CST.A_COMPOSITESCOPE,
                    composite, "String[]");

        String appli = propertiesDeclaration
                .getAttribute(CompositeTypeInstanceHandler.PROPERTY_COMPOSITE_APPLI_SCOPE);
        if (appli != null)
            CompositeTypeInstanceHandler.addProperty(compositeTypeDescription, CST.A_APPLISCOPE, appli,
                    "String[]");

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
