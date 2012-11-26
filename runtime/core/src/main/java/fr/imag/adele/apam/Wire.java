package fr.imag.adele.apam;

public interface Wire {

    public Instance getSource();

    public Instance getDestination();

    public String getDepName();
    
    public boolean hasConstraints() ;
    
    public void remove();

}