package fr.imag.adele.apam.apform;

import java.util.Set;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.apamImpl.CompositeImpl;
import fr.imag.adele.apam.apamImpl.CompositeTypeImpl;
import fr.imag.adele.apam.apamImpl.ImplementationImpl;
import fr.imag.adele.apam.apamImpl.InstanceImpl;

public class Apform {

    private static final CompositeType              rootType              = CompositeTypeImpl.getRootCompositeType();
    private static final Composite                  rootInst              = CompositeImpl.getRootAllComposites();

    private static Set<Implementation>              unusedImplems         = CompositeTypeImpl.getRootCompositeType().getImpls();
    private static Set<Instance>                    unusedInsts           = CompositeImpl.getRootAllComposites()
    .getContainInsts();

    public static Implementation getUnusedImplem(String name) {
        Implementation impl = CST.ImplBroker.getImpl(name);
        if (impl == null)
            return null;
        return (Apform.unusedImplems.contains(impl)) ? impl : null;
    }

    public static Instance getUnusedInst(String name) {
        Instance inst = CST.InstBroker.getInst(name);
        if (inst == null)
            return null;
        return (Apform.unusedInsts.contains(inst)) ? inst : null;
    }

    /**
     * The implementation that was unused so far, is now logicaly deployed.
     * Remove it from the unUsed compositeType.
     * 
     * @param impl
     */
    public static void setUsedImpl(Implementation impl) {
        if (impl.isUsed())
            return;
        ((ImplementationImpl) impl).setUsed(true);
        ((CompositeTypeImpl) Apform.rootType).removeImpl(impl);
        if (impl instanceof CompositeType) { // it is a composite
            ((CompositeTypeImpl) Apform.rootType).removeEmbedded((CompositeType) impl);
        }
    }

    /**
     * The implementation that was unused so far, is now logically deployed.
     * Remove it from the unUsed compositeType.
     * 
     * @param impl
     */
    public static void setUsedInst(Instance inst) {
        if (inst.isUsed())
            return;
        ((CompositeImpl) Apform.rootInst).removeInst(inst);
        ((InstanceImpl) inst).setUsed(true);
        if (inst instanceof Composite) { // it is a composite. Should never happen ?
            ((CompositeImpl) Apform.rootInst).removeSon((Composite) inst);
        }
    }

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
            System.out.println("wake up but imlementation is not present " + expectedImpl);

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
            System.out.println("wake up but specification is not present " + expected);

        return spec;
    }

}
