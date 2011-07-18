package fr.imag.adele.apam.test.dependency;

import java.util.List;

public class DependencyTest implements DependencyTestProvidedInterface {

	private List<DependencyTestInterface> dependencies;

	@SuppressWarnings("unused")
	private void start() {
		for (DependencyTestInterface dependency : dependencies) {
			dependency.hello();
		}

	}
}
