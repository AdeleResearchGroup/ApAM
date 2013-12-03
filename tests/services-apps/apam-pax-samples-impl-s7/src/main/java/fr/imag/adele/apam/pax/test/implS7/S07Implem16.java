package fr.imag.adele.apam.pax.test.implS7;

public class S07Implem16 implements S07Interface16 {

    public S07Dependency02 injected02;

    public S07Dependency02 getInjected02() throws Exception {
	return injected02;
    }

    @Override
    public String whoami() {
	return "S07Inmplem16";
    }
}
