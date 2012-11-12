package fr.imag.adele.apam.test.message;

public class M1 {
    
    private double moy;

    public M1(double a, double b) {
        this.moy = (a+b)/2;
    }

    public double getMoy(){
        return moy;
    }
}
