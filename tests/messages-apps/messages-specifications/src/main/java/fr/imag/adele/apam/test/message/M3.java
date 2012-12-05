package fr.imag.adele.apam.test.message;

public class M3 {
    
    private double moy;

    public M3(double a, double b) {
        this.moy = (a+b)/2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(moy);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        M3 other = (M3) obj;
        if (Double.doubleToLongBits(moy) != Double.doubleToLongBits(other.moy))
            return false;
        return true;
    }

    public double getMoy(){
        return moy;
    }
    
    @Override
    public String toString() {        
        return Double.toString(moy);
    }
}
