package fr.imag.adele.apam.command;

/**
 * Copyright Universite Joseph Fourier (www.ujf-grenoble.fr)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.service.command.Descriptor;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.apamAPI.Composite;

/**
 * 
 * 
 * @author Jacky
 */
@Component(public_factory = false, immediate = true, name = "apam.shell")
@Provides(specifications = ApamCommand.class)
public class ApamCommand {

    /**
     * Defines the command scope (apam).
     */
    @ServiceProperty(name = "osgi.command.scope", value = "apam")
    String   m_scope;

    /**
     * Defines the functions (commands).
     */
    @ServiceProperty(name = "osgi.command.function", value = "{}")
    String[] m_function = new String[] { "specs", "implems", "insts", "spec", "implem", "inst", "applis", "dump",
                        "appli", "compos", "compo", "wire" };

    // ipojo injected
    @Requires
    Apam     apam;

    /**
     * Specifications.
     */

    @Descriptor("Display all the Apam specifications")
    public void specs() {
        Set<ASMSpec> specifications = CST.ASMSpecBroker.getSpecs();
        for (ASMSpec specification : specifications) {
            System.out.println("ASMSpec : " + specification);
        }
    }

    /**
     * Implementations.
     */
    @Descriptor("Display of all the implementations of the local machine")
    public void implems() {
        Set<ASMImpl> implementations = CST.ASMImplBroker.getImpls();
        for (ASMImpl implementation : implementations) {
            System.out.println("ASMImpl : " + implementation);
        }
    }

    /**
     * Instances.
     */
    @Descriptor("Display of all the instances of the local machine")
    public void insts() {
        Set<ASMInst> instances = CST.ASMInstBroker.getInsts();
        for (ASMInst instance : instances) {
            System.out.println("ASMInst : " + instance);
        }
    }

    /**
     * ASMSpec.
     * 
     * @param specificationName the specification name
     */
    @Descriptor("Display informations about the target specification")
    public void spec(@Descriptor("target specification") String specificationName) {
        Set<ASMSpec> specifications = CST.ASMSpecBroker.getSpecs();
        for (ASMSpec specification : specifications) {
            if ((specification.getASMName() != null)
                    && (specification.getASMName().equalsIgnoreCase(specificationName))) {
                printSpecification("", specification);
                testImplementations("   ", specification.getImpls());
                break;
            }
            if ((specification.getSAMName() != null)
                    && (specification.getSAMName().equalsIgnoreCase(specificationName))) {
                printSpecification("", specification);
                testImplementations("   ", specification.getImpls());
                break;
            }
        }
    }

    /**
     * ASMImpl.
     * 
     * @param implementationName the implementation name
     */
    @Descriptor("Display informations about the target implemetation")
    public void implem(@Descriptor("target implementation") String implementationName) {
        ASMImpl implementation = CST.ASMImplBroker.getImpl(implementationName);
        if (implementation == null) {
            System.out.println("No such implementation : " + implementationName);
            return;
        }
        printImplementation("", implementation);
        // testInstances("   ", implementation.getInsts());
    }

    /**
     * ASMInst.
     * 
     * @param implementationName the implementation name
     */
    @Descriptor("Display informations about the target instance")
    public void inst(@Descriptor("target implementation") String instanceName) {
        try {
            ASMInst instance = CST.ASMInstBroker.getInst(instanceName);
            if (instance == null) {
                System.out.println("No such instance : " + instanceName);
            } else
                printInstance("", instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Descriptor("Display all the Apam applications")
    public void applis() {
        Set<Application> applis = apam.getApplications();
        for (Application appli : applis) {
            System.out.println("Application : " + appli.getName());
        }
    }

    @Descriptor("Display informations about the target application")
    public void appli(@Descriptor("target application") String appliName) {
        Application appli = apam.getApplication(appliName);
        if (appli == null) {
            System.out.println("No such application : " + appliName);
            return;
        }
        System.out.println("Application " + appli);
        for (Composite compo : appli.getComposites()) {
            System.out.println("Composite : " + compo.getName());
        }
    }

    @Descriptor("Display the full Apam state model")
    public void dump() {
        dumpApam();
    }

    @Descriptor("Display the state model of the target application")
    public void state(@Descriptor("target application") String appliName) {
        dumpAppli(appliName);
    }

    @Descriptor("Display all the Apam composites")
    public void compos() {
        Set<Application> applis = apam.getApplications();
        for (Application appli : applis) {
            System.out.println("Application : " + appli.getName());
            for (Composite compo : appli.getComposites()) {
                System.out.println("      composite : " + compo.getName());
            }
        }
    }

    @Descriptor("Display all the Apam composites")
    public void compo(@Descriptor("target composite") String compoName) {
        Set<Application> applis = apam.getApplications();
        Composite compo = null;
        for (Application appli : applis) {
            for (Composite acompo : appli.getComposites()) {
                if (compoName.equals(acompo.getName())) {
                    compo = acompo;
                    break;
                }
            }
            if (compo != null)
                break;
        }
        if (compo == null) {
            System.out.println("No such composite : " + compoName);
            return;
        }
        System.out.println("Name : " + compoName + ";  Application " + compo.getApplication().getName());
        System.out.print("  Depends on composites : ");
        for (Composite comDep : compo.getDepend()) {
            System.out.print(comDep.getName() + " ");
        }
        System.out.println("");
        System.out.print("  Contains Specifications: ");
        for (ASMSpec spec : compo.getSpecs()) {
            System.out.print(spec + " ");
        }
        System.out.println("");
        System.out.print("  Contains Implementations: ");
        for (ASMImpl impl : compo.getImpls()) {
            System.out.print(impl + " ");
        }
        System.out.println("");
        System.out.print("  Contains Instances : ");
        for (ASMInst inst : compo.getInsts()) {
            System.out.print(inst + " ");
        }
        System.out.println("");
    }

    @Descriptor("Display all the dependencies")
    public void wire(@Descriptor("target instance") String instName) {
        ASMInst inst = CST.ASMInstBroker.getInst(instName);
        if (inst != null)
            dumpState(inst, "  ", null);
    }

    /**
     * Prints the specification.
     * 
     * @param indent the indent
     * @param specification the specification
     */
    private void printSpecification(String indent, ASMSpec specification) {
        System.out.println(indent + "----- [ ASMSpec : " + specification.getASMName() + " ] -----");
        System.out.println(indent + "   Interfaces:");
        for (String interf : specification.getInterfaceNames()) {
            System.out.println(indent + "      " + interf);
        }

        System.out.println(indent + "   Requires:");
        for (ASMSpec spec : specification.getRequires()) {
            System.out.println(indent + "      " + spec);
        }
        printProperties(indent + "   ", specification.getProperties());

    }

    /**
     * Test instances.
     * 
     * @param indent the indent
     * @param instances the instances
     * @throws ConnectionException the connection exception
     */
    private void testInstances(String indent, Set<ASMInst> instances) {
        if (instances == null)
            return;
        for (ASMInst instance : instances) {
            printInstance(indent, instance);
            System.out.print("ASMImpl " + instance.getImpl());
            System.out.println("ASMSpec " + instance.getSpec());
        }

    }

    /**
     * Prints the instance.
     * 
     * @param indent the indent
     * @param instance the instance
     * @throws ConnectionException the connection exception
     */
    private void printInstance(String indent, ASMInst instance) {
        if (instance == null)
            return;
        System.out.println(indent + "----- [ ASMInst : " + instance.getASMName() + " ] -----");
        ASMImpl implementation = instance.getImpl();
        System.out.println(indent + "   dependencies:");
        for (Wire wire : instance.getWires()) {
            System.out.println(indent + "      " + wire.getDepName() + ": " + wire.getDestination());
        }

        System.out.println(indent + "   called by:");
        for (Wire wire : instance.getInvWires())
            System.out.println(indent + "      (" + wire.getDepName() + ") " + wire.getSource());

        if (implementation == null) {
            System.out.println(indent + " warning :  no factory for this instance");
        } else {
            System.out.println(indent + "   implementation : " + instance.getImpl());
            System.out.println(indent + "   specification  : " + instance.getSpec());
            printProperties(indent + "   ", instance.getProperties());
        }

    }

    /**
     * Test implemtations.
     * 
     * @param indent the indent
     * @param impls the impls
     * @throws ConnectionException the connection exception
     */
    private void testImplementations(String indent, Set<ASMImpl> impls) {
        for (ASMImpl impl : impls) {
            printImplementation(indent, impl);
            testInstances(indent + "   ", impl.getInsts());
        }
    }

    /**
     * Prints the implementation.
     * 
     * @param indent the indent
     * @param impl the impl
     * @throws ConnectionException the connection exception
     */
    private void printImplementation(String indent, ASMImpl impl) {
        System.out.println(indent + "----- [ ASMImpl : " + impl + " ] -----");
        // System.out.println(indent + "   implementation pid : " + impl.getImplPid());
        System.out.println(indent + "   specification : " + impl.getSpec());

        System.out.println(indent + "   Uses:");
        for (ASMImpl implem : impl.getUses()) {
            System.out.println(indent + "      " + implem);
        }
        System.out.println(indent + "   Instances:");
        for (ASMInst inst : impl.getInsts()) {
            System.out.println(indent + "      " + inst);
        }
        printProperties(indent + "   ", impl.getProperties());
    }

    /**
     * Prints the properties.
     * 
     * @param indent the indent
     * @param properties the properties
     */
    private void printProperties(String indent, Map<String, Object> properties) {
        System.out.println(indent + "Properties : ");
        for (String key : properties.keySet()) {
            System.out.println(indent + "   " + key + " = " + properties.get(key));
        }
    }

    private void dumpAppli(String name) {
        for (Application appli : apam.getApplications()) {
            if (appli.getName().equals(name)) {
                System.out.println("Application : " + appli.getName() + "  Main : " + appli.getMainImpl());
                dumpComposite(appli.getMainComposite(), "  ");
                System.out.println("\nState: ");
                dumpState(appli.getMainImpl().getInst(), "  ", "");
                break;
            }
        }
    }

    private void dumpApam() {
        for (Application appli : apam.getApplications()) {
            System.out.println("Application : " + appli.getName() + "  Main : " + appli.getMainImpl());
            dumpComposite(appli.getMainComposite(), "  ");
            System.out.println("\nState: ");
            dumpState(appli.getMainImpl().getInst(), "  ", "");
        }
    }

    private void dumpState(ASMInst inst, String indent, String dep) {
        Set<ASMInst> insts = new HashSet<ASMInst>();
        insts.add(inst);
        System.out.println(indent + dep + ": " + inst + " " + inst.getImpl() + " " + inst.getSpec());
        indent = indent + "  ";
        for (Wire wire : inst.getWires()) {
            System.out.println(indent + wire.getDepName() + ": " + wire.getDestination() + " "
                    + wire.getDestination().getImpl() + " " + wire.getDestination().getSpec());
            dumpState0(wire.getDestination(), indent, wire.getDepName(), insts);
        }
    }

    private void dumpState0(ASMInst inst, String indent, String dep, Set<ASMInst> insts) {
        if (insts.contains(inst)) {
            System.out.println(indent + "  *");
            return;
        }
        insts.add(inst);
        indent = indent + "  ";
        for (Wire wire : inst.getWires()) {
            System.out.println(indent + wire.getDepName() + ": " + wire.getDestination() + " "
                    + wire.getDestination().getImpl() + " " + wire.getDestination().getSpec());
            dumpState0(wire.getDestination(), indent, wire.getDepName(), insts);
        }
    }

    private void dumpComposite(Composite compo, String indent) {
        if (compo == null)
            return;
        System.out.println(indent + compo.getName());
        indent = indent + "  ";
        for (Composite comp : compo.getDepend()) {
            dumpComposite(comp, indent);
        }
    }

}
