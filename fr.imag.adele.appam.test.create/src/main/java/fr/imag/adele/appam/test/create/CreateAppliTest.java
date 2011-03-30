package fr.imag.adele.appam.test.create;

import fr.imag.adele.apam.apamAPI.Apam;
import fr.imag.adele.apam.apamAPI.Composite;

public class CreateAppliTest {
	
	Apam apam;
	
	@SuppressWarnings("unused")
	private void start() {

		Composite compo = apam.createAppli("monAppli", null, "DependencyTest", null, null, null);
		apam.execute(null);
	}

}
