package fr.imag.adele.apam;

import java.util.Set;

import fr.imag.adele.apam.ASMImpl.ASMInstImpl;
import fr.imag.adele.apam.apamAPI.ASMImpl;
import fr.imag.adele.apam.apamAPI.ASMInst;
import fr.imag.adele.apam.apamAPI.ASMSpec;
import fr.imag.adele.apam.apamAPI.Application;
import fr.imag.adele.apam.apamAPI.CompExInst;
import fr.imag.adele.apam.apamAPI.CompExType;
import fr.imag.adele.apam.apamAPI.Composite;
import fr.imag.adele.apam.util.Attributes;
import fr.imag.adele.sam.Instance;

public class CompExInstImpl extends ASMInstImpl implements CompExInst {

    private final Composite		me;
    private final CompExType    compType;
    private final ASMImpl       mainImpl;
    private final ASMInst       mainInst;

    // mainSpec == this.getSpec ()

    private CompExInstImpl(CompExType father, Composite instCompo, ASMInst asmInst) {
        //the composite instance as an ASMInst
        //directly refers to the sam object associated with the main instance.
        super((ASMImpl) father, instCompo, null, asmInst.getSAMInst());
        //create main instance
        // TODO decide who is the father, the composite type or the instcompo that is containing me as an ASMInst
        me = new CompositeImpl(father.getASMName()+"<"+asmInst.getASMName()+">", instCompo, father.getApplication(), null);
        
        //the composite itself as an ASMInst
        //Warning father is itself !!
        if (instCompo == null)
    	   this.setComposite(me);
        
        //because constructor needs the instance, and the instance need the composite ...
        ((ASMInstImpl) asmInst).setComposite(this);

        compType = father;
        mainInst = asmInst;
        mainImpl = asmInst.getImpl();
    }

    /**
     * Get access to the internal implementation of the wrapped instance
     */
	@Override
	public Internal asInternal() {
		return me.asInternal();
	}
	
	
    @Override
    public String getASMName() {
    	return compType.getASMName()+"<"+mainInst.getASMName()+">";
    }
    
    @Override
    public String toString() {
        return getASMName();
    }
 
    //@Override
    public static CompExInst createCompExInst(CompExType compExType, Composite instCompo, Attributes initialproperties) {
        Set<ManagerModel> models = null;
        ASMInst asmInst;
        Instance samCompInst = null;
        if (compExType == null) {
            System.err.println("ERROR :  missing type in createCompExInst");
            return null;
        }
        //        try {
        //            samCompInst = ((ASMImplImpl) compExType).getSamImpl().createInstance(null);
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //        }
        //        if (compExType.getMainImpl() != null) {
        //        if (instCompo == null) { //an application
        //            instCompo = this ;
        //        }
        asmInst = compExType.getMainImpl().createInst(null, initialproperties);
        return new CompExInstImpl(compExType, instCompo, asmInst);
    }

    //look by name. Deploy if needed.
    //        asmInst = (CST.apam).resolveAppli(compExType, instCompo, compExType.getMainImplName(), null, null, null);
    //        if (asmInst == null)
    //            return null;
    //
    //        return new CompExInstImpl(compExType, instCompo, samCompInst, asmInst);
    //    }

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
    public String getName() {
        return me.getName();
    }

    @Override
    public Application getApplication() {
        return me.getApplication();
    }

    @Override
    public Composite createComposite(String name, Set<ManagerModel> models) {
        return me.createComposite(name, models);
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
	public Set<Composite> getDependents() {
		return me.getDependents();
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

}
