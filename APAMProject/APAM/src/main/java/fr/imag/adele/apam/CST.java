package fr.imag.adele.apam;

import fr.imag.adele.apam.impl.APAMImpl;
import fr.imag.adele.apam.impl.ApamResolverImpl;
import fr.imag.adele.apam.impl.ImplementationBrokerImpl;
import fr.imag.adele.apam.impl.InstanceBrokerImpl;
import fr.imag.adele.apam.impl.SpecificationBrokerImpl;

public class CST {


//	public static String[] predefAttributes = { 
//			CST.INSTANTIABLE, CST.MULTIPLE, CST.REMOTABLE, 
//			CST.SHARED, CST.EXCLUSIVE, CST.SINGLETON };
		
//		CST.A_BORROWIMPLEM,
//			CST.A_LOCALIMPLEM, CST.A_FRIENDIMPLEM, CST.A_BORROWINSTANCE,
//			CST.A_LOCALINSTANCE, CST.A_FRIENDINSTANCE, CST.A_APPLIINSTANCE,

    // Borrow / lend properties of composites.
    // Expression | true | false
//	public static final String A_BORROWIMPLEM = "borrowImplementation";
//	public static final String A_LOCALIMPLEM = "localImplementation";
//	public static final String A_FRIENDIMPLEM = "friendImplementation";
//
//	public static final String A_BORROWINSTANCE = "borrowInstance";
//	public static final String A_LOCALINSTANCE = "localInstance";
//	public static final String A_FRIENDINSTANCE = "friendInstance";
//	public static final String A_APPLIINSTANCE = "appliInstance";


	// Values are boolean
	public static final String INSTANTIABLE = "instantiable";
	// multiple on a dependency indicates more than one outgoing wire
	public static final String MULTIPLE = "multiple";
	// remotable indicates that the instance can be used from a remote machine
	public static final String REMOTABLE = "remotable";
	// shared indicates if its instances can have more than one incoming wire
	public static final String SHARED = "shared";
	//exclusive == shared=false and singleton=true
	public static final String EXCLUSIVE = "exclusive" ; 
	//a single instance per implementation
	public static final String SINGLETON = "singleton" ;

	
//	// APAM ROOT COMPOSITE
	public static final String ROOT_COMPOSITE_TYPE = "root";

	// Constant used by OBR
	// Capability
	public static final String         MAVEN                 = "maven";
    public static final String         GROUP_ID              = "groupId";
    public static final String         ARTIFACT_ID           = "artifactId";
    
	public static final String CAPABILITY_COMPONENT = "apam-component";
	public static final String COMPONENT_TYPE 		= "component-type";
	public static final String SPECIFICATION 		= "specification";
	public static final String IMPLEMENTATION 		= "implementation";
//	public static final String COMPOSITE_TYPE 		= "composite-type";
//	public static final String COMPOSITE 			= "composite";
	public static final String INSTANCE 			= "instance";
	public static final String DEFINITION_PREFIX 	= "definition-";
	public static final String PROVIDE_PREFIX 		= "provide-";
	public static final String REQUIRE_PREFIX 		= "require-";
	public static final String REQUIRE_INTERFACE 	= "require-interface";
	public static final String REQUIRE_SPECIFICATION= "require-specification";
	public static final String REQUIRE_MESSAGE 		= "require-message";
	public static final String PROVIDE_INTERFACES 	= "provide-interfaces";
	public static final String PROVIDE_MESSAGES 	= "provide-messages";
	public static final String PROVIDE_SPECIFICATION= "provide-specification";

	public static final String NAME 				= "name";
	public static final String VERSION 				= "version";
	public static final String SPECNAME 			= "spec-name";
	public static final String IMPLNAME 			= "impl-name";
	public static final String INSTNAME 			= "inst-name";
	public static final String INTERFACE 			= "interface";
	public static final String MESSAGE 				= "message";
	public static final String APAM_MAIN_COMPONENT 	= "apam-main-component";
	public static final String APAM_MAIN_INSTANCE 	= "apam-main-instance";
	public static final String APAM_COMPOSITE 		= "apam-composite";
	public static final String APAM_COMPOSITETYPE 	= "apam-compositetype";

	// These prefix cannot be used by users because they would conflict in the
	// OBR.
	public static final String[] reservedPrefix = { CST.DEFINITION_PREFIX,
			CST.PROVIDE_PREFIX, CST.REQUIRE_PREFIX };

	public static final String[] notInheritedAttribute = {NAME,
			COMPONENT_TYPE, VERSION, APAM_COMPOSITETYPE };

	// Attributes that cannot be changed nor set by users
	public static final String[] finalAttributes = { 
			CST.NAME, VERSION, CST.SPECNAME, 
			CST.IMPLNAME, CST.INSTNAME, CST.MESSAGE, CST.APAM_COMPOSITE, 
			CST.APAM_COMPOSITETYPE, CST.APAM_MAIN_COMPONENT, CST.APAM_MAIN_INSTANCE, 
			CST.INTERFACE, CST.REQUIRE_INTERFACE, CST.REQUIRE_SPECIFICATION,
			CST.REQUIRE_MESSAGE, CST.PROVIDE_INTERFACES, 
			CST.PROVIDE_MESSAGES, CST.PROVIDE_SPECIFICATION,
			CST.INSTANTIABLE, CST.MULTIPLE, CST.REMOTABLE, 
			CST.SHARED, CST.EXCLUSIVE, CST.SINGLETON};

	public static final String V_TRUE = "true";
	public static final String V_FALSE = "false";

	// Managers
	public static final String APAMMAN = "APAMMAN";
	public static final String CONFMAN = "CONFMAN";
	public static final String DYNAMAN = "DYNAMAN";
	public static final String DISTRIMAN = "DISTRIMAN";
	public static final String OBRMAN = "OBRMAN";

	// The entry point in the ASM : its brokers
	public static SpecificationBroker SpecBroker = null;
	public static ImplementationBroker ImplBroker = null;
	public static InstanceBroker InstBroker = null;
	public static ApamResolver apamResolver = null;

	// the Apam entry point.
	public static APAMImpl apam = null;

	public CST(APAMImpl theApam) {
		CST.SpecBroker = new SpecificationBrokerImpl();
		CST.ImplBroker = new ImplementationBrokerImpl();
		CST.InstBroker = new InstanceBrokerImpl();
		CST.apam = theApam;
		CST.apamResolver = new ApamResolverImpl(theApam);
	}

}
