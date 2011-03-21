package fr.imag.adele.apam.apamAPI;

import java.util.Set;

import fr.imag.adele.apam.ManagerModel;

public interface Apam {
	
	public void createAppli (Composite appl, ASMImpl main) ;
	public void execute () ;
	public Composite createAppli(String compositeName, ASMImpl main, Set <ManagerModel> models) ;
	public Composite getAppli () ;
	public ASMImpl getAppliMain  ()  ;
	
}
