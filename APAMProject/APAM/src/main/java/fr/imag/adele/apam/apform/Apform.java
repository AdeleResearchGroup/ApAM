package fr.imag.adele.apam.apform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Specification;

public class Apform {

	private static Logger logger = LoggerFactory.getLogger(Apform.class);
	

    /**
     * A bundle is under deployment, in which is located the implementation to wait.
     * The method waits until the implementation arrives and is notified by Apam-iPOJO.
     * 
     * @param expectedImpl the symbolic name of that implementation
     * @return
     */
    public static Implementation getWaitImplementation(String expectedImpl) {
        if (expectedImpl == null)
            return null;
        // if allready here
        Implementation impl = CST.ImplBroker.getImpl(expectedImpl);
        if (impl != null)
            return impl;

        Apform2Apam.waitForDeployedImplementation(expectedImpl);
        // The expected impl arrived. It is in unUsed.
        impl = CST.ImplBroker.getImpl(expectedImpl);
        if (impl == null) // should never occur
            logger.debug("wake up but imlementation is not present " + expectedImpl);

        return impl;
    }

    /**
     * A bundle is under deployment, in which is located the implementation to wait.
     * The method waits until the implementation arrives and is notified by Apam-iPOJO.
     * 
     * @param expected the symbolic name of that implementation
     * @return
     */
    public static Specification getWaitSpecification(String expected) {
        if (expected == null)
            return null;
        // if allready here
        Specification spec = CST.SpecBroker.getSpec(expected);
        if (spec != null)
            return spec;

        Apform2Apam.waitForDeployedSpecification(expected);
        // The expected impl arrived. It is in unUsed.
        spec = CST.SpecBroker.getSpec(expected);
        if (spec == null) // should never occur
            logger.debug("wake up but specification is not present " + expected);

        return spec;
    }

}
