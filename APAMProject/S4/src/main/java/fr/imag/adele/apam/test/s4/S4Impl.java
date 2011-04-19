package fr.imag.adele.apam.test.s4;

public class S4Impl implements S4 {
    @Override
    public void callS4(String s) {
        System.out.println("S4 called " + s);
    }
}
