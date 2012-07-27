package fr.imag.adele.apam.test.dependency;

import java.util.List;

import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ApamComponent;

public class DependencyTest implements DependencyTestProvidedInterface, ApamComponent {

    private List<DependencyTestInterface> dependencies;

    public void apamStart(ASMInst apamInstance) {
        System.out.println("Starting DependencyTest " + apamInstance);
        for (DependencyTestInterface dependency : dependencies) {
            dependency.hello();
        }
        System.out.println("Ending DependencyTest");
    }

    public void apamStop() {

    }

    public void apamRelease() {

    }
}
