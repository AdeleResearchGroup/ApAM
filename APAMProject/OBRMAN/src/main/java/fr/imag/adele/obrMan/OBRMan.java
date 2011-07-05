package fr.imag.adele.obrMan;

import java.util.List;
import java.util.Set;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.utils.filter.FilterImpl;
import org.osgi.framework.Filter;

import fr.imag.adele.apam.ManagerModel;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.apamAPI.Manager;

public class OBRMan implements Manager {

    // iPOJO injected
    private RepositoryAdmin repoAdmin;
    private Apam            apam;

    private Resolver        resolver;
    private Repository      local;
    private Resource[]      allResources;

    private void printCap(Capability aCap) {
        System.out.println("   Capability name: " + aCap.getName());
        for (Property prop : aCap.getProperties()) {
            System.out.println("     " + prop.getName() + " type= " + prop.getType() + " val= " + prop.getValue());
        }
    }

    private void printRes(Resource aResource) {
        // System.out.println("capabilities : " + aResource.getCapabilities());
        // System.out.println("getPresentationName : " + aResource.getPresentationName());
        System.out.println("\n\nRessource SymbolicName : " + aResource.getSymbolicName());
        for (Capability aCap : aResource.getCapabilities()) {
            printCap(aCap);
        }
    }

    private void resolveDeploy(Resource aResource) {
        resolver.add(aResource);
        if (resolver.resolve()) {
            resolver.deploy(Resolver.START);
        } else {
            Reason[] reqs = resolver.getUnsatisfiedRequirements();
            for (Reason req : reqs) {
                System.out.println("Unable to resolve: " + req);
            }
        }
    }

    public void start() {
        System.out.println("entering OBRMan");
        // local = repoAdmin.getLocalRepository();
        try {
            local = repoAdmin.addRepository("file:///F:/Maven/.m2/repository.xml");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("local repo : " + local.getURI());
        resolver = repoAdmin.resolver();
        allResources = local.getResources();
        for (Resource res : allResources) {
            printRes(res);
            // test("(name=" + res.getSymbolicName() + ")");
        }
        Resource selected;
        selected = lookFor("bundle", "(symbolicname=ApamCommand)");
        // test("(name=S4)");
        // test("");
        // test("");
    }

    public void stop() {

    }

    private Resource lookFor(String capability, String filterStr) {
        System.out.println("looking for capability : " + capability + "; filter : " + filterStr);
        Requirement req = repoAdmin.getHelper().requirement(capability, filterStr);
        try {
            FilterImpl filter = FilterImpl.newInstance(filterStr);
            allResources = local.getResources();
            System.out.println("requeriment discover : "
                    + repoAdmin.discoverResources(new Requirement[] { req }).length);
            for (Resource res : allResources) {
                for (Capability aCap : res.getCapabilities()) {
                    System.out.println("requeriment match : " + req.isSatisfied(aCap));
                    if (aCap.getName().equals(capability)) {
                        System.out.println("resource : " + res.getSymbolicName() + "; Capability : "
                                + aCap.getName() + "; properties : " + aCap.getProperties());
                        if (filter.matchCase(aCap.getPropertiesAsMap())) {
                            System.out.println(" found !!!");
                            return res;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deployInstall(Resource res) {
        resolver.add(res);
        if (resolver.resolve()) {
            resolver.deploy(Resolver.START);
        } else {
            Reason[] reqs = resolver.getUnsatisfiedRequirements();
            for (Reason req : reqs) {
                System.out.println("Unable to resolve: " + req);
            }
        }
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Filter> getConstraintsSpec(String interfaceName, String specName, String depName,
            List<Filter> initConstraints) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Manager> getSelectionPathSpec(ASMInst from, String interfaceName, String specName, String depName,
            Set<Filter> constraints, List<Manager> involved) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Manager> getSelectionPathImpl(ASMInst from, String samImplName, String implName, String depName,
            Set<Filter> constraints, List<Manager> involved) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ASMInst resolveSpec(ASMInst from, String interfaceName, String specName, String depName,
            Set<Filter> constraints) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<ASMInst> resolveSpecs(ASMInst from, String interfaceName, String specName, String depName,
            Set<Filter> constraints) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ASMInst resolveImpl(ASMInst from, String samImplName, String implName, String depName,
            Set<Filter> constraints) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<ASMInst> resolveImpls(ASMInst from, String samImplName, String implName, String depName,
            Set<Filter> constraints) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getPriority() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void newComposite(ManagerModel model, Composite composite) {
        // TODO Auto-generated method stub

    }

}
