package fr.imag.adele.apam.util;

// Constant found in obr repository, specific to OBR. 


public class OBR {

    // Capability
//    public static final String CAPABILITY_IMPLEMENTATION = "apam-implementation";
    // Capability
//    public static final String CAPABILITY_SPECIFICATION  = "apam-specification";
    // Value boolean
    public static final String CAPABILITY_COMPONENT      = "apam-component";          
    public static final String COMPONENT_TYPE      		 = "component-type";          
    public static final String SPECIFICATION      		 = "specification";          
    public static final String IMPLEMENTATION      		 = "implementation";          
    public static final String COMPOSITE_TYPE      		 = "composite-type";          
    public static final String COMPOSITE      		     = "composite";          
    public static final String INSTANCE      		     = "instance";          

    // These attributes cannot be used by users because they would conflict in the OBR.
    public static final String[] reservedAttributes        = { 
    	OBR.A_DEFINITION_PREFIX, 
    	OBR.A_PROVIDE_PREFIX, 
    	OBR.A_REQUIRE_INTERFACE,
        OBR.A_REQUIRE_SPECIFICATION, 
        OBR.A_REQUIRE_MESSAGE,
        OBR.A_PROVIDE_INTERFACES, 
        OBR.A_NAME, 
        OBR.A_PROVIDE_MESSAGES, 
        OBR.A_PROVIDE_SPECIFICATION };

    //All values are  string
    public static final String A_DEFINITION_PREFIX       = "definition-";
    public static final String A_PROVIDE_PREFIX          = "provide-";    
    public static final String A_NAME                    = "name";    
    public static final String A_REQUIRE_INTERFACE       = "require-interface";
    public static final String A_REQUIRE_SPECIFICATION   = "require-specification";
    public static final String A_REQUIRE_MESSAGE         = "require-message";
    public static final String A_PROVIDE_INTERFACES      = "provide-interfaces";
    public static final String A_PROVIDE_MESSAGES        = "provide-messages";
    public static final String A_PROVIDE_SPECIFICATION   = "provide-specification";

}