package fr.imag.adele.apam.apformAPI;

import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apformAPI.ApformImplementation;
import fr.imag.adele.apam.apformAPI.ApformInstance;
import fr.imag.adele.apam.apformAPI.ApformSpecification;
import fr.imag.adele.sam.Implementation;

public interface Apform2Apam {

    /**
     * A new instance, represented by object "client" just appeared in the platform.
     */
    public void newInstance(String instanceName, ApformInstance client);

    /**
     * A new implementation, represented by object "client" just appeared in the platform.
     * 
     * @param implemName : the symbolic name.
     * @param client
     */
    public void newImplementation(String implemName, ApformImplementation client);

    /**
     * A new specification, represented by object "client" just appeared in the platform.
     * 
     * @param specName
     * @param client
     */
    public void newSpecification(String specName, ApformSpecification client);

    /**
     * The instance called "instance name" just disappeared from the platform.
     * 
     * @param instanceName
     */
    public void vanishInstance(String instanceName);

    /**
     * * The implementation called "implementation name" just disappeared from the platform.
     * 
     * @param implementationName
     */
    public void vanishImplementation(String implementationName);

    /**
     * 
     * @param specificationName
     */
    public void vanishSpecification(String specificationName);

}
