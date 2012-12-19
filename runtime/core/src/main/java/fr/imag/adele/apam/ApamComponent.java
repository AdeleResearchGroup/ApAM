package fr.imag.adele.apam;

public interface ApamComponent {

    /**
     * This method is called as soon as the instance is created. Provides as parameter the reference of that instance in
     * Apam.
     * 
     * @param apamInstance : this instance.
     */
    void apamInit(Instance apamInstance);

    void apamRemove();

}
