package fr.imag.adele.apam;

import fr.imag.adele.apam.impl.APAMImpl;
import fr.imag.adele.apam.impl.ApamResolverImpl;
import fr.imag.adele.apam.impl.ImplementationBrokerImpl;
import fr.imag.adele.apam.impl.InstanceBrokerImpl;
import fr.imag.adele.apam.impl.SpecificationBrokerImpl;

public class CST {

    // Constants "A_" means attribute name; "V_" means attribute value

    public static String[]             predefAttributes      = {
        CST.A_BORROWIMPLEM, CST.A_LOCALIMPLEM,
        CST.A_FRIENDIMPLEM, CST.A_BORROWINSTANCE,
        CST.A_LOCALINSTANCE, CST.A_FRIENDINSTANCE, CST.A_APPLIINSTANCE,
        CST.A_INSTANTIABLE,
        CST.A_MULTIPLE, CST.A_REMOTABLE, CST.A_SHARED
    };

    // Borrow / lend properties of composites.
    // Expression | true | false
    public static final String         A_BORROWIMPLEM        = "borrowImplementation";
    public static final String         A_LOCALIMPLEM         = "localImplementation";
    public static final String         A_FRIENDIMPLEM        = "friendImplementation";

    public static final String         A_BORROWINSTANCE      = "borrowInstance";
    public static final String         A_LOCALINSTANCE       = "localInstance";
    public static final String         A_FRIENDINSTANCE      = "friendInstance";
    public static final String         A_APPLIINSTANCE       = "appliInstance";

    // Value boolean
    public static final String         A_INSTANTIABLE        = "instantiable";
    // multiple on a group head indicates if more than one resolution is allowed in the scope
    public static final String         A_MULTIPLE            = "multiple";
    // remotable indicates that the instance can be used from a remote machine
    public static final String         A_REMOTABLE           = "remotable";
    // shared on an implementation indicates if its instances can have more than one incoming wire
    public static final String         A_SHARED              = "shared";

    //APAM ROOT COMPOSITE
    public static final String ROOT_COMPOSITE_TYPE = "root.composite.type";
    

    //Constant used by OBR
    // Capability
    public static final String CAPABILITY_COMPONENT      = "apam-component";          
    public static final String COMPONENT_TYPE      		 = "component-type";          
    public static final String SPECIFICATION      		 = "specification";          
    public static final String IMPLEMENTATION      		 = "implementation";          
    public static final String COMPOSITE_TYPE      		 = "composite-type";          
    public static final String COMPOSITE      		     = "composite";          
    public static final String INSTANCE      		     = "instance";          
    //All values are  string
    public static final String A_DEFINITION_PREFIX       = "definition-";
    public static final String A_PROVIDE_PREFIX          = "provide-";    
    public static final String A_REQUIRE_PREFIX          = "require-";    
    public static final String A_REQUIRE_INTERFACE       = "require-interface";
    public static final String A_REQUIRE_SPECIFICATION   = "require-specification";
    public static final String A_REQUIRE_MESSAGE         = "require-message";
    public static final String A_PROVIDE_INTERFACES      = "provide-interfaces";
    public static final String A_PROVIDE_MESSAGES        = "provide-messages";
    public static final String A_PROVIDE_SPECIFICATION   = "provide-specification";

    
    public static final String         A_NAME            = "name";
    public static final String         A_VERSION         = "version";
    public static final String         A_SPECNAME        = "spec-name";
    public static final String         A_IMPLNAME        = "impl-name";
    public static final String         A_INSTNAME        = "inst-name";
    public static final String         A_INTERFACE       = "interface";
    public static final String         A_MESSAGE         = "message";
    public static final String         A_MAIN_COMPONENT  = "apam-main-component";
    public static final String         A_MAIN_INSTANCE   = "apam-main-instance";
    public static final String         A_COMPOSITE       = "apam-composite";
    public static final String         A_COMPOSITETYPE   = "apam-compositetype";

    // These prefix cannot be used by users because they would conflict in the OBR.
    public static final String[] reservedPrefix        = { 
    	CST.A_DEFINITION_PREFIX, 
    	CST.A_PROVIDE_PREFIX, 
    	CST.A_REQUIRE_PREFIX 
    };

    public static final String [] notInheritedAttribute = {
    	A_NAME, COMPONENT_TYPE, A_VERSION
    };
    
    // Attributes that cannot be changed nor set by users
    public static final String[]       finalAttributes       = {
        CST.A_SPECNAME, 
        CST.A_IMPLNAME, 
        CST.A_INSTNAME,
        CST.A_COMPOSITE, 
        CST.A_MAIN_COMPONENT, 
        CST.A_MAIN_INSTANCE,
        CST.A_INTERFACE, 
        CST.A_MESSAGE,
    	CST.A_REQUIRE_INTERFACE,
        CST.A_REQUIRE_SPECIFICATION, 
        CST.A_REQUIRE_MESSAGE,
        CST.A_PROVIDE_INTERFACES, 
        CST.A_NAME, 
        CST.A_PROVIDE_MESSAGES, 
        CST.A_PROVIDE_SPECIFICATION };

    public static final String         V_TRUE            = "true";
    public static final String         V_FALSE           = "false";

    // Managers
    public static final String         APAMMAN           = "APAMMAN";
    public static final String         CONFMAN           = "CONFMAN";
    public static final String         DYNAMAN           = "DYNAMAN";
    public static final String         DISTRIMAN         = "DISTRIMAN";
    public static final String         OBRMAN            = "OBRMAN";


    // The entry point in the ASM : its brokers
    public static SpecificationBroker  SpecBroker        = null;
    public static ImplementationBroker ImplBroker        = null;
    public static InstanceBroker       InstBroker        = null;
    public static ApamResolver         apamResolver      = null;

    // the Apam entry point.
    public static APAMImpl             apam              = null;

    public CST(APAMImpl theApam) {
        CST.SpecBroker = new SpecificationBrokerImpl();
        CST.ImplBroker = new ImplementationBrokerImpl();
        CST.InstBroker = new InstanceBrokerImpl();
        CST.apam = theApam;
        CST.apamResolver = new ApamResolverImpl(theApam);
    }

}
