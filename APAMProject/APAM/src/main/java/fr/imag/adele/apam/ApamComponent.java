package fr.imag.adele.apam;

public interface ApamComponent {

    /**
     * This methos is called as soon as the instance is created. Provides as parameter the reference of that instance in
     * Apam.
     * 
     * @param apamInstance : this instance.
     */
    public void apamStart(Instance apamInstance);

    public void apamStop();

    public void apamRelease();
}
