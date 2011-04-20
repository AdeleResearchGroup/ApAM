package fr.imag.adele.apam.test.s5;

public class S5Impl implements S5 {

    @Override
    public void callS5(String msg) {
        System.out.println("S4 called " + msg);
    }

}
