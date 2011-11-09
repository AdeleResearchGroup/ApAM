package fr.imag.adele.apam.apformAPI;

import fr.imag.adele.apam.apamAPI.ASMImpl;

//import fr.imag.adele.apam.CompositeImpl;
//import fr.imag.adele.apam.CompositeTypeImpl;
//import fr.imag.adele.apam.apamAPI.ASMImpl;
//import fr.imag.adele.apam.apamAPI.Composite;
//import fr.imag.adele.apam.apamAPI.CompositeType;

public interface Apform {

    public ASMImpl getWaitImplementation(String expectedImpl);

    // use root composite to store unused implems and instances.

//    public static ASMImpl getUnusedImplem(String name);
//    
//    /**
//     * A bundle is under deployment, in which is located the implementation to wait.
//     * The method waits until the implementation arrives and is notified by Apam-iPOJO.
//     * 
//     * @param expectedImpl the symbolic name of that implementation
//     * @return
//     */
//    public static ASMImpl getWaitImplementation(String expectedImpl);

}
