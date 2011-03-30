package fr.imag.adele.appam.test.dependency;

import java.util.List;

public class DependencyTest {

	private List<DependencyTestInterface> dependencies;

	@SuppressWarnings("unused")
	private void start() {
		for (DependencyTestInterface dependency : dependencies) {
			dependency.hello();
		}

	}
}
