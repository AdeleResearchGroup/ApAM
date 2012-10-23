package fr.imag.adele.apam.distriman;

import fr.imag.adele.apam.*;
import fr.imag.adele.apam.core.DependencyDeclaration;
import fr.imag.adele.apam.core.ResolvableReference;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class Distriman implements DependencyManager{

    //Default logger
    private static Logger logger = LoggerFactory.getLogger(Distriman.class);

    //Distriman priority, default is 2
    private int priority = 2;

    private final BundleContext context;

    public Distriman(BundleContext context) {
        this.context = context;
    }

    public String getName() {
        return CST.DISTRIMAN;
    }

    @Validate
    private void init(){
        logInfo("Starting...");
        //Add Distriman to Apam
        ApamManagers.addDependencyManager(this, priority);
        logInfo("Successfully initialized");
    }

    @Invalidate
    private void stop(){
        logInfo("Stopping...");
        //Remove Distriman from Apam
        ApamManagers.removeDependencyManager(this);
        logInfo("Successfully stopped");
    }

    public ComponentBundle findBundle(CompositeType compoType, String bundleSymbolicName, String componentName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void getSelectionPath(CompositeType compTypeFrom, DependencyDeclaration dependency, List<DependencyManager> selPath) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getPriority() {
        return priority;
    }

    public void newComposite(ManagerModel model, CompositeType composite) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Implementation resolveSpec(CompositeType compoTypeFrom, DependencyDeclaration dependency) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<Implementation> resolveSpecs(CompositeType compoTypeFrom, DependencyDeclaration dependency) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Instance findInstByName(Composite composite, String instName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Implementation findImplByName(CompositeType compoType, String implName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Specification findSpecByName(CompositeType compoType, String specName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Component findComponentByName(CompositeType compoType, String compName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Implementation findImplByDependency(CompositeType compoType, DependencyDeclaration dependency) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Instance resolveImpl(Composite compo, Implementation impl, DependencyDeclaration dependency) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<Instance> resolveImpls(Composite compo, Implementation impl, DependencyDeclaration dependency) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void notifySelection(Instance client, ResolvableReference resName, String depName, Implementation impl, Instance inst, Set<Instance> insts) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    protected static void logInfo(String message,Throwable t){
        logger.info("["+CST.DISTRIMAN+"]"+message,t);
    }

    protected static void logInfo(String message){
        logger.info("["+CST.DISTRIMAN+"]"+message);
    }
}
