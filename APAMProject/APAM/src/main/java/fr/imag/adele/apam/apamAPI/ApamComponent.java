package fr.imag.adele.apam.apamAPI;

public interface ApamComponent {

    public void apamStart(ASMInst apamInstance);

    public void apamStop();

    public void apamRelease();
}
