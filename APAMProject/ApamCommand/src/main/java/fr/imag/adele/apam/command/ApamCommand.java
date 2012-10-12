package fr.imag.adele.apam.command;

/**
 * Copyright Universite Joseph Fourier (www.ujf-grenoble.fr)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.service.command.Descriptor;

import fr.imag.adele.apam.Apam;
import fr.imag.adele.apam.CST;
//import fr.imag.adele.apam.Component; // a cause des annotations de iPOJO !!
import fr.imag.adele.apam.Composite;
import fr.imag.adele.apam.CompositeType;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.Specification;
import fr.imag.adele.apam.Wire;
import fr.imag.adele.apam.apform.Apform2Apam;
import fr.imag.adele.apam.apform.Apform2Apam.Request;
import fr.imag.adele.apam.core.ComponentDeclaration;
import fr.imag.adele.apam.core.ResourceReference;

/**
 * 
 * 
 * @author Jacky
 */
@Instantiate
@Component(public_factory = false, immediate = true, name = "apam.shell")
@Provides(specifications = ApamCommand.class)
public class ApamCommand {

	/**
	 * Defines the command scope (apam).
	 */
	@ServiceProperty(name = "osgi.command.scope", value = "apam")
	String m_scope;

	/**
	 * Defines the functions (commands).
	 */
	@ServiceProperty(name = "osgi.command.function", value = "{}")
	String[] m_function = new String[] { "put",  "specs", "implems", "insts", "spec", "implem", "inst", "dump", "compoTypes",
		"compoType", "compos", "compo", "wire", "launch", "pending", "apdate", "l" };

	// Apam injected
	@Requires
	Apam apam;


	/**
	 * ASMSpec.
	 * 
	 * @param specificationName
	 *            the specification name
	 */
	@Descriptor("Updates the target component")
	public void apdate(@Descriptor("target component to update. Warning: updates the whole Bundle.") String componentName) {
		CST.apamResolver.updateComponent (componentName) ;
	}

	/**
	 * Resolver Commands.
	 */
	@Descriptor("Resolve apam components on the target composite")
	public void put(@Descriptor("the name of the component to resolve ") String componentName,
			@Descriptor("the name of the composite target or root ") String compositeTarget) {
		CompositeType target = null;

		if ("root".equals(compositeTarget)){
			System.out.println("Resolving "+ componentName + " on the root composite");
		} else {
			target = apam.getCompositeType(compositeTarget);
			if (target== null){
				System.out.println("Invalid composite name "+ compositeTarget);
				return;
			}

		}
		System.out.println("< Searching " + componentName +" in specifications > " );
		Specification spec = CST.apamResolver.findSpecByName(target, componentName);
		if (spec ==null){
			System.out.println("< Searching "+ componentName + " in implementations > " );
			CST.apamResolver.findImplByName(target, componentName);
		}
	}

	/**
	 * Specifications.
	 */

	@Descriptor("Display all the Apam specifications")
	public void specs() {
		Set<Specification> specifications = new TreeSet<Specification>(CST.componentBroker.getSpecs());
		for (Specification specification : specifications) {
			System.out.println("ASMSpec : " + specification);
		}
	}

	/**
	 * Implementations.
	 */
	@Descriptor("Display of all the implementations of the local machine")
	public void implems() {
		Set<Implementation> implementations = new TreeSet<Implementation>(CST.componentBroker.getImpls());
		for (Implementation implementation : implementations) {
			System.out.println("ASMImpl : " + implementation);
		}
	}

	/**
	 * Instances.
	 */
	@Descriptor("Display of all the instances of the local machine")
	public void insts() {
		Set<Instance> instances = new TreeSet<Instance>(CST.componentBroker.getInsts());
		for (Instance instance : instances) {
			System.out.println("ASMInst : " + instance);
		}
	}

	/**
	 * ASMSpec.
	 * 
	 * @param specificationName
	 *            the specification name
	 */
	@Descriptor("Display informations about the target specification")
	public void spec(@Descriptor("target specification") String specificationName) {
		Set<Specification> specifications = CST.componentBroker.getSpecs();
		for (Specification specification : specifications) {
			if ((specification.getName() != null) && (specification.getName().equalsIgnoreCase(specificationName))) {
				printSpecification("", specification);
				// testImplementations("   ", specification.getImpls());
				break;
			}
		}
	}

	/**
	 * ASMImpl.
	 * 
	 * @param implementationName
	 *            the implementation name
	 */
	@Descriptor("Display informations about the target implementation")
	public void implem(@Descriptor("target implementation") String implementationName) {
		Implementation implementation = CST.componentBroker.getImpl(implementationName);
		if (implementation == null) {
			System.out.println("No such implementation : " + implementationName);
			return;
		}
		printImplementation("", implementation);
		// testInstances("   ", implementation.getInsts());
	}


	@Descriptor("Start a new instance of the target implementation in root composite")
	public void l(@Descriptor("target implementation") String componentName) {
		launch (componentName, "root") ;
	}


	@Descriptor("Start a new instance of the target implementation")
	public void launch(@Descriptor("target implementation") String componentName,
			@Descriptor("the name of the composite target or root ") String compositeTarget) {

		Composite target = null;
		CompositeType targetType = null;

		if ("root".equals(compositeTarget)){
			System.out.println("Resolving "+ componentName + " on the root composite");
		} else {
			target = apam.getComposite(compositeTarget);
			if (target== null){
				System.out.println("Invalid composite instance "+ compositeTarget);
				return;
			}
			targetType = target.getCompType() ;
		}

		fr.imag.adele.apam.Component compo =  CST.apamResolver.findComponentByName(targetType, componentName);
		if (compo instanceof Implementation)
			((Implementation)compo).createInstance(target,null);
		if (compo instanceof Specification) {
			Implementation impl = CST.apamResolver.resolveSpecByName(targetType, componentName, null, null) ;
			if (impl != null) 
				impl.createInstance(null, null);
		}
	}

	/**
	 * ASMInst.
	 * 
	 * @param implementationName
	 *            the implementation name
	 */
	@Descriptor("Display informations about the target instance")
	public void inst(@Descriptor("target implementation") String instanceName) {
		try {
			Instance instance = CST.componentBroker.getInst(instanceName);
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
		Collection<Composite> applis = apam.getComposites();
		for (Composite appli : applis) {
			System.out.println("Application : " + appli.getName());
		}
	}

	@Descriptor("Display informations about the target application")
	public void appli(@Descriptor("target application") String appliName) {
		Composite appli = apam.getComposite(appliName);
		if (appli == null) {
			System.out.println("No such root composite : " + appliName);
			return;
		}
		System.out.println("Application " + appli);
		for (Composite compo : appli.getSons()) {
			System.out.println("Son Composites : " + compo.getName());
		}
		for (Composite compo : appli.getDepend()) {
			System.out.println("Depends on composites : " + compo.getName());
		}
	}

	@Descriptor("Display the full Apam state model")
	public void dump() {
		dumpApam();
	}

	@Descriptor("Display the pending platform installations")
	public void pending() {
		System.out.println("Platform pernding requests");
		for (Request pendingRequest : Apform2Apam.getPending()) {
			ComponentDeclaration declaration = pendingRequest.getComponent().getDeclaration();
			System.out.println("Adding component "+declaration.getName()+" is waiting for component "+pendingRequest.getRequiredComponent());
		}

	}


	// @Descriptor("Display the state model of the target application")
	// public void state(@Descriptor("target application") String appliName) {
	// dumpCompoType(appliName);
	// }

	@Descriptor("Display all the Apam composites types")
	public void compoTypes() {
		Collection<CompositeType> applis = apam.getCompositeTypes();
		for (CompositeType appli : applis) {
			System.out.println("    " + appli);
		}
	}

	@Descriptor("Display an Apam composite type")
	public void compoType(@Descriptor("target composite type") String name) {
		CompositeType compo = apam.getCompositeType(name);
		if (name == null) {
			System.out.println("No such composite : " + name);
			return;
		}
		printCompositeType(compo, "");
		System.out.println("");
	}

	@Descriptor("Display all the Root Apam composites")
	public void compos() {
		Collection<Composite> comps = apam.getRootComposites();
		for (Composite compo : comps) {
			System.out.println("    " + compo);
		}
	}

	@Descriptor("Display an Apam composites")
	public void compo(@Descriptor("target composite") String compoName) {
		Composite compo = apam.getComposite(compoName);
		if (compo == null) {
			System.out.println("No such composite : " + compoName);
			return;
		}
		printComposite(compo, "");
		System.out.println("");
	}

	private void printCompositeType(CompositeType compo, String indent) {
		System.out.println(indent + "Composite Type " + compo.getName() + ". Main implementation : "
				+ compo.getMainImpl() + ". Models : " + compo.getModels());

		indent += "   " ;
		System.out.print(indent + "Provides resources : ");
		for (ResourceReference ref : compo.getCompoDeclaration().getProvidedResources()) {
			System.out.print(ref + " ");
		}
		System.out.println("");

		System.out.print(indent + "Embedded in composite types : ");
		for (CompositeType comType : compo.getInvEmbedded()) {
			System.out.print(comType.getName() + " ");
		}
		System.out.println("");

		System.out.print(indent + "Contains composite types : ");
		for (CompositeType comType : compo.getEmbedded()) {
			System.out.print(comType.getName() + " ");
		}
		System.out.println("");
		System.out.print(indent + "Imports composite types : ");
		for (CompositeType comDep : compo.getImport()) {
			System.out.print(comDep.getName() + " ");
		}
		System.out.println("");

		System.out.print(indent + "Uses composite types : ");
		for (Implementation comDep : compo.getUses()) {
			System.out.print(comDep.getName() + " ");
		}
		System.out.println("");

		System.out.print(indent + "Contains Implementations: ");
		for (Implementation impl : compo.getImpls()) {
			System.out.print(impl + " ");
		}
		System.out.println("");

		System.out.print(indent + "Composite Instances : ");
		for (Instance inst : compo.getInsts()) {
			System.out.print(inst + " ");
		}
		System.out.println("");

		System.out.println(compo.getApformImpl().getDeclaration().printDeclaration(indent));

		for (Instance compInst : compo.getInsts()) {
			printComposite((Composite) compInst, indent + "   ");
		}

		for (CompositeType comType : compo.getEmbedded()) {
			System.out.println("\n");
			printCompositeType(comType, indent );
		}
	}

	private void printComposite(Composite compo, String indent) {
		System.out.println(indent + "Composite " + compo.getName() + " Composite Type : "
				+ compo.getCompType().getName() + " Father : " + compo.getFather());
		System.out.println(indent + "   In application : " + compo.getAppliComposite());
		System.out.print  (indent + "   Son composites : ");
		for (Composite comDep : compo.getSons()) {
			System.out.print(comDep.getName() + " ");
		}
		System.out.println("");

		System.out.print(indent + "   Depends on composites : ");
		for (Composite comDep : compo.getDepend()) {
			System.out.print(comDep.getName() + " ");
		}
		System.out.println("");

		if (!compo.getContainInsts().isEmpty()) {
			System.out.print(indent + "   Contains instances : ");
			for (Instance inst : compo.getContainInsts()) {
				System.out.print(inst + " ");
			}
			System.out.println("");
		}

		indent += "  " ;
		System.out.println(indent + " State " + compo);
		dumpState(compo.getMainInst(), indent, " ");
		//System.out.println("");

		System.out.println(compo.getApformInst().getDeclaration().printDeclaration(indent));

		if (!compo.getSons().isEmpty()) {
			System.out.println("\n");
			for (Composite comp : compo.getSons()) {
				printComposite(comp, indent + "   ");
			}
		}
	}

	@Descriptor("Display all the dependencies")
	public void wire(@Descriptor("target instance") String instName) {
		Instance inst = CST.componentBroker.getInst(instName);
		if (inst != null)
			dumpState(inst, "  ", null);
	}

	/**
	 * Prints the specification.
	 * 
	 * @param indent
	 *            the indent
	 * @param specification
	 *            the specification
	 */
	private void printSpecification(String indent, Specification specification) {
		System.out.println(indent + "----- [ ASMSpec : " + specification.getName() + " ] -----");
		indent += "   " ;
		System.out.println(indent + "Interfaces:");
		for (ResourceReference res : specification.getDeclaration().getProvidedResources()) {
			System.out.println(indent + "      " + res);
		}

		System.out.println(specification.getDeclaration().getDependencies());

		System.out.println(indent + "Effective Required specs:");
		for (Specification spec : specification.getRequires()) {
			System.out.println(indent + "      " + spec);
		}

		System.out.println(indent + "Required by:");

		for (Specification spec : specification.getInvRequires()) {
			System.out.println(indent + "      " + spec);
		}

		System.out.println(indent + "Implementations:");
		for (Implementation impl : specification.getImpls()) {
			System.out.println(indent + "      " + impl);
		}
		printProperties(indent, specification.getAllProperties());
		System.out.println(specification.getApformSpec().getDeclaration().printDeclaration(indent));

	}

	/**
	 * Test instances.
	 * 
	 * @param indent
	 *            the indent
	 * @param instances
	 *            the instances
	 * @throws ConnectionException
	 *             the connection exception
	 */
	private void testInstances(String indent, Set<Instance> instances) {
		if (instances == null)
			return;
		for (Instance instance : instances) {
			printInstance(indent, instance);
			System.out.print("ASMImpl " + instance.getImpl());
			System.out.println("ASMSpec " + instance.getSpec());
		}

	}

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
	private void printInstance(String indent, Instance instance) {
		if (instance == null)
			return;
		System.out.println(indent + "----- [ ASMInst : " + instance.getName() + " ] -----");
		Implementation implementation = instance.getImpl();
		indent += "   " ;
		System.out.println(indent + "Dependencies:");
		for (Wire wire : instance.getWires()) {
			System.out.println(indent + "   " + wire.getDepName() + ": " + wire.getDestination());
		}

		System.out.println(indent + "Called by:");
		for (Wire wire : instance.getInvWires())
			System.out.println(indent + "   (" + wire.getDepName() + ") " + wire.getSource());

		if (implementation == null) {
			System.out.println(indent + "warning :  no factory for this instance");
		} else {
			System.out.println(indent + "specification  : " + instance.getSpec());
			System.out.println(indent + "implementation : " + instance.getImpl());
			System.out.println(indent + "in composite   : " + instance.getComposite());
			System.out.println(indent + "in application : " + instance.getAppliComposite());
			printProperties(indent, instance.getAllProperties());
		}
		System.out.println(instance.getApformInst().getDeclaration().printDeclaration(indent));
	}

	//	/**
	//	 * Test implemtations.
	//	 * 
	//	 * @param indent
	//	 *            the indent
	//	 * @param impls
	//	 *            the impls
	//	 * @throws ConnectionException
	//	 *             the connection exception
	//	 */
	//	private void testImplementations(String indent, Set<Implementation> impls) {
	//		for (Implementation impl : impls) {
	//			printImplementation(indent, impl);
	//			testInstances(indent + "   ", impl.getInsts());
	//		}
	//	}

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
	private void printImplementation(String indent, Implementation impl) {
		System.out.println(indent + "----- [ ASMImpl : " + impl + " ] -----");

		indent += "  " ;
		System.out.println(indent + "specification : " + impl.getSpec());

		System.out.println(indent + "In composite types:");
		for (CompositeType compo : impl.getInCompositeType()) {
			System.out.println(indent + "      " + compo.getName());
		}

		System.out.println(indent + "Uses:");
		for (Implementation implem : impl.getUses()) {
			System.out.println(indent + "      " + implem);
		}

		System.out.println(indent + "Used by:");
		for (Implementation implem : impl.getInvUses()) {
			System.out.println(indent + "      " + implem);
		}

		System.out.println(indent + "Instances:");
		for (Instance inst : impl.getInsts()) {
			System.out.println(indent + "      " + inst);
		}
		printProperties(indent , impl.getAllProperties());

		System.out.println(impl.getApformImpl().getDeclaration().printDeclaration(indent));
	}

	/**
	 * Prints the properties.
	 * 
	 * @param indent
	 *            the indent
	 * @param properties
	 *            the properties
	 */
	private void printProperties(String indent, Map<String, String> properties) {
		System.out.println(indent + "Properties : ");
		for (String key : properties.keySet()) {
			System.out.println(indent + "   " + key + " = " + properties.get(key));
		}
	}

	private void dumpCompoType(String name) {
		CompositeType compType = apam.getCompositeType(name);
		if (compType == null) {
			System.out.println("No such application :" + name);
			return;
		}
		printCompositeType(compType, "");
	}

	//	private void dumpCompo(Composite comp) {
	//		printComposite(comp, "");
	//		System.out.println("State: ");
	//		dumpState(comp.getMainInst(), "  ", "");
	//		System.out.println("\n");
	//	}

	private void dumpApam() {
		for (CompositeType compo : apam.getRootCompositeTypes()) {
			dumpCompoType(compo.getName());
			System.out.println("\n");
		}
	}

	private void dumpState(Instance inst, String indent, String dep) {
		if (inst == null)
			return;
		Set<Instance> insts = new HashSet<Instance>();
		insts.add(inst);
		System.out.println(indent + dep + ": " + inst + " " + inst.getImpl() + " " + inst.getSpec());
		indent = indent + "  ";
		for (Wire wire : inst.getWires()) {
			System.out.println(indent + wire.getDepName() + ": " + wire.getDestination() + " "
					+ wire.getDestination().getImpl() + " " + wire.getDestination().getSpec());
			dumpState0(wire.getDestination(), indent, wire.getDepName(), insts);
		}
	}

	private void dumpState0(Instance inst, String indent, String dep, Set<Instance> insts) {
		if (insts.contains(inst)) {
			System.out.println(indent + "*" + dep + ": " + inst.getName());
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

}

