package fr.imag.adele.apam.jmx;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Component;
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.Apform2Apam.Request;
import fr.imag.adele.apam.declarations.ResourceReference;

public class ApamJMXImpl implements ApamJMX {

    private Apam apam;

    
    String fieldTest ="toto";
    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#start()
     */
    
    public void start() {
        System.out.println("\nSTART APAM JMX Start");
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#stop()
     */
    public void stop() {
        System.out.println("\nSTOP APAM JMX");
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#startAppli()
     */
    @Override
    public String test() {
         return ("\nTest OK!");
        
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#up(java.lang.String)
     */
 
    @Override
    public String up( String componentName) {
         CST.apamResolver.updateComponent(componentName);
         return componentName + "remotely updated!";
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#put(java.lang.String)
     */


    @Override
    public String put(String componentName) {
        return put(componentName, "root");
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#put(java.lang.String, java.lang.String)
     */
    @Override
    public String put(String componentName, String compositeTarget) {
        CompositeType target = null;
        String text="";
        if ("root".equals(compositeTarget)) {
            text+=("\nResolving " + componentName + " on the root composite");
        } else {
            target = apam.getCompositeType(compositeTarget);
            if (target == null) {
                text+=("\nInvalid composite name " + compositeTarget);
               
            }

        }
        text+=("\n< Searching " + componentName + " in " + target + " repositories> ");
        Component c= CST.apamResolver.findComponentByName(target, componentName);
        return text+="\n"+c.getName()+" deployed";
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#specs()
     */

    @Override
    public String specs() {
        String text="";
        Set<Specification> specifications = new TreeSet<Specification>(CST.componentBroker.getSpecs());
        for (Specification specification : specifications) {
            text+=("\nASMSpec : " + specification);
        }
        return text;
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#implems()
     */
    @Override
    public String implems() {
        String text="";
        Set<Implementation> implementations = new TreeSet<Implementation>(CST.componentBroker.getImpls());
        for (Implementation implementation : implementations) {
            text+=("\nASMImpl : " + implementation);
        }
        return text;
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#insts()
     */
    @Override
    public String insts() {
        String text="";
        Set<Instance> instances = new TreeSet<Instance>(CST.componentBroker.getInsts());
        for (Instance instance : instances) {
            text+=("\nASMInst : " + instance);
        }
        return text;
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#spec(java.lang.String)
     */
    @Override
    public String spec(String specificationName) {
        String text="";
        Set<Specification> specifications = CST.componentBroker.getSpecs();
        for (Specification specification : specifications) {
            if ((specification.getName() != null) && (specification.getName().equalsIgnoreCase(specificationName))) {
                text+="\n"+printSpecification("", specification);
                // testImplementations("   ", specification.getImpls());
                break;
            }
        }
        return text;
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#implem(java.lang.String)
     */
    
    @Override
    public String implem( String implementationName) {
        String text="";
        Implementation implementation = CST.componentBroker.getImpl(implementationName);
        if (implementation == null) {
            text+=("\nNo such implementation : " + implementationName);
            return text;
        }
        return text+="\n"+printImplementation("", implementation);
        // testInstances("   ", implementation.getInsts());
    }

    
    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#l(java.lang.String)
     */
    @Override
    public String l( String componentName) {
        return launch(componentName, "root");
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#launch(java.lang.String, java.lang.String)
     */
    @Override
    public String launch(String componentName, String compositeTarget) {
        String text="";
        Composite target = null;
        CompositeType targetType = null;

        if ("root".equals(compositeTarget)) {
            text+=("\nResolving " + componentName + " on the root composite");
        } else {
            target = apam.getComposite(compositeTarget);
            if (target == null) {
                text+=("\nInvalid composite instance " + compositeTarget);
                return text;
            }
            targetType = target.getCompType();
        }

        fr.imag.adele.apam.Component compo = CST.apamResolver.findComponentByName(targetType, componentName);
        if (compo instanceof Implementation)
            ((Implementation) compo).createInstance(target, null);
        if (compo instanceof Specification) {
            Implementation impl = CST.apamResolver.resolveSpecByName(targetType, componentName, null, null);
            if (impl != null)
                impl.createInstance(null, null);
        }
        return text;
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#inst(java.lang.String)
     */

    @Override
    public String inst(String instanceName) {
        String text="";
        try {
            Instance instance = CST.componentBroker.getInst(instanceName);
            if (instance == null) {
                text+=("\nNo such instance : " + instanceName);
            } else
                text+="\n"+printInstance("", instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#applis()
     */
    @Override
    public String applis() {
        String text="";
        Collection<Composite> applis = apam.getComposites();
        for (Composite appli : applis) {
            text+=("\nApplication : " + appli.getName());
        }
        return text;
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#appli(java.lang.String)
     */
    @Override
    public String appli(String appliName) {
        String text="";
        Composite appli = apam.getComposite(appliName);
        if (appli == null) {
            text+=("\nNo such root composite : " + appliName);
            return text;
        }
        text+=("\nApplication " + appli);
        for (Composite compo : appli.getSons()) {
            text+=("\nSon Composites : " + compo.getName());
        }
        for (Composite compo : appli.getDepend()) {
            text+=("\nDepends on composites : " + compo.getName());
        }
       return text;
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#dump()
     */
    @Override
    public String dump() {
       return dumpApam();
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#pending()
     */
    @Override
    public String pending() {
        String text="";
        text+=("\nPlatform pernding requests");
        for (Request pendingRequest : Apform2Apam.getPending()) {
            text+=("\n"+pendingRequest.getDescription() + " is waiting for component "
                    + pendingRequest.getRequiredComponent());
        }
        return text;
    }

    // @Descriptor("Display the state model of the target application")
    // public String state(@Descriptor("target application") String appliName) {
    // dumpCompoType(appliName);
    // }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#compoTypes()
     */
    @Override
    public String compoTypes() {
        String text="";
        Collection<CompositeType> applis = apam.getCompositeTypes();
        for (CompositeType appli : applis) {
            text+=("\n    " + appli);
        }
        return text;
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#compoType(java.lang.String)
     */
    @Override
    public String compoType(String name) {
        String text="";
        CompositeType compo = apam.getCompositeType(name);
        if (name == null) {
            text+=("\nNo such composite : " + name);
            return text;
        }
        return text+="\n"+printCompositeType(compo, "");
        
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#compos()
     */
    @Override
    public String compos() {
        String text="";
        Collection<Composite> comps = apam.getRootComposites();
        for (Composite compo : comps) {
            text+=("\n    " + compo);
        }
        return text;
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#compo(java.lang.String)
     */
    @Override
    public String compo(String compoName) {
        String text="";
        Composite compo = apam.getComposite(compoName);
        if (compo == null) {
            text+=("\nNo such composite : " + compoName);
            return text;
        }
        return "\n"+printComposite(compo, "");
        
    }

    private String printCompositeType(CompositeType compo, String indent) {
        String text="";
        text+=("\n"+indent + "Composite Type " + compo.getName() + ". Main implementation : "
                + compo.getMainImpl() + ". Models : " + compo.getModels());

        indent += "   ";
        text+=("\n"+indent + "Provides resources : ");
        for (ResourceReference ref : compo.getCompoDeclaration().getProvidedResources()) {
            text+=("\n"+ref + " ");
        }
        text+=("\n");

        text+=("\n"+indent + "Embedded in composite types : ");
        for (CompositeType comType : compo.getInvEmbedded()) {
            text+=("\n"+comType.getName() + " ");
        }
        text+=("\n");

        text+=("\n"+indent + "Contains composite types : ");
        for (CompositeType comType : compo.getEmbedded()) {
            text+=("\n"+comType.getName() + " ");
        }
        text+=("\n");
        text+=("\n"+indent + "Imports composite types : ");
        for (CompositeType comDep : compo.getImport()) {
            text+=("\n"+comDep.getName() + " ");
        }
        text+=("\n");

        text+=("\n"+indent + "Uses composite types : ");
        for (Implementation comDep : compo.getUses()) {
            text+=("\n"+comDep.getName() + " ");
        }
        text+=("\n");

        text+=("\n"+indent + "Contains Implementations: ");
        for (Implementation impl : compo.getImpls()) {
            text+=("\n"+impl + " ");
        }
        text+=("\n");

        text+=("\n"+indent + "Composite Instances : ");
        for (Instance inst : compo.getInsts()) {
            text+=("\n"+inst + " ");
        }
        text+=("\n");

        text+=("\n"+compo.getApformImpl().getDeclaration().printDeclaration(indent));

        for (Instance compInst : compo.getInsts()) {
            text+="\n"+printComposite((Composite) compInst, indent + "   ");
        }

        for (CompositeType comType : compo.getEmbedded()) {
            text+=("\n\n");
            text+="\n"+printCompositeType(comType, indent);
        }
        return text;
    }

    private String printComposite(Composite compo, String indent) {
        String text="";
        text+=("\n"+indent + "Composite " + compo.getName() + " Composite Type : "
                + compo.getCompType().getName() + " Father : " + compo.getFather());
        text+=("\n"+indent + "   In application : " + compo.getAppliComposite());
        text+=("\n"+indent + "   Son composites : ");
        for (Composite comDep : compo.getSons()) {
            text+=("\n"+comDep.getName() + " ");
        }
        text+=("\n");

        text+=("\n"+indent + "   Depends on composites : ");
        for (Composite comDep : compo.getDepend()) {
            text+=("\n"+comDep.getName() + " ");
        }
        text+=("\n");

        if (!compo.getContainInsts().isEmpty()) {
            text+=("\n"+indent + "   Contains instances : ");
            for (Instance inst : compo.getContainInsts()) {
                text+=("\n"+inst + " ");
            }
            text+=("\n");
        }

        indent += "  ";
        text+=("\n"+indent + " State " + compo);
        text+="\n"+dumpState(compo.getMainInst(), indent, " ");
        // text+=("\n");

        text+=("\n"+compo.getApformInst().getDeclaration().printDeclaration(indent));

        if (!compo.getSons().isEmpty()) {
            text+=("\n\n");
            for (Composite comp : compo.getSons()) {
                text+="\n"+printComposite(comp, indent + "   ");
            }
        }
        return text;
    }

    /* (non-Javadoc)
     * @see fr.imag.adele.apam.jmx.ApamJMX#wire(java.lang.String)
     */
    @Override
    public String wire(String instName) {
        String text="";
        Instance inst = CST.componentBroker.getInst(instName);
        if (inst != null)
            return text+="\n"+  dumpState(inst, "  ", null);
        return text;
    }

    /**
     * Prints the specification.
     * 
     * @param indent
     *            the indent
     * @param specification
     *            the specification
     */
    private String printSpecification(String indent, Specification specification) {
        String text="";
        text+=("\n"+indent + "----- [ ASMSpec : " + specification.getName() + " ] -----");
        indent += "   ";
        text+=("\n"+indent + "Interfaces:");
        for (ResourceReference res : specification.getDeclaration().getProvidedResources()) {
            text+=("\n"+indent + "      " + res);
        }

        text+=("\n"+specification.getDeclaration().getDependencies());

        text+=("\n"+indent + "Effective Required specs:");
        for (Specification spec : specification.getRequires()) {
            text+=("\n"+indent + "      " + spec);
        }

        text+=("\n"+indent + "Required by:");

        for (Specification spec : specification.getInvRequires()) {
            text+=("\n"+indent + "      " + spec);
        }

        text+=("\n"+indent + "Implementations:");
        for (Implementation impl : specification.getImpls()) {
            text+=("\n"+indent + "      " + impl);
        }
        text+=printProperties(indent, specification.getAllProperties());
       return text+=("\n"+specification.getApformSpec().getDeclaration().printDeclaration(indent));

    }

//    /**
//     * Test instances.
//     * 
//     * @param indent
//     *            the indent
//     * @param instances
//     *            the instances
//     * @throws ConnectionException
//     *             the connection exception
//     */
//    private String testInstances(String indent, Set<Instance> instances) {
//        String text="";
//        if (instances == null)
//            return text;
//        for (Instance instance : instances) {
//            text+="\n"+printInstance(indent, instance);
//            text+=("\n"+"ASMImpl " + instance.getImpl());
//            text+=("\nASMSpec " + instance.getSpec());
//        }
//        return text;
//    }

    /**
     * Prints the instance.
     * 
     * @param indent
     *            the indent
     * @param instance
     *            the instance
     * @throws ConnectionException
     *             the connection exception
     */
    private String printInstance(String indent, Instance instance) {
        String text="";
        if (instance == null)
            return text;
        text+=("\n"+indent + "----- [ ASMInst : " + instance.getName() + " ] -----");
        Implementation implementation = instance.getImpl();
        indent += "   ";
        text+=("\n"+indent + "Dependencies:");
        for (Wire wire : instance.getWires()) {
            text+=("\n"+indent + "   " + wire.getDepName() + ": " + wire.getDestination());
        }

        text+=("\n"+indent + "Called by:");
        for (Wire wire : instance.getInvWires())
            text+=("\n"+indent + "   (" + wire.getDepName() + ") " + wire.getSource());

        if (implementation == null) {
            text+=("\n"+indent + "warning :  no factory for this instance");
        } else {
            text+=("\n"+indent + "specification  : " + instance.getSpec());
            text+=("\n"+indent + "implementation : " + instance.getImpl());
            text+=("\n"+indent + "in composite   : " + instance.getComposite());
            text+=("\n"+indent + "in application : " + instance.getAppliComposite());
            text+=printProperties(indent, instance.getAllProperties());
        }
        return text+=("\n"+instance.getApformInst().getDeclaration().printDeclaration(indent));
    }

    // /**
    // * Test implemtations.
    // *
    // * @param indent
    // * the indent
    // * @param impls
    // * the impls
    // * @throws ConnectionException
    // * the connection exception
    // */
    // private String testImplementations(String indent, Set<Implementation> impls) {
    // for (Implementation impl : impls) {
    // printImplementation(indent, impl);
    // testInstances(indent + "   ", impl.getInsts());
    // }
    // }

    /**
     * Prints the implementation.
     * 
     * @param indent
     *            the indent
     * @param impl
     *            the impl
     * @throws ConnectionException
     *             the connection exception
     */
    private String printImplementation(String indent, Implementation impl) {
        String text="";
        text+=("\n"+indent + "----- [ ASMImpl : " + impl + " ] -----");

        indent += "  ";
        text+=("\n"+indent + "specification : " + impl.getSpec());

        text+=("\n"+indent + "In composite types:");
        for (CompositeType compo : impl.getInCompositeType()) {
            text+=("\n"+indent + "      " + compo.getName());
        }

        text+=("\n"+indent + "Uses:");
        for (Implementation implem : impl.getUses()) {
            text+=("\n"+indent + "      " + implem);
        }

        text+=("\n"+indent + "Used by:");
        for (Implementation implem : impl.getInvUses()) {
            text+=("\n"+indent + "      " + implem);
        }

        text+=("\n"+indent + "Instances:");
        for (Instance inst : impl.getInsts()) {
            text+=("\n"+indent + "      " + inst);
        }
        text+="\n"+printProperties(indent, impl.getAllProperties());

        return text+=("\n"+impl.getApformImpl().getDeclaration().printDeclaration(indent));
    }

    /**
     * Prints the properties.
     * 
     * @param indent
     *            the indent
     * @param map
     *            the properties
     */
    private String printProperties(String indent, Map<String, Object> map) {
        String text="";
        text+=("\n"+indent + "Properties : ");
        for (String key : map.keySet()) {
            text+=("\n"+indent + "   " + key + " = " + map.get(key));
        }
        return text;
    }

    private String dumpCompoType(String name) {
        String text="";
        CompositeType compType = apam.getCompositeType(name);
        if (compType == null) {
            text+=("\nNo such application :" + name);
            return text;
        }
        return text+="\n"+printCompositeType(compType, "");
    }

    // private String dumpCompo(Composite comp) {
    // printComposite(comp, "");
    // text+=("\nState: ");
    // dumpState(comp.getMainInst(), "  ", "");
    // text+=("\n\n");
    // }

    private String dumpApam() {
        String text="";
        for (CompositeType compo : apam.getRootCompositeTypes()) {
            text+="\n"+dumpCompoType(compo.getName());
            text+=("\n\n");
        }
        return text;
    }

    private String dumpState(Instance inst, String indent, String dep) {
        String text="";
        if (inst == null)
            return text;
        Set<Instance> insts = new HashSet<Instance>();
        insts.add(inst);
        text+=("\n"+indent + dep + ": " + inst + " " + inst.getImpl() + " " + inst.getSpec());
        indent = indent + "  ";
        for (Wire wire : inst.getWires()) {
            text+=("\n"+indent + wire.getDepName() + ": " + wire.getDestination() + " "
                    + wire.getDestination().getImpl() + " " + wire.getDestination().getSpec());
            text+="\n"+dumpState0(wire.getDestination(), indent, wire.getDepName(), insts);
        }
        return text;
    }

    private String dumpState0(Instance inst, String indent, String dep, Set<Instance> insts) {
        String text="";
        if (insts.contains(inst)) {
            return("\n"+indent + "*" + dep + ": " + inst.getName());
            
        }
        insts.add(inst);
        indent = indent + "  ";
        for (Wire wire : inst.getWires()) {
            text+=("\n"+indent + wire.getDepName() + ": " + wire.getDestination() + " "     + wire.getDestination().getImpl() + " " + wire.getDestination().getSpec());
            text+="\n"+dumpState0(wire.getDestination(), indent, wire.getDepName(), insts);
        }
        return text;
    }
}
