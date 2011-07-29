package fr.imag.adele.apam;

import java.util.Set;

import fr.imag.adele.am.exception.ConnectionException;
import fr.imag.adele.apam.ASMImpl.ASMImplImpl;
import fr.imag.adele.apam.ASMImpl.ASMInstImpl;
import fr.imag.adele.apam.ASMImpl.ASMSpecImpl;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.apamAPI.CompExInst;
import fr.imag.adele.apam.apamAPI.CompExType;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Implementation;
import fr.imag.adele.sam.Instance;

public class CompExInstImpl extends ASMInstImpl implements CompExInst, Composite {

    private final CompositeImpl me;
    private final CompExType    compType;
    private final ASMImpl       mainImpl;
    private final ASMInst       mainInst;

    // mainSpec == this.getSpec ()

    private CompExInstImpl(CompExType father, Composite instCompo, Instance samCompInst, ASMInst asmInst) {
        //the composite instance as an ASMInst
        super((ASMImpl) father, instCompo, null, samCompInst);

        //create main instance
        me = new CompositeImpl(asmInst.getASMName(), father, father.getApplication(), null);
        compType = father;
        mainInst = asmInst;
        mainImpl = asmInst.getImpl();
    }

    //@Override
    public static CompExInst createCompExInst(CompExType compExType, Composite instCompo, Attributes initialproperties) {
        Set<ManagerModel> models = null;
        //look for main implem. Deploy if needed.
        ASMInst asmInst;
        Instance samCompInst = null;
        try {
            samCompInst = ((ASMImplImpl) compExType).getSamImpl().createInstance(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (compExType.getMainImpl() != null) {
            asmInst = compExType.getMainImpl().createInst(instCompo, initialproperties);
            return new CompExInstImpl(compExType, instCompo, samCompInst, asmInst);
        }
        //look by name. Deploy if needed.
        asmInst = (CST.apam).resolveAppli(compExType, instCompo, compExType.getMainImplName(), null, null, null);
        if (asmInst == null)
            return null;

        return new CompExInstImpl(compExType, instCompo, samCompInst, asmInst);
    }

    @Override
    public ASMInst getMainInst() {
        return mainInst;
    }

    @Override
    public ASMImpl getMainImpl() {
        return mainImpl;
    }

    @Override
    public CompExType getCompType() {
        return compType;
    }

    @Override
    public Object getServiceObject() {
        return ((ASMInstImpl) mainInst).getServiceObject();
    }

    @Override
    public String getName() {
        return me.getName();
    }

    @Override
    public Application getApplication() {
        return me.getApplication();
    }

    @Override
    public Composite createComposite(Composite father, String name, Set<ManagerModel> models) {
        return me.createComposite(father, name, models);
    }

    @Override
    public void addDepend(Composite destination) {
        me.addDepend(destination);
    }

    @Override
    public boolean removeDepend(Composite destination) {
        return me.removeDepend(destination);
    }

    @Override
    public Set<Composite> getDepend() {
        return me.getDepend();
    }

    @Override
    public boolean dependsOn(Composite destination) {
        return me.dependsOn(destination);
    }

    @Override
    public Composite getFather() {
        return me.getFather();
    }

    @Override
    public Set<Composite> getSons() {
        return me.getSons();
    }

    @Override
    public ManagerModel getModel(String name) {
        return me.getModel(name);
    }

    @Override
    public Set<ManagerModel> getModels() {
        return me.getModels();
    }

    @Override
    public void addSpec(ASMSpec spec) {
        me.addSpec(spec);
    }

    @Override
    public boolean containsSpec(ASMSpec spec) {
        return me.containsSpec(spec);
    }

    @Override
    public Set<ASMSpec> getSpecs() {
        return me.getSpecs();
    }

    @Override
    public void addImpl(ASMImpl impl) {
        me.addImpl(impl);
    }

    @Override
    public boolean containsImpl(ASMImpl spec) {
        return me.containsImpl(spec);
    }

    @Override
    public Set<ASMImpl> getImpls() {
        return me.getImpls();
    }

    @Override
    public void addInst(ASMInst inst) {
        me.addInst(inst);
    }

    @Override
    public boolean containsInst(ASMInst inst) {
        return me.containsInst(inst);
    }

    @Override
    public Set<ASMInst> getInsts() {
        return me.getInsts();
    }

    @Override
    public void setMainSpec(ASMSpec spec) {
        new Exception("cannot set Spec in a CompExType").printStackTrace();
    }

    @Override
    public void setMainImpl(ASMImpl impl) {
        new Exception("cannot set Impl in a CompExType").printStackTrace();
    }

    @Override
    public void setMainInst(ASMInst inst) {
        new Exception("cannot set Inst in a CompExType").printStackTrace();
    }

    @Override
    public ASMSpec getMainSpec() {
        return mainImpl.getSpec();
    }

}
