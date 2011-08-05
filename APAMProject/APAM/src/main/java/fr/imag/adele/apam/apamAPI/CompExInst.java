package fr.imag.adele.apam.apamAPI;

public interface CompExInst extends ASMInst, Composite {

    public ASMInst getMainInst();

    public ASMImpl getMainImpl();

    public CompExType getCompType();
}
