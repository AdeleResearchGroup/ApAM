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
        CST.A_MULTIPLE, CST.A_REMOTABLE, CST.A_SHARED,
        CST.A_MODELS
    };

    // Borrow / lend properties of composites.
    // Expression | true | false
    public static final String         A_BORROWIMPLEM        = "borrowimplementation";
    public static final String         A_LOCALIMPLEM         = "localimplementation";
    public static final String         A_FRIENDIMPLEM        = "friendimplementation";

    public static final String         A_BORROWINSTANCE      = "borrowinstance";
    public static final String         A_LOCALINSTANCE       = "localinstance";
    public static final String         A_FRIENDINSTANCE      = "friendinstance";
    public static final String         A_APPLIINSTANCE       = "appliInstance";

    // Value boolean
    public static final String         A_INSTANTIABLE        = "instantiable";
    // multiple on a group head indicates if more than one resolution is allowed in the scope
    public static final String         A_MULTIPLE            = "multiple";
    // remotable indicates that the instance can be used from a remote machine
    public static final String         A_REMOTABLE           = "remotable";
    // shared on an implementation indicates if its instances can have more than one incoming wire
    public static final String         A_SHARED              = "shared";
    // List<ManagerModel>
    public static final String         A_MODELS              = "apam-models";

    // Attributes that cannot be changed by users
    public static final String[]       finalAttributes       = {
        CST.A_SPECNAME, CST.A_IMPLNAME, CST.A_INSTNAME,
        CST.A_COMPOSITE, CST.A_MAIN_COMPONENT,
        CST.A_INTERFACE, CST.A_MESSAGE
    };

    public static final String         A_SPECNAME        = "spec-name";
    public static final String         A_IMPLNAME        = "impl-name";
    public static final String         A_INSTNAME        = "inst-name";
    public static final String         A_INTERFACE       = "interface";
    public static final String         A_MESSAGE         = "message";
    public static final String         A_MAIN_COMPONENT  = "apam-main-component";
    public static final String         A_COMPOSITE       = "apam-composite";
    public static final String         A_COMPOSITETYPE   = "apam-compositetype";

    public static final String         V_TRUE            = "true";
    public static final String         V_FALSE           = "false";

    // Managers
    public static final String         APAMMAN           = "APAMMAN";
    public static final String         CONFMAN           = "CONFMAN";
    public static final String         DYNAMAN           = "DYNAMAN";
    public static final String         DISTRIMAN         = "DISTRIMAN";
    public static final String         OBRMAN            = "OBRMAN";

    public static final String         ROOTCOMPOSITETYPE = "rootCompositeType";
    public static final String         ROOTCOMPOSITE	 = "rootComposite";
    
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
