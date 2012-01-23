package fr.imag.adele.apam.util;

// Constant found in obr repository, specific to OBR. 


public class OBR {

    // Capability
    public static final String CAPABILITY_IMPLEMENTATION = "apam-implementation";
    // Capability
    public static final String CAPABILITY_SPECIFICATION  = "apam-specification";
    // Value boolean
    public static final String CAPABILITY_COMPONENT      = "apam-component";          // iPOJO

    // These attributes cannot be used by users because they would conflict in the OBR.
    public static final String[] reservedAttributes        = { OBR.A_DEFINITION_PREFIX, OBR.A_REQUIRE_INTERFACE,
                                                           OBR.A_REQUIRE_SPECIFICATION, OBR.A_REQUIRE_MESSAGE,
            OBR.A_PROVIDE_INTERFACES,
        OBR.A_PROVIDE_MESSAGES, OBR.A_PROVIDE_SPECIFICATION };

    public static final String A_DEFINITION_PREFIX       = "definition-";
    // String
    public static final String A_REQUIRE_INTERFACE       = "require-interface";
    // String
    public static final String A_REQUIRE_SPECIFICATION   = "require-specification";
    // String
    public static final String A_REQUIRE_MESSAGE         = "require-message";
    // String
    public static final String A_PROVIDE_INTERFACES      = "provide-interfaces";
    // String
    public static final String A_PROVIDE_MESSAGES        = "provide-messages";
    // String
    public static final String A_PROVIDE_SPECIFICATION   = "provide-specification";


}